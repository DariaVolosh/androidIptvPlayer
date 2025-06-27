package com.example.iptvplayer.domain.time

import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import javax.inject.Inject

class CalendarManager @Inject constructor(

) {
    fun getCalendarDay(calendar: Calendar): Int {
        return calendar.get(Calendar.DAY_OF_MONTH)
    }

    fun getCalendar(date: Long, timeZone: TimeZone? = null): Calendar {
        // defaults to current system locale (in tbilisi case - gmt+4), receives utc time as a parameter
        val calendar = Calendar.getInstance()
        if (timeZone != null) {
            calendar.timeZone = timeZone
        }
        calendar.time = Date(date * 1000)
        return calendar
    }

    fun getCalendarMonth(calendar: Calendar): Int {
        return calendar.get(Calendar.MONTH)
    }
}