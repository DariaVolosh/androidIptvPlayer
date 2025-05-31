package com.example.iptvplayer.view.channelsAndEpgRow

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iptvplayer.data.Utils
import com.example.iptvplayer.domain.archive.GetDvrRangeUseCase
import com.example.iptvplayer.domain.sharedPrefs.SharedPreferencesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject

const val CURRENT_TIME_KEY = "current_time"
const val IS_LIVE_KEY = "is_live"

@HiltViewModel
class ArchiveViewModel @Inject constructor(
    private val getDvrRangeUseCase: GetDvrRangeUseCase,
    private val sharedPreferencesUseCase: SharedPreferencesUseCase
): ViewModel() {
    private val _archiveSegmentUrl: MutableStateFlow<String> = MutableStateFlow("")
    val archiveSegmentUrl: StateFlow<String> = _archiveSegmentUrl

    private val _currentChannelDvrRange: MutableStateFlow<Pair<Long, Long>> = MutableStateFlow(Pair(0,0))
    val currentChannelDvrRange: StateFlow<Pair<Long, Long>> = _currentChannelDvrRange

    private val _focusedChannelDvrRange: MutableStateFlow<Pair<Long, Long>> = MutableStateFlow(Pair(0,0))
    val focusedChannelDvrRange: StateFlow<Pair<Long, Long>> = _focusedChannelDvrRange

    // for example 3 march -> 3
    private val _dvrFirstAndLastDay: MutableLiveData<Pair<Int, Int>> = MutableLiveData()
    val dvrFirstAndLastDay: LiveData<Pair<Int, Int>> = _dvrFirstAndLastDay

    private val _dvrFirstAndLastMonth: MutableLiveData<Pair<Int, Int>> = MutableLiveData()
    val dvrFirstAndLastMonth: MutableLiveData<Pair<Int, Int>> = _dvrFirstAndLastMonth

    private val _rewindError: MutableLiveData<String> = MutableLiveData()
    val rewindError: LiveData<String> = _rewindError

    var focusedChannelDvrCollectionJob: Job? = null
    var currentChannelDvrCollectionJob: Job? = null

    fun setRewindError(error: String) {
        _rewindError.value = error
    }

    private fun getDvrFirstAndLastDays(
        firstDayCalendar: Calendar,
        lastDayCalendar: Calendar
    ) {
        val dvrFirstDay = Utils.getCalendarDay(firstDayCalendar)
        val dvrLastDay = Utils.getCalendarDay(lastDayCalendar)

        _dvrFirstAndLastDay.value = Pair(dvrFirstDay, dvrLastDay)
    }

    private fun getDvrFirstAndLastMonths(
        firstDayCalendar: Calendar,
        lastDayCalendar: Calendar
    ) {
        val dvrFirstMonth = Utils.getCalendarMonth(firstDayCalendar) + 1
        val dvrLastMonth = Utils.getCalendarMonth(lastDayCalendar) + 1

        _dvrFirstAndLastMonth.value = Pair(dvrFirstMonth, dvrLastMonth)
    }

    suspend fun getDvrRange(isCurrentChannel: Boolean, streamName: String) {
        val dvrRange = getDvrRangeUseCase.getDvrRange(streamName)

        withContext(Dispatchers.Main) {
            if (isCurrentChannel) {
                _currentChannelDvrRange.value = dvrRange
                val dvrRangeStartCalendar = Utils.getCalendar(dvrRange.first)
                val dvrRangeEndCalendar = Utils.getCalendar(dvrRange.second)

                getDvrFirstAndLastDays(dvrRangeStartCalendar, dvrRangeEndCalendar)
                getDvrFirstAndLastMonths(dvrRangeStartCalendar, dvrRangeEndCalendar)
            } else {
                _focusedChannelDvrRange.value = dvrRange
            }
        }
    }

    fun getArchiveUrl(url: String, currentTime: Long) {
        if (url == "") return
        Log.i("get archive url method", "called")
        val datePattern = "EEEE d MMMM HH:mm:ss"
        Log.i("REALLY", Utils.formatDate(currentTime, datePattern))
        Log.i("base url given", url.toString())
        val baseUrl = url.substring(0, url.lastIndexOf("/") + 1)
        val token = url.substring(url.lastIndexOf("=") + 1, url.length)

        val archiveUrl = baseUrl + "index-$currentTime-now.m3u8?token=$token"
        Log.i("base url", "$baseUrl $token $archiveUrl")
        // checking again, because if the rewind was not continuous, time did not change,
        // therefore still in present, rewinding to current time would result in
        // file not found exception
        viewModelScope.launch {
            if (isStreamWithinDvrRange(currentTime)) {
                _archiveSegmentUrl.value = archiveUrl
            }
        }
    }

    suspend fun isStreamWithinDvrRange(newTime: Long): Boolean =
        withContext(Dispatchers.IO) {
            val dvrRange = currentChannelDvrRange
                .filter { it.first != 0L }
                .first()
            Log.i("dvr range collected", dvrRange.toString())
            val datePattern = "EEEE d MMMM HH:mm:ss"
            Log.i("dvr range compare", "${Utils.formatDate(dvrRange.first, datePattern)}")
            Log.i("dvr range compare", "${Utils.formatDate(newTime, datePattern)}")
            Log.i("dvr range compare", "${Utils.formatDate(dvrRange.second, datePattern)}")

            val isMoreThanFirstBound = newTime >= dvrRange.first
            val isLessThanSecondBound = newTime <= dvrRange.second

            val isWithinDvrRange = isMoreThanFirstBound && isLessThanSecondBound
            isWithinDvrRange
        }

    fun startDvrCollectionJob(isCurrentChannel: Boolean, streamName: String) {
        val jobBody: suspend CoroutineScope.() -> Unit = {
            withContext(Dispatchers.IO) {
                while (true) {
                    getDvrRange(isCurrentChannel, streamName)
                    delay(5000)
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
}