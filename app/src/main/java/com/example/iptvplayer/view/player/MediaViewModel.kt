package com.example.iptvplayer.view.player

import android.os.Looper
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iptvplayer.data.Utils
import com.example.iptvplayer.data.repositories.MediaDataSource
import com.example.iptvplayer.domain.GetMediaDataSourceUseCase
import com.example.iptvplayer.domain.GetTsSegmentsUseCase
import com.example.iptvplayer.domain.HandleNextSegmentRequestedUseCase
import com.example.iptvplayer.domain.SetMediaUrlUseCase
import com.example.iptvplayer.domain.SharedPreferencesUseCase
import com.example.iptvplayer.view.channelsAndEpgRow.CURRENT_TIME_KEY
import com.example.iptvplayer.view.channelsAndEpgRow.IS_LIVE_KEY
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import java.util.LinkedList
import javax.inject.Inject

@HiltViewModel
class MediaViewModel @Inject constructor(
    private val getTsSegmentsUseCase: GetTsSegmentsUseCase,
    private val getMediaDataSourceUseCase: GetMediaDataSourceUseCase,
    private val setMediaUrlUseCase: SetMediaUrlUseCase,
    private val handleNextSegmentRequestedUseCase: HandleNextSegmentRequestedUseCase,
    private val sharedPreferencesUseCase: SharedPreferencesUseCase
): ViewModel() {
    var ijkPlayer: IjkMediaPlayer? = null

    private val _isDataSourceSet: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isDataSourceSet: StateFlow<Boolean> = _isDataSourceSet

    private val _isPaused: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused

    private val _isSeeking: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isSeeking: StateFlow<Boolean> = _isSeeking

    private val _isLive: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val isLive: StateFlow<Boolean> = _isLive

    private val _isPlaybackStarted: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isPlaybackStarted: StateFlow<Boolean> = _isPlaybackStarted

    private val _currentTime: MutableStateFlow<Long> = MutableStateFlow(0L)
    val currentTime: StateFlow<Long> = _currentTime

    private val _liveTime: MutableStateFlow<Long> = MutableStateFlow(0L)
    val liveTime: StateFlow<Long> = _liveTime

    private val _seekSecondsFlow: MutableStateFlow<Int> = MutableStateFlow(0)
    val seekSecondsFlow: StateFlow<Int> = _seekSecondsFlow

    private var _seekSeconds = 0

    private val urlQueue = LinkedList<String>()
    private var isPlayerReset = true
    private var firstSegmentRead = false

    private var tsJob: Job? = null
    private var segmentRequestJob: Job? = null

    init {
        val cachedCurrentTime = sharedPreferencesUseCase.getLongValue(CURRENT_TIME_KEY)
        val isLive = sharedPreferencesUseCase.getBooleanValue(IS_LIVE_KEY)
        val datePattern = "EEEE d MMMM HH:mm:ss"
        Log.i("PREFS", "current time: ${Utils.formatDate(cachedCurrentTime, datePattern)}")
        updateIsLive(isLive)

        viewModelScope.launch {
            val currentLiveTime = Utils.getGmtTime()
            setLiveTime(currentLiveTime)

            while (true) {
                setLiveTime(_liveTime.value + 1)
                delay(1000)
            }
        }

        viewModelScope.launch {
            val currentLiveTime = Utils.getGmtTime()

            if (cachedCurrentTime == 0L) {
                // current time was not fetched from cache, set it to live time
                setCurrentTime(currentLiveTime)
            } else {
                // current time is available from cache, set it
                setCurrentTime(cachedCurrentTime)
            }

            while (true) {
                if (!_isPaused.value && !_isSeeking.value) {
                    setCurrentTime(_currentTime.value + 1)
                }
                delay(1000)
            }
        }

        Log.i("PREFS", "is live: $isLive")
    }

    suspend fun setLiveTime(time: Long) {
        val datePattern = "EEEE d MMMM HH:mm:ss"
        Log.i("live time!", Utils.formatDate(time, datePattern))
        _liveTime.emit(time)
    }

    suspend fun setCurrentTime(time: Long) {
        if (time != 0L) {
            _currentTime.emit(time)
            sharedPreferencesUseCase.saveLongValue(CURRENT_TIME_KEY, time)

        }
    }

    fun updateIsSeeking(isSeeking: Boolean) {
        _isSeeking.value = isSeeking
    }

    fun updateIsLive(isLive: Boolean) {
        Log.i("update is live", "$isLive")
        _isLive.value = isLive
        sharedPreferencesUseCase.saveBooleanValue(IS_LIVE_KEY, isLive)
    }

    fun onSeekFinish() {
        Log.i("on seek finish", 0.toString())
        _seekSecondsFlow.value = 0
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

    private suspend fun setNextUrl(url: String) {
        setMediaUrlUseCase.setMediaUrl(url) { mediaSource ->
            if (!firstSegmentRead) startPlayback(mediaSource)
        }
    }

    private fun startPlayback(mediaSource: MediaDataSource) {
        try {
            Log.i("START PLAYBACK VIEW MODEL", "startPlayback $mediaSource")
            ijkPlayer?.prepareAsync()
            firstSegmentRead = true
        } catch (e: Exception) {
            Log.i("lel", "startPlayback exception ${e.message}")
        }
    }

    private fun setOnSegmentRequestCallback() {
        handleNextSegmentRequestedUseCase.setOnNextSegmentRequestedCallback {
            segmentRequestJob?.cancel()
            segmentRequestJob = viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    while (true) {
                        urlQueue.poll()?.let { url ->
                            setNextUrl(url)
                            segmentRequestJob?.cancel()
                        }

                        delay(2000)
                    }
                }
            }
        }
    }

    fun setMediaUrl(url: String) {
        if (url.isEmpty()) return
        Log.i("VIEW MODEL SET MEDIA URL", url)
        if (ijkPlayer != null && !isPlayerReset) {
            reset()
        }

        startTsCollectingJob(url)
    }

    fun pause() {
        if (!_isPaused.value) {
            ijkPlayer?.pause()
            _isPaused.value = true
        }
    }

    fun play() {
        ijkPlayer?.start()
        _isPaused.value = false
        Log.i("paused", "false")
    }

    fun reset() {
        ijkPlayer?.reset()
        tsJob?.cancel()
        urlQueue.clear()
        firstSegmentRead = false
        _isDataSourceSet.value = false
        _isPlaybackStarted.value = false
        isPlayerReset = true
    }

    fun startTsCollectingJob(url: String) {
        tsJob = viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val isOnMainThread = Looper.getMainLooper() == Looper.myLooper()
                Log.i("is on main thread channels list", "set media url $isOnMainThread")
                getTsSegmentsUseCase.extractTsSegments(url, isLive.value).collect { u ->
                    Log.i("collected", "collected $u")
                    if (ijkPlayer == null || isPlayerReset) {
                        ijkPlayer = IjkMediaPlayer()

                        ijkPlayer?.setOnPreparedListener {
                            Log.i("on prepared", "yea")
                            play()
                            _isDataSourceSet.value = true
                        }

                        ijkPlayer?.setOnInfoListener { mp, what, extra ->
                            when (what) {
                                IjkMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START -> {
                                    _isPlaybackStarted.value = true
                                    true
                                }
                                else -> false
                            }
                        }

                        ijkPlayer?.setDataSource(getMediaDataSourceUseCase.getMediaDataSource())
                        IjkMediaPlayer.native_setLogLevel(IjkMediaPlayer.IJK_LOG_DEBUG)

                        setOnSegmentRequestCallback()
                        setNextUrl(u)
                        isPlayerReset = false
                    } else {
                        urlQueue.add(u)
                    }
                }
            }
        }
    }
}