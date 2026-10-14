package com.keyfortress.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeFormatter {

    private val fullDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val timeOnlyFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun format(timestamp: Long): String {
        if (timestamp <= 0) return ""
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        val minute = 60 * 1000L
        val hour = 60 * minute
        val day = 24 * hour

        return when {
            diff < minute -> "刚刚"
            diff < hour -> "${diff / minute} 分钟前"
            diff < day -> "今天 ${timeOnlyFormat.format(Date(timestamp))}"
            diff < 2 * day -> "昨天 ${timeOnlyFormat.format(Date(timestamp))}"
            else -> fullDateFormat.format(Date(timestamp))
        }
    }

    fun formatFull(timestamp: Long): String {
        if (timestamp <= 0) return ""
        return fullDateFormat.format(Date(timestamp))
    }
}
