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

    fun seekBack() {
        _seekSeconds.value?.let { seek ->
            _seekSeconds.postValue(if (seek == 0) -1 else seek * 2)
            Log.i("seek", (seek * 2).toString())
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getArchiveUrl(url: String, currentTime: Long) {
        viewModelScope.launch {
            seekSeconds.value?.let { seek ->
                val datePattern = "EEEE d MMMM HH:mm:ss"
                Log.i("seek ger archive", seek.toString())
                val startTime =  currentTime + seek
                val baseUrl = url.substring(0, url.lastIndexOf("/") + 1)
                val archiveUrl = baseUrl + "index-$startTime-now.m3u8"
                _archiveSegmentUrl.postValue(archiveUrl)
                _seekSeconds.postValue(0)
            }
        }
    }
}