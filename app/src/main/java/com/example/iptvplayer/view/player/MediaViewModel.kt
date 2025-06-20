package com.example.iptvplayer.view.player

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iptvplayer.domain.sharedPrefs.SharedPreferencesUseCase
import com.example.iptvplayer.view.time.CURRENT_TIME_KEY
import com.example.iptvplayer.view.time.DateManager
import com.example.iptvplayer.view.time.IS_LIVE_KEY
import com.example.iptvplayer.view.time.TimeOrchestrator
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val sharedPreferencesUseCase: SharedPreferencesUseCase,
    private val playbackOrchestrator: PlaybackOrchestrator,
    private val mediaManager: MediaManager,
    private val timeOrchestrator: TimeOrchestrator,
    private val dateManager: DateManager
): ViewModel() {

    val isDataSourceSet: StateFlow<Boolean> = mediaManager.isDataSourceSet.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )

    val ijkPlayer: StateFlow<IjkMediaPlayer?> = mediaManager.ijkPlayer.stateIn(
        viewModelScope, SharingStarted.Eagerly, null
    )

    val isPaused: StateFlow<Boolean> = mediaManager.isPaused.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )

    val isSeeking: StateFlow<Boolean> = mediaManager.isSeeking.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )

    val isLive: StateFlow<Boolean> = mediaManager.isLive.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )

    val isPlaybackStarted: StateFlow<Boolean> = mediaManager.isPlaybackStarted.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )

    private val _seekSecondsFlow: MutableStateFlow<Int> = MutableStateFlow(0)
    val seekSecondsFlow: StateFlow<Int> = _seekSecondsFlow

    val newSegmentsNeeded: StateFlow<Boolean> = mediaManager.newSegmentsNeeded.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )

    val lastSegmentFromQueue: StateFlow<String> = mediaManager.lastSegmentFromQueue.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), ""
    )

    val _isSurfaceAttached: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isSurfaceAttached: StateFlow<Boolean> = _isSurfaceAttached


    private var _seekSeconds = 0

    init {
        val cachedCurrentTime = sharedPreferencesUseCase.getLongValue(CURRENT_TIME_KEY)
        val cachedIsLive = sharedPreferencesUseCase.getBooleanValue(IS_LIVE_KEY)
        val datePattern = "EEEE d MMMM HH:mm:ss"
        Log.i("PREFS", "current time: ${dateManager.formatDate(cachedCurrentTime, datePattern)}")

        mediaManager.updateIsLive(cachedIsLive)
        timeOrchestrator.initialize(cachedCurrentTime)

        Log.i("PREFS", "is live: $cachedIsLive")
    }

    fun getLastSegmentFromQueue(): String {
        return lastSegmentFromQueue.value
    }

    fun updateLiveTime(time: Long) {
        timeOrchestrator.updateLiveTime(time)
    }

    fun updateCurrentTime(time: Long) {
        timeOrchestrator.updateCurrentTime(time)
    }

    fun updateIsSeeking(isSeeking: Boolean) {
        mediaManager.updateIsSeeking(isSeeking)
    }

    fun updateIsLive(isLive: Boolean) {
        mediaManager.updateIsLive(isLive)
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
        mediaManager.pause()
    }

    fun play() {
        mediaManager.play()
    }

    fun resetPlayer() {
        //cancelTsCollectingJob()
        mediaManager.resetPlayer()
    }

    fun startTsCollectingJob(channelUrl: String) {
        playbackOrchestrator.startTsCollectingJob(channelUrl)
    }

    fun cancelTsCollectingJob() {
        playbackOrchestrator.cancelTsCollectingJob()
    }

    fun updateIsSurfaceAttached(isAttached: Boolean) {
        _isSurfaceAttached.value = isAttached
    }

    fun initializePlayer() {
        mediaManager.initializePlayer()
    }
}