package com.example.iptvplayer.view.channels

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iptvplayer.data.Utils
import com.example.iptvplayer.domain.GetDvrRangeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class ArchiveViewModel @Inject constructor(
    private val getDvrRangeUseCase: GetDvrRangeUseCase
): ViewModel() {
    private val _archiveSegmentUrl: MutableLiveData<String> = MutableLiveData()
    val archiveSegmentUrl: LiveData<String> = _archiveSegmentUrl

    private val _seekSeconds: MutableLiveData<Int> = MutableLiveData(0)
    val seekSeconds: LiveData<Int> = _seekSeconds

    private val _currentTime: MutableLiveData<Long> = MutableLiveData()
    val currentTime: LiveData<Long> = _currentTime

    private val _liveTime: MutableLiveData<Long> = MutableLiveData()
    val liveTime: LiveData<Long> = _liveTime

    private val _dvrRange: MutableLiveData<Pair<Long, Long>> = MutableLiveData()
    val dvrRange: LiveData<Pair<Long, Long>> = _dvrRange

    // for example march -> 3
    private val _dvrMonth: MutableLiveData<Int> = MutableLiveData()
    val dvrMonth: LiveData<Int> = _dvrMonth

    // for example 3 march -> 3
    private val _dvrFirstAndLastDay: MutableLiveData<Pair<Int, Int>> = MutableLiveData()
    val dvrFirstAndLastDay: LiveData<Pair<Int, Int>> = _dvrFirstAndLastDay

    fun setLiveTime(time: Long) {
        Log.i("LIVE TIME", time.toString())
        _liveTime.value = time
    }

    fun seekBack() {
        _seekSeconds.value?.let { seek ->
            _seekSeconds.postValue(
                if (seek == 0) -1
                else {
                    // 256 is a rewind limit (~4 minutes) to prevent user from rewinding archive
                    // exponentially. this way user will not rewind archive more then by 4 minutes
                    // at once
                    if (seek <= -64) seek - 64
                    else seek * 2
                }
            )
        }
    }

    fun seekForward() {
        _seekSeconds.value?.let { seek ->
            _seekSeconds.postValue(
                if (seek == 0) 1
                else {
                    if (seek >= 64) seek + 64
                    else seek * 2
                }
            )
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

    @RequiresApi(Build.VERSION_CODES.O)
    fun getDvrRange(streamName: String) {
        viewModelScope.launch {
            val dvrRange = getDvrRangeUseCase.getDvrRange(streamName)
            _dvrRange.value = dvrRange

            val dvrRangeStartCalendar = Utils.getCalendar(dvrRange.first)
            val dvrRangeEndCalendar = Utils.getCalendar(dvrRange.second)

            getDvrMonth(dvrRangeStartCalendar)
            getDvrFirstAndLastDays(dvrRangeStartCalendar, dvrRangeEndCalendar)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getArchiveUrl(url: String) {
        Log.i("GET ARCHIVE URL", "GET ARCHIVE URL ${currentTime.value}")
        if (url == "") return
        viewModelScope.launch {
            seekSeconds.value?.let { seek ->
                currentTime.value?.let { time ->
                    val datePattern = "EEEE d MMMM HH:mm:ss"
                    Log.i("REALLY", time.toString())
                    val baseUrl = url.substring(0, url.lastIndexOf("/") + 1)

                    if (seek == 0) {
                        val archiveUrl = baseUrl + "index-$time-now.m3u8"
                        _archiveSegmentUrl.value = archiveUrl
                    } else {
                        val startTime =  time + seek
                        val archiveUrl = baseUrl + "index-$startTime-now.m3u8"
                        _archiveSegmentUrl.value = archiveUrl
                        _seekSeconds.value = 0
                    }
                }
            }
        }
    }

    fun setCurrentTime(time: Long) {
        if (time != 0L) {
            _currentTime.value = time
            Log.i("TIME", _currentTime.value.toString())
        }
    }
}