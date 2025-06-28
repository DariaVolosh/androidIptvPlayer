package com.example.iptvplayer.unitTests.media

import app.cash.turbine.turbineScope
import com.example.iptvplayer.data.media.TsExtractor
import com.example.iptvplayer.data.repositories.MediaDataSource
import com.example.iptvplayer.data.repositories.MediaPlaybackRepository
import com.example.iptvplayer.domain.archive.ArchiveManager
import com.example.iptvplayer.domain.channels.ChannelsManager
import com.example.iptvplayer.domain.media.MediaManager
import com.example.iptvplayer.domain.media.MediaPlaybackOrchestrator
import com.example.iptvplayer.domain.sharedPrefs.SharedPreferencesUseCase
import com.example.iptvplayer.retrofit.data.ChannelData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import tv.danmaku.ijk.media.player.IMediaPlayer
import tv.danmaku.ijk.media.player.IjkMediaPlayer

@ExtendWith(MockitoExtension::class)
class MediaPlaybackOrchestratorTest {
    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var sharedPreferencesUseCase: SharedPreferencesUseCase
    @Mock
    private lateinit var ijkMediaPlayer: IjkMediaPlayer
    @Mock
    private lateinit var mediaDataSource: MediaDataSource
    @Mock
    private lateinit var mediaPlaybackRepository: MediaPlaybackRepository
    @Mock
    private lateinit var tsExtractor: TsExtractor
    @Mock
    private lateinit var archiveManager: ArchiveManager

    private lateinit var mediaPlaybackOrchestrator: MediaPlaybackOrchestrator

    @Mock
    private lateinit var mediaManager: MediaManager
    @Mock
    private lateinit var channelsManager: ChannelsManager

