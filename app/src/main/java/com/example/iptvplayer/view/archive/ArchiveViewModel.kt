package com.example.iptvplayer.view.archive

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.iptvplayer.di.IoDispatcher
import com.example.iptvplayer.domain.archive.ArchiveManager
import com.example.iptvplayer.domain.archive.ArchiveOrchestrator
import com.example.iptvplayer.domain.archive.GetDvrRangesUseCase
import com.example.iptvplayer.domain.sharedPrefs.SharedPreferencesUseCase
import com.example.iptvplayer.domain.time.CalendarManager
import com.example.iptvplayer.domain.time.DateManager
import com.example.iptvplayer.retrofit.data.DvrRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject


enum class CurrentDvrInfoState {
    LOADING, // dvr info is loading
    NOT_AVAILABLE_GLOBAL, // error to load dvr info -> either api is not available or no dvr available
    AVAILABLE_GLOBAL, // dvr info is available
    PLAYING_IN_RANGE, // current time range is available
    GAP_DETECTED_AND_WAITING, // gap, playback stopped
    END_OF_DVR_REACHED // end of the dvr
}

data class DvrDaysRange(
    val firstDay: Int = -1,
    val lastDay: Int = -1
)

data class DvrMonthsRange(
    val firstMonth: Int = -1,
    val lastMonth: Int = -1
)

