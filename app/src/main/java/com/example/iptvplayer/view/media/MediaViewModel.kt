package com.example.iptvplayer.view.media

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.iptvplayer.di.IoDispatcher
import com.example.iptvplayer.domain.media.MediaManager
import com.example.iptvplayer.domain.media.MediaPlaybackOrchestrator
import com.example.iptvplayer.domain.time.TimeOrchestrator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import javax.inject.Inject

@HiltViewModel
class MediaViewModel @Inject constructor(
    private val mediaPlaybackOrchestrator: MediaPlaybackOrchestrator,
    private val mediaManager: MediaManager,
    private val timeOrchestrator: TimeOrchestrator,
    @IoDispatcher private val viewModelScope: CoroutineScope
): ViewModel() {

    val isDataSourceSet: StateFlow<Boolean> = mediaPlaybackOrchestrator.isDataSourceSet.stateIn(
        viewModelScope, SharingStarted.Eagerly, false
    )

    val ijkPlayer: StateFlow<IjkMediaPlayer?> = mediaManager.ijkPlayer.stateIn(
        viewModelScope, SharingStarted.Eagerly, null
    )

    val isPaused: StateFlow<Boolean> = mediaPlaybackOrchestrator.isPaused.stateIn(
        viewModelScope, SharingStarted.Eagerly, false
    )

    val isSeeking: StateFlow<Boolean> = mediaPlaybackOrchestrator.isSeeking.stateIn(
        viewModelScope, SharingStarted.Eagerly, false
    )

    /*val isLive: StateFlow<StreamTypeState> = mediaPlaybackOrchestrator.streamTypeState.stateIn(
        viewModelScope, SharingStarted.Eagerly, StreamTypeState.LIVE
    )*/

    val isLive = MutableStateFlow(true)

    val isPlaybackStarted: StateFlow<Boolean> = mediaPlaybackOrchestrator.isPlaybackStarted.stateIn(
        viewModelScope, SharingStarted.Eagerly, true
    )

    private val _seekSecondsFlow: MutableStateFlow<Int> = MutableStateFlow(0)
    val seekSecondsFlow: StateFlow<Int> = _seekSecondsFlow

    val newSegmentsNeeded: StateFlow<Boolean> = mediaPlaybackOrchestrator.newSegmentsNeeded.stateIn(
        viewModelScope, SharingStarted.Eagerly, true
    )

    val _isSurfaceAttached: MutableStateFlow<Boolean> = MutableStateFlow(false)


    private var _seekSeconds = 0

    fun getLastSegmentFromQueue(): String {
        //return lastSegmentFromQueue.value
        return ""
    }

    fun updateLiveTime(time: Long) {
        timeOrchestrator.updateLiveTime(time)
    }

    fun updateCurrentTime(time: Long) {
        timeOrchestrator.updateCurrentTime(time)
    }

    fun updateIsSeeking(isSeeking: Boolean) {
        mediaPlaybackOrchestrator.updateIsSeeking(isSeeking)
    }

    fun updateIsLive(isLive: Boolean) {
        mediaPlaybackOrchestrator.updateIsLive(isLive)
    }

    fun onSeekFinish() {
        viewModelScope.launch {
            Log.i("on seek finish", 0.toString())
            delay(100)
            _seekSecondsFlow.value = 0
            _seekSeconds = 0
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
            _seekSecondsFlow.value = _seekSeconds
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

    fun pause() {
        mediaPlaybackOrchestrator.pausePlayerPlayback()
    }

    fun play() {
        mediaPlaybackOrchestrator.startPlayerPlayback()
    }

    fun resetPlayer() {
        //cancelTsCollectingJob()
        mediaPlaybackOrchestrator.resetPlayer()
    }

    fun startCollectingSegments(channelUrl: String) {
        //mediaOrchestrator.startPlaylistParsing(channelUrl)
    }

    fun cancelTsCollectingJob() {
        //mediaOrchestrator.cancelTsCollectingJob()
    }

    fun updateIsSurfaceAttached(isAttached: Boolean) {
        _isSurfaceAttached.value = isAttached
    }

    fun startCurrentChannelPlayback() {
        //mediaOrchestrator.startCurrentChannelPlayback()
    }
}