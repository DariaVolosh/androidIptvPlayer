package com.example.iptvplayer.view.channels

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArchiveViewModel @Inject constructor(

): ViewModel() {
    private val _archiveSegmentUrl: MutableLiveData<String> = MutableLiveData()
    val archiveSegmentUrl: LiveData<String> = _archiveSegmentUrl

    private val _seekSeconds: MutableLiveData<Int> = MutableLiveData(0)
    val seekSeconds: LiveData<Int> = _seekSeconds

    private val _currentTime: MutableLiveData<Long> = MutableLiveData()
    val currentTime: LiveData<Long> = _currentTime

    private val _liveTime: MutableLiveData<Long> = MutableLiveData()
    val liveTime: LiveData<Long> = _liveTime

    fun setLiveTime(time: Long) {
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
                    if (seek <= -256) seek - 256
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
                    if (seek >= 256) seek + 256
                    else seek * 2
                }
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getArchiveUrl(url: String) {
        Log.i("GET ARCHIVE URL", "GET ARCHIVE URL ${currentTime.value}")
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
        _currentTime.value = time
        Log.i("TIME", _currentTime.value.toString())
    }
}