@HiltViewModel
class ArchiveViewModel @Inject constructor(
    private val getDvrRangesUseCase: GetDvrRangesUseCase,
    private val archiveManager: ArchiveManager,
    private val calendarManager: CalendarManager,
    private val dateManager: DateManager,
    private val archiveOrchestrator: ArchiveOrchestrator,
    private val sharedPreferencesUseCase: SharedPreferencesUseCase,
    @IoDispatcher private val viewModelScope: CoroutineScope
): ViewModel() {
    val archiveSegmentUrl: StateFlow<String> = archiveManager.archiveSegmentUrl.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), ""
    )

    val currentChannelDvrRanges: StateFlow<List<DvrRange>> = archiveManager.currentChannelDvrRanges.stateIn(
        viewModelScope, SharingStarted.Eagerly, emptyList()
    )

    val focusedChannelDvrRanges: StateFlow<List<DvrRange>> = archiveManager.focusedChannelDvrRanges.stateIn(
        viewModelScope, SharingStarted.Eagerly, emptyList()
    )

    // for example 3 march -> 3
    val dvrFirstAndLastDay: StateFlow<DvrDaysRange> = archiveManager.dvrFirstAndLastDay.stateIn(
        viewModelScope, SharingStarted.Eagerly, DvrDaysRange()
    )

    val dvrFirstAndLastMonth: StateFlow<DvrMonthsRange> = archiveManager.dvrFirstAndLastMonth.stateIn(
        viewModelScope, SharingStarted.Eagerly, DvrMonthsRange()
    )

    val rewindError: StateFlow<String> = archiveManager.rewindError.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), ""
    )

    val currentChannelDvrRange: StateFlow<Int> = archiveManager.currentChannelDvrRange.stateIn(
        viewModelScope, SharingStarted.Eagerly, -1
    )

    val currentChannelDvrInfoState: StateFlow<CurrentDvrInfoState> = archiveManager.currentChannelDvrInfoState.stateIn(
        viewModelScope, SharingStarted.Eagerly, CurrentDvrInfoState.LOADING
    )

    val focusedChannelDvrInfoState: StateFlow<CurrentDvrInfoState> = archiveManager.focusedChannelDvrInfoState.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), CurrentDvrInfoState.LOADING
    )

    val focusedChannelDvrRange: StateFlow<Int> = archiveManager.focusedChannelDvrRange.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), -1
    )

    var focusedChannelDvrCollectionJob: Job? = null
    var currentChannelDvrCollectionJob: Job? = null

    private fun getDvrFirstAndLastDays(
        firstDayCalendar: Calendar,
        lastDayCalendar: Calendar
    ) {
        val dvrFirstDay = calendarManager.getCalendarDay(firstDayCalendar)
        val dvrLastDay = calendarManager.getCalendarDay(lastDayCalendar)

        archiveManager.updateDvrFirstAndLastDay(DvrDaysRange(dvrFirstDay, dvrLastDay))
    }

    private fun getDvrFirstAndLastMonths(
        firstDayCalendar: Calendar,
        lastDayCalendar: Calendar
    ) {
        val dvrFirstMonth = calendarManager.getCalendarMonth(firstDayCalendar) + 1
        val dvrLastMonth = calendarManager.getCalendarMonth(lastDayCalendar) + 1

        archiveManager.updateDvrFirstAndLastMonth(DvrMonthsRange(dvrFirstMonth, dvrLastMonth))
    }


    suspend fun getDvrRanges(isCurrentChannel: Boolean, streamName: String) {
        val dvrRanges = getDvrRangesUseCase.getDvrRanges(streamName)
        Log.i("got dvr ranges", dvrRanges.toString())
        val datePattern = "dd MMMM HH:mm:ss"
        for (dvrRange in dvrRanges) {
            Log.i("got dvr range: ", "" +
                    "from: ${dateManager.formatDate(dvrRange.from, datePattern)} " +
                    "stop: ${dateManager.formatDate(dvrRange.from + dvrRange.duration, datePattern)}" +
                    "")
        }
        archiveManager.setDvrRanges(isCurrentChannel, dvrRanges)

        withContext(Dispatchers.Main) {
            if (isCurrentChannel && dvrRanges.isNotEmpty()) {
                val dvrRangeStartCalendar = calendarManager.getCalendar(dvrRanges[0].from)
                val dvrRangeEndCalendar = calendarManager.getCalendar(
                    dvrRanges.last().from + dvrRanges.last().duration
                )

                getDvrFirstAndLastDays(dvrRangeStartCalendar, dvrRangeEndCalendar)
                getDvrFirstAndLastMonths(dvrRangeStartCalendar, dvrRangeEndCalendar)
            }
        }
    }

    fun setRewindError(error: String) {
        archiveManager.setRewindError(error)
    }

    fun setCurrentDvrInfoState(isCurrentChannel: Boolean, state: CurrentDvrInfoState) {
        archiveManager.updateDvrInfoState(isCurrentChannel, state)
    }

    suspend fun isStreamWithinDvrRange(newTime: Long): Boolean =
        withContext(Dispatchers.IO) {
            val dvrRanges = currentChannelDvrRanges
                .filter { it.isNotEmpty() }
                .first()
            Log.i("dvr range collected", dvrRanges.toString())
            //Log.i("dvr range compare", "${Utils.formatDate(dvrRange.first, datePattern)}")
            //Log.i("dvr range compare", "${Utils.formatDate(newTime, datePattern)}")
            //Log.i("dvr range compare", "${Utils.formatDate(dvrRange.second, datePattern)}")

            var isWithinDvrRange = false

            for (dvrRange in dvrRanges) {
                val isMoreThanFirstBound = newTime >= dvrRange.from
                val isLessThanSecondBound = newTime <= dvrRange.from + dvrRange.duration
                if (isMoreThanFirstBound && isLessThanSecondBound) {
                    isWithinDvrRange = true
                    break
                }
            }

            isWithinDvrRange
        }


    fun getArchiveUrl(channelUrl: String, currentTime: Long) {
        println("get archive url $channelUrl")
        viewModelScope.launch {
            archiveManager.getArchiveUrl(channelUrl, currentTime)
        }
    }

    suspend fun startDvrCollectionJob(isCurrentChannel: Boolean, streamName: String) {
        val jobBody: suspend CoroutineScope.() -> Unit = {
            withContext(Dispatchers.IO) {
                while (true) {
                    getDvrRanges(isCurrentChannel, streamName)
                    delay(60000)
                }
            }
        }
        if (isCurrentChannel) {
            currentChannelDvrCollectionJob?.cancel()
            currentChannelDvrCollectionJob = viewModelScope.launch(block = jobBody)
        } else {
            focusedChannelDvrCollectionJob?.cancel()
            focusedChannelDvrCollectionJob = viewModelScope.launch(block = jobBody)
        }
    }

    fun determineCurrentDvrRange(isCurrentChannel: Boolean, currentTime: Long) {
        viewModelScope.launch {
            archiveOrchestrator.determineCurrentDvrRange(isCurrentChannel, currentTime)
        }
    }

    fun getDvrRange(isCurrentChannel: Boolean): DvrRange {
        if (isCurrentChannel) {
            return DvrRange(0,0)
        } else {
            return focusedChannelDvrRanges.value.getOrNull(focusedChannelDvrRange.value) ?: DvrRange(0,0)
        }
    }
}