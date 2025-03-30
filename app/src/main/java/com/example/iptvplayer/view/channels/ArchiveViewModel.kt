package com.example.iptvplayer.view.channels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iptvplayer.data.Utils
import com.example.iptvplayer.domain.GetDvrRangeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class ArchiveViewModel @Inject constructor(
    private val getDvrRangeUseCase: GetDvrRangeUseCase
): ViewModel() {
    private val _archiveSegmentUrl: MutableLiveData<String> = MutableLiveData()
    val archiveSegmentUrl: LiveData<String> = _archiveSegmentUrl

    private val _seekSecondsFlow: MutableSharedFlow<Int> = MutableSharedFlow()
    val seekSecondsFlow: Flow<Int> = _seekSecondsFlow

    private var _seekSeconds = 0

    private val _currentTime: MutableLiveData<Long> = MutableLiveData()
    val currentTime: LiveData<Long> = _currentTime

    private val _liveTime: MutableLiveData<Long> = MutableLiveData()
    val liveTime: LiveData<Long> = _liveTime

    private val _isSeeking: MutableLiveData<Boolean> = MutableLiveData(false)
    val isSeeking: LiveData<Boolean> = _isSeeking

    private val _dvrRange: MutableLiveData<Pair<Long, Long>> = MutableLiveData()
    val dvrRange: LiveData<Pair<Long, Long>> = _dvrRange

    // for example march -> 3
    private val _dvrMonth: MutableLiveData<Int> = MutableLiveData()
    val dvrMonth: LiveData<Int> = _dvrMonth

    // for example 3 march -> 3
    private val _dvrFirstAndLastDay: MutableLiveData<Pair<Int, Int>> = MutableLiveData()
    val dvrFirstAndLastDay: LiveData<Pair<Int, Int>> = _dvrFirstAndLastDay

    private val _isContinuousRewind: MutableLiveData<Boolean> = MutableLiveData(false)
    val isContinuousRewind: LiveData<Boolean> = _isContinuousRewind

    private val _isLive: MutableLiveData<Boolean> = MutableLiveData()
    val isLive: LiveData<Boolean> = _isLive

    fun setLiveTime(time: Long) {
        Log.i("LIVE TIME", time.toString())
        _liveTime.value = time
    }

    fun onSeekFinish() {
        Log.i("on seek finish", 0.toString())
        viewModelScope.launch {
            _seekSecondsFlow.emit(0)
        }
    }

    fun seekBack(seconds: Int = 0) {
       viewModelScope.launch {
           _seekSeconds = if (seconds == 0) {
               if (_seekSeconds == 0) -20
               else {
                   // 60 is a rewind limit (1 minute) to prevent user from rewinding archive
                   // exponentially. this way user will not rewind archive more then by 1 minute
                   // at once
                   if (_seekSeconds <= -60) _seekSeconds - 60
                   else _seekSeconds * 2
               }
           } else {
               Log.i("archive view model", seconds.toString())
               -seconds
           }

           Log.i("seconds passed", _seekSeconds.toString())
           _seekSecondsFlow.emit(_seekSeconds)
           Log.i("executed function", "seek back")
       }
    }

    fun seekForward(seconds: Int = 0) {
        viewModelScope.launch {
            _seekSeconds = if (seconds == 0) {
                if (_seekSeconds == 0) 20
                else {
                    // 64 is a rewind limit (1 minute) to prevent user from rewinding archive
                    // exponentially. this way user will not rewind archive more then by 1 minute
                    // at once
                    if (_seekSeconds >= 60) _seekSeconds + 60
                    else _seekSeconds * 2
                }
            } else {
                Log.i("archive view model", seconds.toString())
                seconds
            }

            Log.i("seconds passed", _seekSeconds.toString())
            _seekSecondsFlow.emit(_seekSeconds)
            Log.i("executed function", "seek back")
        }
    }

    private fun getDvrFirstAndLastDays(
        firstDayCalendar: Calendar,
        lastDayCalendar: Calendar
    ) {
        val dvrFirstDay = Utils.getCalendarDay(firstDayCalendar)
        val dvrLastDay = Utils.getCalendarDay(lastDayCalendar)

        _dvrFirstAndLastDay.value = Pair(dvrFirstDay, dvrLastDay)
    }

    private fun getDvrMonth(firstDayCalendar: Calendar) {
        _dvrMonth.value = Utils.getCalendarMonth(firstDayCalendar) + 1
    }

    suspend fun getDvrRange(streamName: String) {
        val dvrRange = getDvrRangeUseCase.getDvrRange(streamName)
        _dvrRange.value = dvrRange

        val dvrRangeStartCalendar = Utils.getCalendar(dvrRange.first)
        val dvrRangeEndCalendar = Utils.getCalendar(dvrRange.second)

        getDvrMonth(dvrRangeStartCalendar)
        getDvrFirstAndLastDays(dvrRangeStartCalendar, dvrRangeEndCalendar)
    }

    fun getArchiveUrl(url: String) {
        Log.i("GET ARCHIVE URL", "GET ARCHIVE URL ${_seekSeconds}")
        if (url == "") return
        currentTime.value?.let { time ->
            Log.i("get archive url method", "called")
            val datePattern = "EEEE d MMMM HH:mm:ss"
            Log.i("REALLY", time.toString())
            val baseUrl = url.substring(0, url.lastIndexOf("/") + 1)

            val archiveUrl = baseUrl + "index-$time-now.m3u8"
            // checking again, because if the rewind was not continuous, time did not change,
            // therefore still in present, rewinding to current time would result in
            // file not found exception
            if (isStreamWithinDvrRange(time)) {
                _archiveSegmentUrl.value = archiveUrl
            }
            _isSeeking.value = false
            _seekSeconds = 0
        }
    }

    fun setCurrentTime(time: Long) {
        if (time != 0L) {
            _currentTime.value = time
            Log.i("TIME", _currentTime.value.toString())
        }
    }

    fun updateIsSeeking(isSeeking: Boolean) {
        _isSeeking.value = isSeeking
    }

    fun updateIsContinuousRewind(isContinuous: Boolean) {
        _isContinuousRewind.value = isContinuous
    }

    fun isStreamWithinDvrRange(newTime: Long): Boolean =
        dvrRange.value?.let { dvrRange ->
            val datePattern = "EEEE d MMMM HH:mm:ss"
            Log.i("dvr range compare","${Utils.formatDate(dvrRange.first, datePattern)}")
            Log.i("dvr range compare","${Utils.formatDate(newTime, datePattern)}")
            Log.i("dvr range compare","${Utils.formatDate(dvrRange.second, datePattern)}")

            newTime >= dvrRange.first && newTime <= dvrRange.second
        } ?: false

    fun updateIsLive(isLive: Boolean) {
        _isLive.value = isLive
    }
}