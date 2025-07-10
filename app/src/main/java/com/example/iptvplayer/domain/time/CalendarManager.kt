package com.example.iptvplayer.domain.time

import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import javax.inject.Inject

class CalendarManager @Inject constructor(

) {
    fun getCalendar(date: Long): Calendar {
        // defaults to current system locale (in tbilisi case - gmt+4), receives utc time as a parameter
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+4"))
        calendar.time = Date(date * 1000)
        return calendar
    }

    fun getCalendarDay(calendar: Calendar): Int {
        return calendar.get(Calendar.DAY_OF_MONTH)
    }

    fun getCalendarMonth(calendar: Calendar): Int {
        return calendar.get(Calendar.MONTH)
    }

    fun getCalendarHour(calendar: Calendar): Int {
        return calendar.get(Calendar.HOUR_OF_DAY)
    }

    fun getCalendarMinute(calendar: Calendar): Int {
        return calendar.get(Calendar.MINUTE)
    }
}