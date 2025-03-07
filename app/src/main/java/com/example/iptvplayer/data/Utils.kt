package com.example.iptvplayer.data

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.instacart.library.truetime.TrueTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
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
    suspend fun getGmtTime(): Long =
        withContext(Dispatchers.IO) {
            TrueTime.build().withNtpHost("pool.ntp.org").initialize()
            Log.i("I SEE YOU", TrueTime.now().time.toString())
            TrueTime.now().time / 1000
        }
}