    private lateinit var currentChannelControlledFlow: MutableStateFlow<ChannelData>

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        currentChannelControlledFlow = MutableStateFlow(ChannelData())
        whenever(channelsManager.currentChannel).thenReturn(currentChannelControlledFlow)
        whenever(archiveManager.archiveSegmentUrl).thenReturn(MutableStateFlow(""))
    }

    @Test
    fun addUrlToQueue_urlsAvailable_urlQueueSizeIncreasing() = runTest {
        turbineScope {
            mediaPlaybackOrchestrator = MediaPlaybackOrchestrator(
                channelsManager = channelsManager,
                mediaManager = mediaManager,
                archiveManager = archiveManager,
                mediaPlaybackRepository = mediaPlaybackRepository,
                tsExtractor = tsExtractor,
                sharedPreferencesUseCase = sharedPreferencesUseCase,
                orchestratorScope = backgroundScope
            )


            val mockTsSegments = List(4) { index -> "mockTsSegmentUrl${index + 1}" }

            for (i in mockTsSegments.indices) {
                val mockTsSegment = mockTsSegments[i]
                mediaPlaybackOrchestrator.addUrlToQueue(mockTsSegment)
                assertEquals(i + 1, mediaPlaybackOrchestrator.getUrlQueueSize())
            }
        }
    }

    @Test
    fun onSegmentRequest_urlQueueIsPopulated_urlsArePolledFromUrlQueue() = runTest {
        whenever(mediaManager.ijkPlayer).thenReturn(MutableStateFlow(ijkMediaPlayer))
        whenever(mediaPlaybackRepository.getMediaDataSource()).thenReturn(mediaDataSource)

        mediaPlaybackOrchestrator = MediaPlaybackOrchestrator(
            channelsManager = channelsManager,
            mediaManager = mediaManager,
            archiveManager = archiveManager,
            mediaPlaybackRepository = mediaPlaybackRepository,
            tsExtractor = tsExtractor,
            sharedPreferencesUseCase = sharedPreferencesUseCase,
            orchestratorScope = backgroundScope
        )



        val mockTsSegments = List(4) { index -> "mockTsSegmentUrl${index + 1}" }

        for (i in mockTsSegments.indices) {
            val mockTsSegment = mockTsSegments[i]
            mediaPlaybackOrchestrator.addUrlToQueue(mockTsSegment)
            assertEquals(i + 1, mediaPlaybackOrchestrator.getUrlQueueSize())
        }

        var expectedUrlQueueSize = 4

        for (mockTsSegment in mockTsSegments) {
            expectedUrlQueueSize--
            mediaPlaybackOrchestrator.onSegmentRequest(mediaDataSource)
            advanceTimeBy(1)
            verify(mediaDataSource, times(1)).setMediaUrl(mockTsSegment)
            assertEquals(expectedUrlQueueSize, mediaPlaybackOrchestrator.getUrlQueueSize())
        }
    }

    @Test
    fun initializePlayer_appStartup_allVariablesRelatedToPlayerInitAreSetCorrectly() = runTest {
        turbineScope {
            val ijkPlayerFlow: MutableStateFlow<IjkMediaPlayer?> = MutableStateFlow(null)
            val onPreparedListener = argumentCaptor<IMediaPlayer.OnPreparedListener>()
            val onInfoListener = argumentCaptor<(IMediaPlayer, Int, Int) -> Boolean>()

            whenever(mediaManager.ijkPlayer).thenReturn(ijkPlayerFlow)
            whenever(mediaPlaybackRepository.getMediaDataSource()).thenReturn(mediaDataSource)
            whenever(mediaDataSource.setOnNextSegmentRequestedCallback(any())).thenAnswer {  }

            mediaPlaybackOrchestrator = MediaPlaybackOrchestrator(
                channelsManager = channelsManager,
                mediaManager = mediaManager,
                archiveManager = archiveManager,
                mediaPlaybackRepository = mediaPlaybackRepository,
                tsExtractor = tsExtractor,
                sharedPreferencesUseCase = sharedPreferencesUseCase,
                orchestratorScope = backgroundScope
            )


            val isDataSourceSetCollector = mediaPlaybackOrchestrator.isDataSourceSet.testIn(this)
            val isPlaybackStartedCollector =
                mediaPlaybackOrchestrator.isPlaybackStarted.testIn(this)
            val newSegmentsNeededCollector =
                mediaPlaybackOrchestrator.newSegmentsNeeded.testIn(this)
            val segmentsCollectingJob = mediaPlaybackOrchestrator.segmentsLoadingJob.testIn(this)
            val isSeekingCollector = mediaPlaybackOrchestrator.isSeeking.testIn(this)
            val isPausedCollector = mediaPlaybackOrchestrator.isPaused.testIn(this)

            verify(tsExtractor, never()).extractNestedPlaylistUrls(any())
            verify(mediaDataSource, never()).setMediaUrl(any())
            verify(mediaManager, never()).setOnPreparedListener(any())

            assertTrue(mediaPlaybackOrchestrator.getUrlQueueSize() == 0)
            assertFalse(isSeekingCollector.awaitItem())
            assertFalse(isDataSourceSetCollector.awaitItem())
            assertFalse(isPlaybackStartedCollector.awaitItem())
            assertTrue(newSegmentsNeededCollector.awaitItem())

            ijkPlayerFlow.value = ijkMediaPlayer
            advanceTimeBy(1)

            verify(mediaManager, times(1)).setOnPreparedListener(onPreparedListener.capture())
            verify(mediaManager, times(1)).setOnInfoListener(onInfoListener.capture())
            verify(mediaPlaybackRepository, times(1)).getMediaDataSource()
            verify(mediaManager, times(1)).setDataSource(mediaDataSource)
            verify(mediaDataSource, times(1)).setOnNextSegmentRequestedCallback(any())

            isDataSourceSetCollector.cancelAndIgnoreRemainingEvents()
            isPlaybackStartedCollector.cancelAndIgnoreRemainingEvents()
            newSegmentsNeededCollector.cancelAndIgnoreRemainingEvents()
            isSeekingCollector.cancelAndIgnoreRemainingEvents()
            isPausedCollector.cancelAndIgnoreRemainingEvents()
            segmentsCollectingJob.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun startPlaylistParsingAvailable_lastEmittedSegmentUpdated() = runTest {
        turbineScope {
            val mockTsSegments = List(4) { index -> "mockTsSegmentUrl${index + 1}" }

            whenever(tsExtractor.extractNestedPlaylistUrls(any())).thenReturn(emptyList())
            whenever(tsExtractor.extractTsSegmentUrls(any())).thenReturn(mockTsSegments)
            whenever(mediaManager.ijkPlayer).thenReturn(MutableStateFlow(ijkMediaPlayer))
            whenever(mediaPlaybackRepository.getMediaDataSource()).thenReturn(mediaDataSource)

            mediaPlaybackOrchestrator = MediaPlaybackOrchestrator(
                channelsManager = channelsManager,
                mediaManager = mediaManager,
                archiveManager = archiveManager,
                mediaPlaybackRepository = mediaPlaybackRepository,
                tsExtractor = tsExtractor,
                sharedPreferencesUseCase = sharedPreferencesUseCase,
                orchestratorScope = backgroundScope
            )

            val segmentsCollectingJobCollector =
                mediaPlaybackOrchestrator.segmentsLoadingJob.testIn(this)
            val currentChannelDataCollector = mediaPlaybackOrchestrator.currentChannel.testIn(this)

            assertEquals(ChannelData(), currentChannelDataCollector.awaitItem())
            assertNull(segmentsCollectingJobCollector.awaitItem())
            assertEquals(0, mediaPlaybackOrchestrator.getUrlQueueSize())

            mediaPlaybackOrchestrator.startLivePlayback()

            currentChannelControlledFlow.value = ChannelData(name = "name")
            assertEquals(ChannelData(name = "name"), currentChannelDataCollector.awaitItem())
            assertEquals(4, mediaPlaybackOrchestrator.getUrlQueueSize())
            assertEquals(mockTsSegments[3], mediaPlaybackOrchestrator.getLastTsSegmentFromQueue())

            segmentsCollectingJobCollector.cancelAndIgnoreRemainingEvents()
            currentChannelDataCollector.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun startPlaylistParsingAvailable_isUrlQueuePopulated() = runTest {
        turbineScope {
            val mockTsSegments = List(4) { index -> "mockTsSegmentUrl${index + 1}" }

            whenever(mediaManager.ijkPlayer).thenReturn(MutableStateFlow(ijkMediaPlayer))
            whenever(mediaPlaybackRepository.getMediaDataSource()).thenReturn(mediaDataSource)

            mediaPlaybackOrchestrator = MediaPlaybackOrchestrator(
                channelsManager = channelsManager,
                mediaManager = mediaManager,
                archiveManager = archiveManager,
                mediaPlaybackRepository = mediaPlaybackRepository,
                tsExtractor = tsExtractor,
                sharedPreferencesUseCase = sharedPreferencesUseCase,
                orchestratorScope = backgroundScope
            )

            assertEquals(0, mediaPlaybackOrchestrator.getUrlQueueSize())

            for (i in mockTsSegments.indices) {
                val mockTsSegment = mockTsSegments[i]
                mediaPlaybackOrchestrator.addUrlToQueue(mockTsSegment)
                advanceTimeBy(1)
                assertEquals(i + 1, mediaPlaybackOrchestrator.getUrlQueueSize())
            }
        }
    }
}