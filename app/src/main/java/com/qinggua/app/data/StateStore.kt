package com.qinggua.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * 本机状态持久化（SharedPreferences + JSON）。
 * 只保存非 UNREVIEWED 的映射，避免存几千条默认值。
 */
class StateStore(context: Context) {

    private val prefs = context.getSharedPreferences("qinggua_state", Context.MODE_PRIVATE)

    companion object {
        private const val K_BUCKETS = "buckets"      // { "id": "kept|trash|pending" }
        private const val K_CURSORS = "cursors"      // { "photo": 12, "video": 0, "all": 8 }
        private const val K_UNDO = "undo"            // [ { desc, entries:[...] }, ... ]
        private const val K_FILTER = "filter"
        private const val K_SETTINGS = "settings"
    }

    // ---------- 分类状态 ----------

    fun loadBuckets(): MutableMap<Long, Bucket> {
        val map = mutableMapOf<Long, Bucket>()
        try {
            val obj = JSONObject(prefs.getString(K_BUCKETS, "{}") ?: "{}")
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val id = k.toLongOrNull() ?: continue
                map[id] = Bucket.from(obj.optString(k))
            }
        } catch (e: Exception) { }
        return map
    }

    fun saveBuckets(map: Map<Long, Bucket>) {
        val obj = JSONObject()
        map.forEach { (id, b) -> if (b != Bucket.UNREVIEWED) obj.put(id.toString(), b.key) }
        prefs.edit().putString(K_BUCKETS, obj.toString()).apply()
    }

    /** 真删后把 id 从状态里彻底移除 */
    fun removeIds(ids: Collection<Long>) {
        val map = loadBuckets()
        ids.forEach { map.remove(it) }
        saveBuckets(map)
    }

    // ---------- 断点进度 ----------

    fun loadCursors(): MutableMap<Lane, Int> {
        val map = mutableMapOf<Lane, Int>()
        try {
            val obj = JSONObject(prefs.getString(K_CURSORS, "{}") ?: "{}")
            Lane.values().forEach { lane -> map[lane] = obj.optInt(lane.key, 0) }
        } catch (e: Exception) {
            Lane.values().forEach { map[it] = 0 }
        }
        return map
    }

    fun saveCursors(map: Map<Lane, Int>) {
        val obj = JSONObject()
        map.forEach { (lane, idx) -> obj.put(lane.key, idx) }
        prefs.edit().putString(K_CURSORS, obj.toString()).apply()
    }

    // ---------- 撤销栈 ----------

    fun loadUndo(): MutableList<UndoBatch> {
        val list = mutableListOf<UndoBatch>()
        try {
            val arr = JSONArray(prefs.getString(K_UNDO, "[]") ?: "[]")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val entries = mutableListOf<UndoEntry>()
                val ea = o.optJSONArray("entries") ?: JSONArray()
                for (j in 0 until ea.length()) {
                    val e = ea.getJSONObject(j)
                    entries += UndoEntry(
                        mediaId = e.getLong("id"),
                        previousBucket = Bucket.from(e.optString("b")),
                        lane = Lane.from(e.optString("lane")),
                        cursorBefore = e.optInt("cur")
                    )
                }
                list += UndoBatch(entries, o.optString("desc"))
            }
        } catch (e: Exception) { }
        return list
    }

    fun saveUndo(list: List<UndoBatch>, limit: Int) {
        val arr = JSONArray()
        list.takeLast(limit).forEach { batch ->
            val o = JSONObject()
            o.put("desc", batch.description)
            val ea = JSONArray()
            batch.entries.forEach { e ->
                val x = JSONObject()
                x.put("id", e.mediaId)
                x.put("b", e.previousBucket.key)
                x.put("lane", e.lane.key)
                x.put("cur", e.cursorBefore)
                ea.put(x)
            }
            o.put("entries", ea)
            arr.put(o)
        }
        prefs.edit().putString(K_UNDO, arr.toString()).apply()
    }

    // ---------- 筛选 ----------

    fun loadFilter(): FilterState = try {
        val o = JSONObject(prefs.getString(K_FILTER, "{}") ?: "{}")
        FilterState(
            sortNewestFirst = if (o.has("sort")) o.getBoolean("sort") else true,
            bucketName = if (o.isNull("bucket")) null else o.optString("bucket"),
            yearMonth = if (o.isNull("ym")) null else o.optString("ym")
        )
    } catch (e: Exception) { FilterState() }

    fun saveFilter(f: FilterState) {
        val o = JSONObject()
        o.put("sort", f.sortNewestFirst)
        o.put("bucket", f.bucketName)
        o.put("ym", f.yearMonth)
        prefs.edit().putString(K_FILTER, o.toString()).apply()
    }

    // ---------- 设置 ----------

    fun loadSettings(): Settings = try {
        val o = JSONObject(prefs.getString(K_SETTINGS, "{}") ?: "{}")
        Settings(
            unlockEnabled = o.optBoolean("unlock", false),
            gestureSensitivity = o.optDouble("sens", 0.35).toFloat().coerceIn(0.15f, 0.7f),
            undoLimit = o.optInt("undoLimit", 20).coerceIn(1, 100),
            trashConfirm = if (o.has("confirm")) o.getBoolean("confirm") else true
        )
    } catch (e: Exception) { Settings() }

    fun saveSettings(s: Settings) {
        val o = JSONObject()
        o.put("unlock", s.unlockEnabled)
        o.put("sens", s.gestureSensitivity.toDouble())
        o.put("undoLimit", s.undoLimit)
        o.put("confirm", s.trashConfirm)
        prefs.edit().putString(K_SETTINGS, o.toString()).apply()
    }
}
