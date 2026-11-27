package com.qinggua.app.data

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 通过 MediaStore 读取本机全部图片与视频，以及执行删除。
 * 权限由外层（MainActivity）申请，这里只负责查询。
 */
object MediaRepository {

    private fun projection(isVideo: Boolean): Array<String> {
        val base = mutableListOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.ORIENTATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            base.add(MediaStore.MediaColumns.LATITUDE)
            base.add(MediaStore.MediaColumns.LONGITUDE)
            base.add(MediaStore.MediaColumns.DATE_TAKEN)
        }
        if (isVideo) base.add(MediaStore.Video.Media.DURATION)
        return base.distinct().toTypedArray()
    }

    private fun readCursor(c: Cursor, isVideo: Boolean): MediaItem {
        fun longOf(col: String): Long = try {
            val i = c.getColumnIndex(col); if (i >= 0 && !c.isNull(i)) c.getLong(i) else 0L
        } catch (e: Exception) { 0L }
        fun intOf(col: String): Int = try {
            val i = c.getColumnIndex(col); if (i >= 0 && !c.isNull(i)) c.getInt(i) else 0
        } catch (e: Exception) { 0 }
        fun strOf(col: String): String = try {
            val i = c.getColumnIndex(col); if (i >= 0 && !c.isNull(i)) c.getString(i) else ""
        } catch (e: Exception) { "" }
        fun doubleOf(col: String): Double? = try {
            val i = c.getColumnIndex(col)
            if (i >= 0 && !c.isNull(i)) { val v = c.getDouble(i); if (v == 0.0) null else v } else null
        } catch (e: Exception) { null }

        val id = longOf(MediaStore.MediaColumns._ID)
        val external = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                       else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val uri = Uri.withAppendedPath(external, id.toString())
        val dateTakenSec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            longOf(MediaStore.MediaColumns.DATE_TAKEN) / 1000L else 0L

        return MediaItem(
            id = id,
            uri = uri,
            displayName = strOf(MediaStore.MediaColumns.DISPLAY_NAME),
            bucketName = strOf(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME).ifBlank { "其他" },
            dateAddedSec = longOf(MediaStore.MediaColumns.DATE_ADDED),
            dateTakenSec = dateTakenSec,
            sizeBytes = longOf(MediaStore.MediaColumns.SIZE),
            mimeType = strOf(MediaStore.MediaColumns.MIME_TYPE),
            isVideo = isVideo,
            durationMs = if (isVideo) longOf(MediaStore.Video.Media.DURATION) else 0L,
            latitude = doubleOf(MediaStore.MediaColumns.LATITUDE),
            longitude = doubleOf(MediaStore.MediaColumns.LONGITUDE),
            orientation = intOf(MediaStore.MediaColumns.ORIENTATION),
            width = intOf(MediaStore.MediaColumns.WIDTH),
            height = intOf(MediaStore.MediaColumns.HEIGHT)
        )
    }

    suspend fun loadAll(context: Context, includeImages: Boolean, includeVideos: Boolean): List<MediaItem> =
        withContext(Dispatchers.IO) {
            val result = mutableListOf<MediaItem>()
            val cr = context.contentResolver
            if (includeImages) result += query(cr, false)
            if (includeVideos) result += query(cr, true)
            result.distinctBy { "${it.isVideo}_${it.id}" }
        }

    private fun query(cr: ContentResolver, isVideo: Boolean): List<MediaItem> {
        val baseUri = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                      else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        // Android 10+ 读取 GPS 必须用 setRequireOriginal，否则经纬列为空
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try { MediaStore.setRequireOriginal(baseUri) } catch (e: Exception) { baseUri }
        } else baseUri
        val sort = "${MediaStore.MediaColumns.DATE_ADDED} DESC"
        val out = mutableListOf<MediaItem>()
        try {
            cr.query(uri, projection(isVideo), null, null, sort)?.use { c ->
                while (c.moveToNext()) out += readCursor(c, isVideo)
            }
        } catch (e: SecurityException) {
            // 权限未授予或用户只授权了部分照片，返回已读到的部分
        } catch (e: Exception) {
            // 某些机型列不存在时逐列降级查询
            try {
                val safe = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    arrayOf(
                        MediaStore.MediaColumns._ID,
                        MediaStore.MediaColumns.DISPLAY_NAME,
                        MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
                        MediaStore.MediaColumns.DATE_ADDED,
                        MediaStore.MediaColumns.SIZE,
                        MediaStore.MediaColumns.MIME_TYPE
                    )
                } else {
                    arrayOf(
                        MediaStore.MediaColumns._ID,
                        MediaStore.MediaColumns.DISPLAY_NAME,
                        MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
                        MediaStore.MediaColumns.DATE_ADDED,
                        MediaStore.MediaColumns.SIZE,
                        MediaStore.MediaColumns.MIME_TYPE
                    )
                }
                cr.query(uri, safe, null, null, sort)?.use { c ->
                    while (c.moveToNext()) out += readCursor(c, isVideo)
                }
            } catch (ignored: Exception) { }
        }
        return out
    }

    /**
     * 真删：Android 11+ 返回 IntentSender 交给系统弹窗确认；
     * 低版本返回 null，由调用方走 ContentResolver.delete。
     */
    fun createDeleteRequest(context: Context, items: List<MediaItem>): android.content.IntentSender? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && items.isNotEmpty()) {
            val uris = items.map { it.uri }
            val request = MediaStore.createDeleteRequest(context.contentResolver, uris)
            return request.intentSender
        }
        return null
    }

    /** 低版本直接删除，返回是否至少删成功一条 */
    fun deleteLegacy(context: Context, items: List<MediaItem>): Boolean {
        var ok = false
        items.forEach {
            try {
                val n = context.contentResolver.delete(it.uri, null, null)
                if (n > 0) ok = true
            } catch (e: Exception) { }
        }
        return ok
    }

    fun allBuckets(items: List<MediaItem>): List<String> =
        items.map { it.bucketName }.distinct().sorted()

    fun yearMonths(items: List<MediaItem>): List<String> =
        items.map { formatYearMonth(it.timeMs) }.distinct().sortedDescending()

    private fun formatYearMonth(ms: Long): String {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = ms }
        return "%04d-%02d".format(cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1)
    }
}
