package com.qinggua.app.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 应用全局状态与业务逻辑 */
class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val store = StateStore(app)

    private val _all = MutableStateFlow<List<MediaItem>>(emptyList())
    val all: StateFlow<List<MediaItem>> = _all.asStateFlow()

    private val _buckets = MutableStateFlow<MutableMap<Long, Bucket>>(store.loadBuckets())
    val buckets: StateFlow<Map<Long, Bucket>> = _buckets.asStateFlow()

    private val _cursors = MutableStateFlow<MutableMap<Lane, Int>>(store.loadCursors())
    val cursors: StateFlow<Map<Lane, Int>> = _cursors.asStateFlow()

    private val _undo = MutableStateFlow<MutableList<UndoBatch>>(store.loadUndo())
    val undo: StateFlow<List<UndoBatch>> = _undo.asStateFlow()

    private val _filter = MutableStateFlow(store.loadFilter())
    val filter: StateFlow<FilterState> = _filter.asStateFlow()

    private val _settings = MutableStateFlow(store.loadSettings())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    /** 加载媒体（权限已授予时调用） */
    fun reload(includeImages: Boolean = true, includeVideos: Boolean = true) {
        _loading.value = true
        viewModelScope.launch {
            val list = MediaRepository.loadAll(getApplication(), includeImages, includeVideos)
            _all.value = list
            // 清理已被系统删除、但状态里还留着的记录
            val alive = list.map { it.id }.toHashSet()
            val cleaned = _buckets.value.filterKeys { it in alive }.toMutableMap()
            if (cleaned.size != _buckets.value.size) {
                _buckets.value = cleaned
                store.saveBuckets(cleaned)
            }
            _loading.value = false
        }
    }

    fun notify(message: String) { _toast.value = message }
    fun clearToast() { _toast.value = null }

    // ---------- 分类队列 ----------

    /** 待整理卡片流：UNREVIEWED + 当前入口 + 筛选 */
    fun queueFor(lane: Lane, f: FilterState = _filter.value): List<MediaItem> {
        var list = _all.value.filter { item ->
            val laneOk = when (lane) {
                Lane.PHOTO -> !item.isVideo
                Lane.VIDEO -> item.isVideo
                Lane.ALL -> true
            }
            val bucketOk = (_buckets.value[item.id] ?: Bucket.UNREVIEWED) == Bucket.UNREVIEWED
            val albumOk = f.bucketName == null || item.bucketName == f.bucketName
            val ymOk = f.yearMonth == null || yearMonthOf(item.timeMs) == f.yearMonth
            laneOk && bucketOk && albumOk && ymOk
        }
        list = if (f.sortNewestFirst) list.sortedByDescending { it.timeMs }
               else list.sortedBy { it.timeMs }
        return list
    }

    fun itemsIn(bucket: Bucket, lane: Lane = Lane.ALL): List<MediaItem> =
        _all.value.filter {
            (_buckets.value[it.id] ?: Bucket.UNREVIEWED) == bucket &&
            when (lane) {
                Lane.PHOTO -> !it.isVideo
                Lane.VIDEO -> it.isVideo
                Lane.ALL -> true
            }
        }.sortedByDescending { it.timeMs }

    fun bucketOf(id: Long): Bucket = _buckets.value[id] ?: Bucket.UNREVIEWED

    fun albums(): List<String> = MediaRepository.allBuckets(_all.value)
    fun months(): List<String> = MediaRepository.yearMonths(_all.value)

    // ---------- 光标（断点） ----------

    fun cursor(lane: Lane): Int = _cursors.value[lane] ?: 0

    fun setCursor(lane: Lane, index: Int) {
        val next = _cursors.value.toMutableMap()
        next[lane] = index.coerceAtLeast(0)
        _cursors.value = next
        store.saveCursors(next)
    }

    // ---------- 滑动分类 ----------

    fun swipe(item: MediaItem, action: SwipeAction, lane: Lane, cursorBefore: Int) {
        val target = when (action) {
            SwipeAction.KEEP -> Bucket.KEPT
            SwipeAction.TRASH -> Bucket.TRASH
            SwipeAction.PENDING -> Bucket.PENDING
        }
        val previous = bucketOf(item.id)
        moveTo(listOf(item.id), target)
        pushUndo(
            UndoBatch(
                listOf(UndoEntry(item.id, previous, lane, cursorBefore)),
                when (action) {
                    SwipeAction.KEEP -> "已保留"
                    SwipeAction.TRASH -> "移入回收箱"
                    SwipeAction.PENDING -> "移入待定"
                }
            )
        )
    }

    private fun moveTo(ids: Collection<Long>, bucket: Bucket) {
        val next = _buckets.value.toMutableMap()
        ids.forEach { next[it] = bucket }
        _buckets.value = next
        store.saveBuckets(next)
    }

    /** 从待定/已保留批量移到目标桶 */
    fun moveBatch(ids: Collection<Long>, target: Bucket, lane: Lane = Lane.ALL, desc: String) {
        val entries = ids.map { UndoEntry(it, bucketOf(it), lane, 0) }
        moveTo(ids, target)
        pushUndo(UndoBatch(entries, desc))
    }

    /** 从任意桶移回待整理 */
    fun backToUnreviewed(ids: Collection<Long>, lane: Lane = Lane.ALL, desc: String = "已移回待整理") {
        val entries = ids.map { UndoEntry(it, bucketOf(it), lane, 0) }
        val next = _buckets.value.toMutableMap()
        ids.forEach { next.remove(it) }
        _buckets.value = next
        store.saveBuckets(next)
        pushUndo(UndoBatch(entries, desc))
    }

    // ---------- 撤销 ----------

    private fun pushUndo(batch: UndoBatch) {
        if (batch.entries.isEmpty()) return
        val next = _undo.value.toMutableList()
        next += batch
        val limit = _settings.value.undoLimit
        while (next.size > limit) next.removeAt(0)
        _undo.value = next
        store.saveUndo(next, limit)
    }

    fun canUndo(): Boolean = _undo.value.isNotEmpty()

    fun undoLast() {
        val stack = _undo.value
        if (stack.isEmpty()) { notify("没有可撤销的操作"); return }
        val batch = stack.last()
        val nextBuckets = _buckets.value.toMutableMap()
        batch.entries.forEach { e ->
            if (e.previousBucket == Bucket.UNREVIEWED) nextBuckets.remove(e.mediaId)
            else nextBuckets[e.mediaId] = e.previousBucket
        }
        _buckets.value = nextBuckets
        store.saveBuckets(nextBuckets)

        // 恢复该入口的光标
        batch.entries.firstOrNull()?.let { e ->
            if (e.lane != Lane.ALL) setCursor(e.lane, e.cursorBefore)
        }

        val nextStack = stack.dropLast(1).toMutableList()
        _undo.value = nextStack
        store.saveUndo(nextStack, _settings.value.undoLimit)
        notify("已撤销：${batch.description}")
    }

    // ---------- 真删 ----------

    /** 系统删除完成后调用，清理本地状态 */
    fun onDeleted(ids: Collection<Long>) {
        val nextBuckets = _buckets.value.toMutableMap()
        ids.forEach { nextBuckets.remove(it) }
        _buckets.value = nextBuckets
        store.removeIds(ids)
        _all.value = _all.value.filterNot { it.id in ids }
        notify("已从手机删除 ${ids.size} 项")
    }

    // ---------- 设置与筛选 ----------

    fun updateSettings(block: (Settings) -> Settings) {
        val next = block(_settings.value)
        _settings.value = next
        store.saveSettings(next)
    }

    fun updateFilter(block: (FilterState) -> FilterState) {
        val next = block(_filter.value)
        _filter.value = next
        store.saveFilter(next)
    }

    // ---------- 工具 ----------

    private fun yearMonthOf(ms: Long): String {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = ms }
        return "%04d-%02d".format(
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH) + 1
        )
    }
}
