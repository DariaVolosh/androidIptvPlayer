package com.example.iptvplayer.domain.media

import android.view.Surface
import com.example.iptvplayer.data.media.TsExtractor
import com.example.iptvplayer.data.repositories.MediaDataSource
import com.example.iptvplayer.data.repositories.MediaPlaybackRepository
import com.example.iptvplayer.di.IoDispatcher
import com.example.iptvplayer.domain.archive.ArchiveManager
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
    private val archiveManager: ArchiveManager,
    private val mediaPlaybackRepository: MediaPlaybackRepository,
    private val tsExtractor: TsExtractor,
    private val sharedPreferencesUseCase: SharedPreferencesUseCase,
    @IoDispatcher private val orchestratorScope: CoroutineScope
) {

    private val _streamTypeState: MutableStateFlow<StreamTypeState> =
        MutableStateFlow(StreamTypeState.INITIALIZING)
    val streamTypeState: StateFlow<StreamTypeState> = _streamTypeState

    val currentChannel: StateFlow<ChannelData> = channelsManager.currentChannel.stateIn(
        orchestratorScope, SharingStarted.Eagerly, ChannelData()
    )

    val archiveUrl: StateFlow<String> = archiveManager.archiveSegmentUrl.stateIn(
        orchestratorScope, SharingStarted.Eagerly, ""
    )

    var isOrchestratorInitialized: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private val urlQueue = LinkedList<String>()
    private val emittedSegmentUrls = mutableListOf<String>()
    private var segmentRequestJob: Job? = null
    private val LIVE_SEGMENTS_BUFFER_THREESHOLD = 20
    private val DVR_SEGMENTS_THREESHOLD = 5 // we fetch 180 seconds of ts segments ~30 segments
    private val LIVE_SEGMENTS_THREESHOLD = 1 // we fetch 24 seconds of live ts segments ~4 segments

    private val _segmentsLoadingJob: MutableStateFlow<Job?> = MutableStateFlow(null)
    val segmentsLoadingJob: StateFlow<Job?> = _segmentsLoadingJob

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
            println("$cachedIsLive isLIVE")

            updateStreamTypeState(
                if (cachedIsLive) StreamTypeState.LIVE
                else StreamTypeState.ARCHIVE
            )

            if (cachedIsLive) {
                startLivePlayback()
            } else {
                startArchivePlayback()
            }
        }
    }

    private suspend fun initializePlayerForPlayback() {
        mediaManager.ijkPlayer.first {
                ijkPlayer -> ijkPlayer != null
        }.let {
            mediaManager.setOnPreparedListener { _isDataSourceSet.value = true }
            mediaManager.setOnInfoListener { mp, what, extra ->
                when (what) {
                    IjkMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START -> {
                        _isPlaybackStarted.value = true
                        startPlayerPlayback()
                        true
                    }
                    else -> false
                }
            }

            val dataSource = mediaPlaybackRepository.getMediaDataSource()
            mediaManager.setDataSource(dataSource)
            dataSource.setOnNextSegmentRequestedCallback { onSegmentRequest(dataSource) }

            if (!isOrchestratorInitialized.value) {
                isOrchestratorInitialized.value = true
            }
        }
    }

    fun discardOldestHalfSegments() {
        emittedSegmentUrls.subList(0, LIVE_SEGMENTS_BUFFER_THREESHOLD / 2).clear()
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

    fun onSegmentRequest(mediaDataSource: MediaDataSource) {
        segmentRequestJob?.cancel()
        segmentRequestJob = orchestratorScope.launch {
            while (true) {
                urlQueue.poll()?.let { url ->
                    println("url polled $url")
                    println("queue size ${urlQueue.size}")
                    updateAreNewSegmentsNeeded(newSegmentsNeeded())
                    mediaDataSource.setMediaUrl(url)
                    segmentRequestJob?.cancel()
                }

                delay(2000)
            }
        }
    }

    suspend fun extractTsSegments(url: String) {
        val nestedUrls = tsExtractor.extractNestedPlaylistUrls(url)

        if (nestedUrls.isNotEmpty()) {
            for (nestedUrl in nestedUrls) {
                extractTsSegments(nestedUrl)
            }
        } else {
            val tsSegments = tsExtractor.extractTsSegmentUrls(url)
            for (tsSegment in tsSegments) {
                if (tsSegment !in emittedSegmentUrls) {
                    emittedSegmentUrls.add(tsSegment)
                    addUrlToQueue(tsSegment)
                }
            }
        }
    }

    fun startLiveSegmentsLoading(url: String) =
        orchestratorScope.launch {
            while (true) {
                println("extract $url")
                extractTsSegments(url)
                delay(4000)
            }
        }

    fun startArchiveSegmentsLoading(archiveUrl: String) =
        orchestratorScope.launch {
            extractTsSegments(archiveUrl)
        }

    suspend fun startArchivePlayback() {
        if (!isOrchestratorInitialized.value) {
            initializePlayerForPlayback()
        } else {
            resetPlaybackInformation()
            resetPlayer()
        }

        archiveUrl.collect { archiveUrl ->
            println("collected archive url $archiveUrl")
            if (archiveUrl.isNotEmpty()) {
                _segmentsLoadingJob.value = startArchiveSegmentsLoading(archiveUrl)
            }
        }
    }

    suspend fun startLivePlayback() {
        println("start playback")

        if (!isOrchestratorInitialized.value) {
            initializePlayerForPlayback()
        } else {
            resetPlaybackInformation()
            resetPlayer()
        }

        currentChannel.first {
                channel -> channel != ChannelData()
        }.let { currentChannel ->
            _segmentsLoadingJob.value = startLiveSegmentsLoading(currentChannel.channelUrl)
        }
    }

    private fun resetPlaybackInformation() {
        urlQueue.clear()
        emittedSegmentUrls.clear()
        _segmentsLoadingJob.value?.cancel()
        _isDataSourceSet.value = false
        _isPlaybackStarted.value = false
        _newSegmentsNeeded.value = true
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
        updateIsLive(false)
        mediaManager.pause()
    }

    suspend fun resetPlayer() {
        mediaManager.resetPlayer()
        initializePlayerForPlayback()
    }

    fun getLastTsSegmentFromQueue(): String {
        if (urlQueue.size == 0) {
            return ""
        } else {
            return urlQueue.last
        }
    }

    fun getUrlQueueSize(): Int {
        return urlQueue.size
    }

    fun addUrlToQueue(url: String) {
        println("url added to queue $url")
        if (getUrlQueueSize() >= LIVE_SEGMENTS_BUFFER_THREESHOLD) {
            discardOldestHalfSegments()
        }
        urlQueue.add(url)
    }

    fun pollUrl(): String {
        return urlQueue.poll() ?: ""
    }

    fun setPlayerSurface(surface: Surface) {
        println("surface set orchestrator? $surface")
        mediaManager.setPlayerSurface(surface)
    }
}