package com.example.ui.components

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    private val fullDateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())

    fun formatDueDate(epochMs: Long?): String {
        if (epochMs == null) return ""
        val now = System.currentTimeMillis()
        val dueCal = Calendar.getInstance().apply { timeInMillis = epochMs }
        val nowCal = Calendar.getInstance().apply { timeInMillis = now }

        val isSameDay = dueCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                dueCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)

        val tomorrowCal = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, 1)
        }
        val isTomorrow = dueCal.get(Calendar.YEAR) == tomorrowCal.get(Calendar.YEAR) &&
                dueCal.get(Calendar.DAY_OF_YEAR) == tomorrowCal.get(Calendar.DAY_OF_YEAR)

        val yesterdayCal = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val isYesterday = dueCal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
                dueCal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR)

        return when {
            isSameDay -> "Today, ${timeFormat.format(Date(epochMs))}"
            isTomorrow -> "Tomorrow, ${timeFormat.format(Date(epochMs))}"
            isYesterday -> "Yesterday"
            epochMs < now -> "Overdue: ${dateFormat.format(Date(epochMs))}"
            else -> fullDateFormat.format(Date(epochMs))
        }
    }

    fun isOverdue(epochMs: Long?, isCompleted: Boolean): Boolean {
        if (epochMs == null || isCompleted) return false
        val endOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.timeInMillis
        return epochMs < System.currentTimeMillis()
    }

    fun isDueToday(epochMs: Long?): Boolean {
        if (epochMs == null) return false
        val nowCal = Calendar.getInstance()
        val dueCal = Calendar.getInstance().apply { timeInMillis = epochMs }
        return nowCal.get(Calendar.YEAR) == dueCal.get(Calendar.YEAR) &&
                nowCal.get(Calendar.DAY_OF_YEAR) == dueCal.get(Calendar.DAY_OF_YEAR)
    }
}
