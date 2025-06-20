package com.example.iptvplayer.view.time

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iptvplayer.di.MainDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

enum class DateType {
    CURRENT_FULL_DATE,
    START_TIME, // either current epg start time or DVR start time
    STOP_TIME  // either current epg stop time or DVR stop time
}

@HiltViewModel
class DateAndTimeViewModel @Inject constructor(
    private val dateManager: DateManager,
    private val calendarManager: CalendarManager,
    private val timeOrchestrator: TimeOrchestrator,
    @MainDispatcher private val coroutineScope: CoroutineScope
): ViewModel() {

    private val _currentFullDate: MutableStateFlow<String> = MutableStateFlow("Current date not available")
    val currentFullDate: StateFlow<String> = _currentFullDate

    private val _startTime: MutableStateFlow<String> = MutableStateFlow("")
    val startTime: StateFlow<String> = _startTime

    private val _stopTime: MutableStateFlow<String> = MutableStateFlow("")
    val stopTime: StateFlow<String> = _stopTime

    val currentTime: StateFlow<Long> = timeOrchestrator.currentTime.stateIn(
        viewModelScope, SharingStarted.Eagerly, 0L
    )

    val liveTime: StateFlow<Long> = timeOrchestrator.liveTime.stateIn(
        viewModelScope, SharingStarted.Eagerly, 0L
    )

    init {
        val datePattern = "EEEE d MMMM HH:mm:ss"

        combine(
            timeOrchestrator.liveTime,
            timeOrchestrator.currentTime,
            timeOrchestrator.isLive
        ) { liveTimeValue, currentTimeValue, isLive ->
            if (isLive) {
                if (liveTimeValue != 0L) {
                    formatDate(liveTimeValue, datePattern, DateType.CURRENT_FULL_DATE)
                }
            } else {
                if (currentTimeValue != 0L) {
                    formatDate(currentTimeValue, datePattern, DateType.CURRENT_FULL_DATE)
                }
            }
        }.launchIn(coroutineScope)
    }

    fun getCalendar(date: Long, timeZone: TimeZone? = null): Calendar {
        return calendarManager.getCalendar(date, timeZone)
    }

    fun formatDate(
        date: Long,
        datePattern: String,
        dateType: DateType,
        timeZone: TimeZone = TimeZone.getTimeZone("GMT+4")
    ) {
        when (dateType) {
            DateType.CURRENT_FULL_DATE -> {
                println("calling format date")
                println("inside view model ${dateManager.hashCode()}")
                val res = dateManager.formatDate(
                    date,
                    datePattern,
                    timeZone
                )
                _currentFullDate.value = res
            }
            DateType.START_TIME ->
                _startTime.value = dateManager.formatDate(
                    date,
                    datePattern,
                    timeZone
                )
            DateType.STOP_TIME -> _stopTime.value = dateManager.formatDate(
                date,
                datePattern,
                timeZone
            )
        }
    }

    fun resetDate(dateType: DateType) {
        when (dateType) {
            DateType.CURRENT_FULL_DATE -> _currentFullDate.value = "No date available"
            DateType.START_TIME -> _startTime.value = "--:--"
            DateType.STOP_TIME -> _stopTime.value = "--:--"
        }
    }

    fun parseDate(date: String, pattern: String, timeZone: TimeZone = TimeZone.getTimeZone("GMT+4")): Long {
        return dateManager.parseDate(date, pattern, timeZone)
    }

    fun dateToEpochSeconds(day: Int, month: Int, year: Int, hour: Int, minute: Int): Long {
        return dateManager.dateToEpochSeconds(day, month, year, hour, minute)
    }

    fun getDaysOfMonth(month: Int): Int {
        return dateManager.getDaysOfMonth(month)
    }

    fun getDayOfWeek(month: Int, day: Int): String {
        return dateManager.getDayOfWeek(month, day)
    }

    fun getFullMonthName(monthNumber: Int): String {
        return dateManager.getFullMonthName(monthNumber)
    }
}