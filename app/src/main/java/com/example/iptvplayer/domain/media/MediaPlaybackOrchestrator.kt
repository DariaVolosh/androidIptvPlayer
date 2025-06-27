package com.example.iptvplayer.domain.media

import com.example.iptvplayer.data.media.TsExtractor
import com.example.iptvplayer.data.repositories.MediaRepository
import com.example.iptvplayer.di.IoDispatcher
import com.example.iptvplayer.domain.channels.ChannelsManager
import com.example.iptvplayer.domain.sharedPrefs.SharedPreferencesUseCase
import com.example.iptvplayer.domain.time.IS_LIVE_KEY
import com.example.iptvplayer.retrofit.data.ChannelData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import java.util.LinkedList
import javax.inject.Inject
import javax.inject.Singleton

enum class StreamTypeState {
    INITIALIZING,
    LIVE,
    ARCHIVE,
    ERROR
}

@Singleton
class MediaPlaybackOrchestrator @Inject constructor(
    private val channelsManager: ChannelsManager,
    private val mediaManager: MediaManager,
    private val mediaRepository: MediaRepository,
    private val tsExtractor: TsExtractor,
    private val sharedPreferencesUseCase: SharedPreferencesUseCase,
    private val handleNextSegmentRequestedUseCase: HandleNextSegmentRequestedUseCase,
    private val setMediaUrlUseCase: SetMediaUrlUseCase,
    @IoDispatcher private val orchestratorScope: CoroutineScope
) {

    private val _streamTypeState: MutableStateFlow<StreamTypeState> =
        MutableStateFlow(StreamTypeState.INITIALIZING)
    val streamTypeState: StateFlow<StreamTypeState> = _streamTypeState


    val currentChannel: StateFlow<ChannelData> = channelsManager.currentChannel.stateIn(
        orchestratorScope, SharingStarted.Eagerly, ChannelData()
    )

    private val urlQueue = LinkedList<String>()
    private var segmentRequestJob: Job? = null
    private val DVR_SEGMENTS_THREESHOLD = 5 // we fetch 180 seconds of ts segments ~30 segments
    private val LIVE_SEGMENTS_THREESHOLD = 1 // we fetch 24 seconds of live ts segments ~4 segments

    private val _segmentsCollectingJob: MutableStateFlow<Job?> = MutableStateFlow(null)
    val segmentsCollectingJob: StateFlow<Job?> = _segmentsCollectingJob

    private val _newSegmentsNeeded: MutableStateFlow<Boolean> = MutableStateFlow(true)
    var newSegmentsNeeded: StateFlow<Boolean> = _newSegmentsNeeded

    // player states
    private val _isSeeking: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isSeeking: StateFlow<Boolean> = _isSeeking

    private val _isDataSourceSet: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isDataSourceSet: StateFlow<Boolean> = _isDataSourceSet

    private val _isPlaybackStarted: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isPlaybackStarted: StateFlow<Boolean> = _isPlaybackStarted

    private val _isPaused: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused

    init {
        orchestratorScope.launch {
            val cachedIsLive = sharedPreferencesUseCase.getBooleanValue(IS_LIVE_KEY)

            updateStreamTypeState(
                if (cachedIsLive) StreamTypeState.LIVE
                else StreamTypeState.ARCHIVE
            )
        }

        orchestratorScope.launch {
            mediaManager.ijkPlayer.first {
                ijkPlayer -> ijkPlayer != null
            }.let {
                mediaManager.setOnPreparedListener { _isDataSourceSet.value = true }
                mediaManager.setOnInfoListener { mp, what, extra ->
                    when (what) {
                        IjkMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START -> {
                            _isPlaybackStarted.value = true
                            true
                        }
                        else -> false
                    }
                }
                mediaManager.setDataSource(mediaRepository.getMediaDataSource())
                handleNextSegmentRequestedUseCase.setOnNextSegmentRequestedCallback {
                    onSegmentRequest()
                }
            }
        }
    }

    fun updateAreNewSegmentsNeeded(areNeeded: Boolean) {
        _newSegmentsNeeded.value = areNeeded
    }

    fun updateIsSeeking(isSeeking: Boolean) {
        _isSeeking.value = isSeeking
    }

    fun updateIsLive(isLive: Boolean) {
        updateStreamTypeState(
            if (isLive) StreamTypeState.LIVE
            else StreamTypeState.ARCHIVE
        )

        sharedPreferencesUseCase.saveBooleanValue(IS_LIVE_KEY, isLive)
    }

    fun newSegmentsNeeded(): Boolean =
        when(streamTypeState.value) {
            StreamTypeState.LIVE -> urlQueue.size <= LIVE_SEGMENTS_THREESHOLD
            StreamTypeState.ARCHIVE -> urlQueue.size <= DVR_SEGMENTS_THREESHOLD
            else -> false
        }

    fun onSegmentRequest() {
        segmentRequestJob?.cancel()
        segmentRequestJob = orchestratorScope.launch {
            while (true) {
                urlQueue.poll()?.let { url ->
                    updateAreNewSegmentsNeeded(newSegmentsNeeded())
                    setMediaUrlUseCase.setMediaUrl(url)
                    segmentRequestJob?.cancel()
                }

                delay(2000)
            }
        }
    }

    private suspend fun extractTsSegments(url: String) {
        val nestedUrls = tsExtractor.extractNestedPlaylistUrls(url)

        if (nestedUrls.isNotEmpty()) {
            for (nestedUrl in nestedUrls) {
                extractTsSegments(nestedUrl)
            }
        } else {
            val tsSegments = tsExtractor.extractTsSegmentUrls(url)
            for (tsSegment in tsSegments) {
                addUrlToQueue(tsSegment)
            }
        }
    }

    private fun startLiveSegmentsLoading(url: String) {
        _segmentsCollectingJob.value = orchestratorScope.launch {
            while (true) {
                extractTsSegments(url)
                delay(4000)
            }
        }
    }

    fun startLivePlayback() {
        orchestratorScope.launch {
            currentChannel.first {
                channel -> channel != ChannelData()
            }.let { currentChannel ->
                startLiveSegmentsLoading(currentChannel.channelUrl)
            }
        }
    }

    fun updateStreamTypeState(updatedState: StreamTypeState) {
        _streamTypeState.value = updatedState
    }

    fun startPlayerPlayback() {
        _isPaused.value = false
        mediaManager.play()
    }

    fun pausePlayerPlayback() {
        _isPaused.value = true
        mediaManager.pause()
    }

    fun resetPlayer() {
        urlQueue.clear()
        _isDataSourceSet.value = false
        _isPlaybackStarted.value = false
        _newSegmentsNeeded.value = true
        mediaManager.resetPlayer()
    }

    fun getLastTsSegmentFromQueue(): String {
        return urlQueue.last
    }

    fun getUrlQueueSize(): Int {
        return urlQueue.size
    }

    fun addUrlToQueue(url: String) {
        urlQueue.add(url)
    }
}