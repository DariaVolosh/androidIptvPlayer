package com.example.iptvplayer.view.player

import android.util.Log
import com.example.iptvplayer.data.repositories.MediaDataSource
import com.example.iptvplayer.domain.media.GetMediaDataSourceUseCase
import com.example.iptvplayer.domain.media.HandleNextSegmentRequestedUseCase
import com.example.iptvplayer.domain.media.SetMediaUrlUseCase
import com.example.iptvplayer.domain.sharedPrefs.SharedPreferencesUseCase
import com.example.iptvplayer.view.time.IS_LIVE_KEY
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tv.danmaku.ijk.media.player.IMediaPlayer
import tv.danmaku.ijk.media.player.IMediaPlayer.OnBufferingUpdateListener
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import java.util.LinkedList
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaManager @Inject constructor(
    private val sharedPreferencesUseCase: SharedPreferencesUseCase,
    private val getMediaDataSourceUseCase: GetMediaDataSourceUseCase,
    private val handleNextSegmentRequestedUseCase: HandleNextSegmentRequestedUseCase,
    private val setMediaUrlUseCase: SetMediaUrlUseCase,
) {
    // player instance
// THIS IS THE SINGLE SOURCE OF TRUTH FOR YOUR PLAYER INSTANCE
    private val _ijkPlayer = MutableStateFlow<IjkMediaPlayer?>(null)
    val ijkPlayer: StateFlow<IjkMediaPlayer?> = _ijkPlayer

    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO) // Own scope

    // player states
    private val _isSeeking: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isSeeking: StateFlow<Boolean> = _isSeeking

    private val _isLive: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val isLive: StateFlow<Boolean> = _isLive

    private val _isDataSourceSet: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isDataSourceSet: StateFlow<Boolean> = _isDataSourceSet

    private val _isPlaybackStarted: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isPlaybackStarted: StateFlow<Boolean> = _isPlaybackStarted

    private val _newSegmentsNeeded: MutableStateFlow<Boolean> = MutableStateFlow(true)
    var newSegmentsNeeded: StateFlow<Boolean> = _newSegmentsNeeded

    private val _isPaused: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused

    private val _lastSegmentFromQueue: MutableStateFlow<String> = MutableStateFlow("")
    val lastSegmentFromQueue: StateFlow<String> = _lastSegmentFromQueue

    private val _isFirstSegmentRead: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isFirstSegmentRead: StateFlow<Boolean> = _isFirstSegmentRead

    private val urlQueue = LinkedList<String>()
    private var segmentRequestJob: Job? = null

    // player time states
    private val _isReset: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val isReset: StateFlow<Boolean> = _isReset

    private val DVR_SEGMENTS_THREESHOLD = 5 // we fetch 180 seconds of ts segments ~30 segments
    private val LIVE_SEGMENTS_THREESHOLD = 1 // we fetch 24 seconds of live ts segments ~4 segments

    val datePattern = "EEEE d MMMM HH:mm:ss"

    fun initializePlayer() {
        Log.i("init executed", "true")
        _ijkPlayer.value = IjkMediaPlayer()
        IjkMediaPlayer.native_setLogLevel(IjkMediaPlayer.IJK_LOG_DEBUG)

        setOnPreparedListener()
        setOnInfoListener()
        setOnBufferingUpdateListener()
        setDataSource()
        setOnSegmentRequestCallback()
    }

    fun play() {
        ijkPlayer.value?.start()
        _isPaused.value = false
        Log.i("paused", "false")
    }

    fun pause() {
        if (!_isPaused.value) {
            Log.i("is paused", "true")
            ijkPlayer.value?.pause()
            _isPaused.value = true
        }
    }

    fun setIsPlayerReset(isReset: Boolean) {
        _isReset.value = isReset
        Log.i("is player reset?", _isReset.value.toString())
    }

    fun updateIsDataSourceSet(isDataSourceSet: Boolean) {
        _isDataSourceSet.value = isDataSourceSet
    }

    fun setOnPreparedListener() {
        ijkPlayer.value?.setOnPreparedListener { mp ->
            Log.i("on prepared", "yea")
            Log.i("YEA SET PLAYER!", "${ijkPlayer.value?.dataSource.toString()} LOL DATA SOURCE SET? ON PREPARED")
            Log.i("YEA SET PLAYER!", "${mp.toString()} LOL DATA SOURCE SET? ON PREPARED")

            _isDataSourceSet.value = true
            Log.i("is data source set manager", isDataSourceSet.value.toString())
        }
    }

    fun setOnInfoListener() {
        ijkPlayer.value?.setOnInfoListener { mp, what, extra ->
            Log.i("player state", what.toString())
            when (what) {
                IjkMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START -> {
                    Log.i("rendering video start", "true")
                    _isPlaybackStarted.value = true
                    true
                }
                else -> false
            }
        }
    }

    fun setOnBufferingUpdateListener() {
        ijkPlayer.value?.setOnBufferingUpdateListener(object: OnBufferingUpdateListener {
            override fun onBufferingUpdate(mp: IMediaPlayer?, percent: Int) {
                Log.i("segments", "buffering update $percent")
            }
        })
    }

    fun setDataSource() {
        Log.i("YEA SET PLAYER!", "${ijkPlayer.value.toString()} just set source")
        val dataSource = getMediaDataSourceUseCase.getMediaDataSource()
        Log.i("YEA SET PLAYER!", "${dataSource} LOL DATA SOURCE RECEIVED")
        ijkPlayer.value?.setDataSource(dataSource)
        Log.i("YEA SET PLAYER!", "${ijkPlayer.value.toString()} just set source")
        Log.i("YEA SET PLAYER!", "${ijkPlayer.value?.dataSource.toString()} LOL DATA SOURCE SET?")
    }

    fun updateIsSeeking(isSeeking: Boolean) {
        _isSeeking.value = isSeeking
    }

    fun updateIsLive(isLive: Boolean) {
        Log.i("update is live", "$isLive")
        _isLive.value = isLive
        sharedPreferencesUseCase.saveBooleanValue(IS_LIVE_KEY, isLive)
    }

    fun resetPlayer() {
        Log.i("reset player called", "true")
        ijkPlayer.value?.reset()
        urlQueue.clear()
        _isFirstSegmentRead.value = false
        _isDataSourceSet.value = false
        _isPlaybackStarted.value = false
        setIsPlayerReset(true)
        _newSegmentsNeeded.value = true
    }

    fun updateAreNewSegmentsNeeded(areNeeded: Boolean) {
        _newSegmentsNeeded.value = areNeeded
    }

    fun newSegmentsNeeded(): Boolean {
        Log.i("is live", "is live: ${isLive.value} url queue size:${urlQueue.size}")
        return if (isLive.value) urlQueue.size <= LIVE_SEGMENTS_THREESHOLD
        else urlQueue.size <= DVR_SEGMENTS_THREESHOLD
    }

    private fun startPlayback(mediaSource: MediaDataSource) {
        try {
            Log.i("START PLAYBACK VIEW MODEL", "startPlayback $mediaSource")
            ijkPlayer.value?.prepareAsync()
            _isFirstSegmentRead.value = true
        } catch (e: Exception) {
            Log.i("lel", "startPlayback exception ${e.message}")
        }
    }

    suspend fun setNextUrl(url: String) {
        setMediaUrlUseCase.setMediaUrl(url) { mediaSource ->
            if (!_isFirstSegmentRead.value) startPlayback(mediaSource)
        }
    }

    fun setOnSegmentRequestCallback() {
        handleNextSegmentRequestedUseCase.setOnNextSegmentRequestedCallback {
            segmentRequestJob?.cancel()
            segmentRequestJob = managerScope.launch {
                withContext(Dispatchers.IO) {
                    while (true) {
                        urlQueue.poll()?.let { url ->
                            Log.i("YEA SET PLAYER!", "${ijkPlayer.value?.dataSource.toString()} POLL URL")

                            Log.i("ijk player!!! instance", ijkPlayer.value.toString())
                            Log.i("data source!!! instance", getMediaDataSourceUseCase.getMediaDataSource().toString())

                            updateAreNewSegmentsNeeded(newSegmentsNeeded())
                            Log.i("is live", "new segments needed: ${newSegmentsNeeded.value}")
                            Log.i("queue size", urlQueue.size.toString())
                            Log.i("buffering", "pulled to buffer $url")
                            setNextUrl(url)
                            segmentRequestJob?.cancel()
                        }

                        delay(2000)
                    }
                }
            }
        }
    }

    fun updateLastSegmentFromQueue(segment: String) {
        _lastSegmentFromQueue.value = segment
    }

    fun isPlayerInstanceAvailable(): Boolean {
        return true
    }

    fun getUrlQueue(): LinkedList<String> {
        return urlQueue
    }
}