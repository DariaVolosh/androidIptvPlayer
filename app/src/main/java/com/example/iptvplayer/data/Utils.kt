package com.example.iptvplayer.data

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.instacart.library.truetime.TrueTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object Utils {
    @RequiresApi(Build.VERSION_CODES.O)
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

    @RequiresApi(Build.VERSION_CODES.O)
    fun getFullMonthName(monthNumber: Int): String {
        return Month.of(monthNumber).getDisplayName(TextStyle.FULL, Locale.getDefault())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getAllWeekdays(): List<String> {
        return DayOfWeek.entries.map { dayOfWeek ->
            dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    // return the amount of days in a given month
    fun getDaysOfMonth(month: Int): Int {
        val yearMonth = YearMonth.of(2025, month)
        val daysInMonth = yearMonth.lengthOfMonth()
        return daysInMonth
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getDayOfWeek(month: Int, day: Int): String {
        val date = LocalDate.of(2025, month, day)
        val dateOfWeek = date.dayOfWeek
        return dateOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getGmtTime(): Long =
        withContext(Dispatchers.IO) {
            TrueTime.build().withNtpHost("pool.ntp.org").initialize()
            Log.i("I SEE YOU", TrueTime.now().time.toString())
            TrueTime.now().time / 1000
        }
}