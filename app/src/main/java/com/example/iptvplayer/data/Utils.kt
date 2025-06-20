package com.example.iptvplayer.data

import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

class Utils @Inject constructor(
    private val ntpClient: NtpTimeClient
) {
    fun getCalendar(date: Long, timeZone: TimeZone? = null): Calendar {
        // defaults to current system locale (in tbilisi case - gmt+4), receives utc time as a parameter
        val calendar = Calendar.getInstance()
        if (timeZone != null) {
            calendar.timeZone = timeZone
        }
        calendar.time = Date(date * 1000)
        return calendar
    }

    fun getCalendarDay(calendar: Calendar): Int {
        return calendar.get(Calendar.DAY_OF_MONTH)
    }

    fun getCalendarMonth(calendar: Calendar): Int {
        return calendar.get(Calendar.MONTH)
    }

    fun getCalendarTimeInSeconds(calendar: Calendar): Long {
        return calendar.timeInMillis / 1000
    }

    fun formatDate(date: Long, pattern: String, timeZone: TimeZone? = null): String {
        // formats according to current system locale
        val formatter = SimpleDateFormat(pattern)
        if (timeZone != null) {
            formatter.timeZone = timeZone
        }
        return formatter.format(Date(date * 1000))
    }

    fun parseDate(date: String, pattern: String, timeZone: TimeZone? = null): Long {
        val formatter = SimpleDateFormat(pattern)
        if (timeZone != null) {
            formatter.timeZone = timeZone
        }
        return (formatter.parse(date)?.time?.div(1000)) ?: 0
    }

    fun compareDates(date1: String, date2: String): Int {
        val formatter = SimpleDateFormat("dd MMMM", Locale.getDefault())
        val dateObj1 = formatter.parse(date1)
        val dateObj2 = formatter.parse(date2)
        return dateObj1.compareTo(dateObj2)
    }

    fun getFullMonthName(monthNumber: Int): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.MONTH, monthNumber - 1)
        return calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: ""
    }

    fun getAllWeekdays(): List<String> {
        val symbols = DateFormatSymbols(Locale.getDefault())
        val shortWeekdays = symbols.shortWeekdays

        return shortWeekdays.drop(1).toList()
    }

    // return the amount of days in a given month
    fun getDaysOfMonth(month: Int): Int {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, 2025)
        calendar.set(Calendar.MONTH, month-1)

        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
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

    fun getDayOfWeek(month: Int, day: Int): String {
        val calendar = Calendar.getInstance()
        calendar.apply {
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.YEAR, 2025)
        }
        return calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault()) ?: ""
    }

    suspend fun getGmtTime(): Long =
        ntpClient.getGmtTime()
}