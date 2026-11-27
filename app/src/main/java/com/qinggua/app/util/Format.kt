package com.qinggua.app.util

import com.qinggua.app.data.MediaItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 显示用格式化 */
object Format {

    fun dateTime(ms: Long): String {
        if (ms <= 0) return ""
        return try {
            SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault()).format(Date(ms))
        } catch (e: Exception) { "" }
    }

    fun size(bytes: Long): String = when {
        bytes >= 1024L * 1024L -> String.format(Locale.getDefault(), "%.1f MB", bytes / 1024.0 / 1024.0)
        bytes >= 1024L -> String.format(Locale.getDefault(), "%.0f KB", bytes / 1024.0)
        else -> "$bytes B"
    }

    fun location(item: MediaItem): String? {
        val lat = item.latitude ?: return null
        val lng = item.longitude ?: return null
        return String.format(Locale.getDefault(), "%.3f, %.3f", lat, lng)
    }

    fun duration(ms: Long): String {
        if (ms <= 0) return ""
        val total = ms / 1000
        val m = total / 60
        val s = total % 60
        return "%02d:%02d".format(m, s)
    }

    fun month(ms: Long): String {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = ms }
        return "%04d-%02d".format(
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH) + 1
        )
    }
}
