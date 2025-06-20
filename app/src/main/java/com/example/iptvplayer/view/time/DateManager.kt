package com.example.iptvplayer.view.time

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

class DateManager @Inject constructor(

) {
    fun parseDate(date: String, pattern: String, timeZone: TimeZone = TimeZone.getTimeZone("GMT+4")): Long {
        val formatter = SimpleDateFormat(pattern)
        if (timeZone != null) {
            formatter.timeZone = timeZone
        }
        return (formatter.parse(date)?.time?.div(1000)) ?: 0
    }

    fun formatDate(date: Long, pattern: String, timeZone: TimeZone = TimeZone.getTimeZone("GMT+4")): String {
        println("formatDate - timestamp: $date, pattern: $pattern")
        val formatter = SimpleDateFormat(pattern)
        if (timeZone != null) {
            formatter.timeZone = timeZone
        }

        val formattedString = formatter.format(Date(date * 1000))
        return formattedString
    }

    fun dateToEpochSeconds(day: Int, month: Int, year: Int, hour: Int, minute: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.apply {
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.MONTH, month - 1)
            set(Calendar.YEAR, year)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }

        return calendar.timeInMillis / 1000
    }

    fun getDaysOfMonth(month: Int): Int {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, 2025)
        calendar.set(Calendar.MONTH, month-1)

        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun getDayOfWeek(month: Int, day: Int): String {
        val calendar = Calendar.getInstance()
        calendar.apply {
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.YEAR, 2025)
        }
        return calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault()) ?: ""
    }

    fun getFullMonthName(monthNumber: Int): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.MONTH, monthNumber - 1)
        return calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: ""
    }
}