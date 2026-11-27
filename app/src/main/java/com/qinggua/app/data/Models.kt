package com.qinggua.app.data

import android.net.Uri

/** 媒体项，字段来自 MediaStore */
data class MediaItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val bucketName: String,      // 相册名
    val dateAddedSec: Long,      // 加入相册时间（秒）
    val dateTakenSec: Long,      // 拍摄时间（秒），0 表示无
    val sizeBytes: Long,
    val mimeType: String,
    val isVideo: Boolean,
    val durationMs: Long = 0L,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val orientation: Int = 0,
    val width: Int = 0,
    val height: Int = 0
) {
    /** 卡片显示用的时间（毫秒） */
    val timeMs: Long get() = (if (dateTakenSec > 0) dateTakenSec else dateAddedSec) * 1000L
}

/** 四个分类桶 */
enum class Bucket(val key: String) {
    UNREVIEWED("unreviewed"),   // 待整理
    KEPT("kept"),               // 已保留
    TRASH("trash"),             // 回收箱
    PENDING("pending");         // 待定

    companion object {
        fun from(key: String?): Bucket = values().find { it.key == key } ?: UNREVIEWED
    }
}

/** 三个入口 */
enum class Lane(val key: String) {
    PHOTO("photo"), VIDEO("video"), ALL("all");

    companion object {
        fun from(key: String?): Lane = values().find { it.key == key } ?: PHOTO
    }
}

/** 滑动动作 */
enum class SwipeAction { KEEP, TRASH, PENDING }

/** 撤销记录：把某个 id 从 from 桶移回，取消这次分类 */
data class UndoEntry(
    val mediaId: Long,
    val previousBucket: Bucket,
    val lane: Lane,
    val cursorBefore: Int
)

/** 一次批量操作的可撤销记录 */
data class UndoBatch(
    val entries: List<UndoEntry>,
    val description: String
)

/** 筛选条件 */
data class FilterState(
    val sortNewestFirst: Boolean = true,
    val bucketName: String? = null,
    val yearMonth: String? = null
)

/** 用户设置 */
data class Settings(
    val unlockEnabled: Boolean = false,
    val gestureSensitivity: Float = 0.35f,   // 触发滑动的屏幕宽度比例
    val undoLimit: Int = 20,
    val trashConfirm: Boolean = true
)
