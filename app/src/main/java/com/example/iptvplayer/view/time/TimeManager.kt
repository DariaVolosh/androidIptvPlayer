package com.example.iptvplayer.view.time

import com.example.iptvplayer.data.NtpTimeClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeManager @Inject constructor(
    private val ntpClient: NtpTimeClient
) {
    private val _currentTime: MutableStateFlow<Long> = MutableStateFlow(0L)
    val currentTime: StateFlow<Long> = _currentTime

    private val _liveTime: MutableStateFlow<Long> = MutableStateFlow(0L)
    val liveTime: StateFlow<Long> = _liveTime

    fun updateCurrentTime(time: Long) {
        if (time != 0L) {
            val datePattern = "EEEE d MMMM HH:mm:ss"
            //Log.i("current time", "current: ${Utils.formatDate(time, datePattern)}")
            _currentTime.value = time
        }
    }

    fun updateLiveTime(time: Long) {
        val datePattern = "EEEE d MMMM HH:mm:ss"
        //Log.i("current time", "live: ${Utils.formatDate(time, datePattern)}")
        _liveTime.value = time
    }

    suspend fun getGmtTime(): Long =
        ntpClient.getGmtTime()
}