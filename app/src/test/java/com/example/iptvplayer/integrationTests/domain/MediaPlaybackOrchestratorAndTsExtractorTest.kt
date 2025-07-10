package com.example.iptvplayer.integrationTests.domain

import android.content.Context
import com.example.iptvplayer.data.media.TsExtractor
import com.example.iptvplayer.data.repositories.FileUtilsRepository
import com.example.iptvplayer.data.repositories.MediaPlaybackRepository
import com.example.iptvplayer.domain.archive.ArchiveManager
import com.example.iptvplayer.domain.channels.ChannelsManager
import com.example.iptvplayer.domain.errors.ErrorManager
import com.example.iptvplayer.domain.media.MediaManager
import com.example.iptvplayer.domain.media.MediaPlaybackOrchestrator
import com.example.iptvplayer.domain.sharedPrefs.SharedPreferencesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class MediaPlaybackOrchestratorAndTsExtractorTest {
    private val testDispatcher =  StandardTestDispatcher()

    @Mock private lateinit var context: Context
    @Mock private lateinit var fileUtilsRepository: FileUtilsRepository
    @Mock private lateinit var channelsManager: ChannelsManager
    @Mock private lateinit var mediaManager: MediaManager
    @Mock private lateinit var archiveManager: ArchiveManager
    @Mock private lateinit var mediaPlaybackRepository: MediaPlaybackRepository
    @Mock private lateinit var sharedPreferencesUseCase: SharedPreferencesUseCase
    @Mock private lateinit var errorManager: ErrorManager

    private lateinit var tsExtractor: TsExtractor
    private lateinit var mediaPlaybackOrchestrator: MediaPlaybackOrchestrator

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        tsExtractor = TsExtractor(
            fileUtilsRepository = fileUtilsRepository,
            errorManager = errorManager
        )
    }

    @Test
    fun extractTsSegments_nestedUrlsAndTsSegmentsAvailable_urlQueueUpdatedAndReadFileCalled() = runTest {
        val rootUrl = "fakeUrl.m3u8"

        val nestedUrls = listOf(
            "nestedUrl1.m3u8",
            "nestedUrl2.m3u8"
        )

        val segments = List(6) { index -> "segment${index+1}.ts"}

        whenever(fileUtilsRepository.readFile(eq(rootUrl), any<(String, String) -> Unit>())).thenReturn(nestedUrls)
        whenever(fileUtilsRepository.readFile(eq(nestedUrls[0]), any<(String, String) -> Unit>())).thenReturn(
            segments.subList(0, 3)
        )
        whenever(fileUtilsRepository.readFile(eq(nestedUrls[1]), any<(String, String) -> Unit>())).thenReturn(
            segments.subList(3, 6)
        )

        mediaPlaybackOrchestrator = MediaPlaybackOrchestrator(
            channelsManager = channelsManager,
            mediaManager = mediaManager,
            archiveManager = archiveManager,
            errorManager = errorManager,
            mediaPlaybackRepository = mediaPlaybackRepository,
            tsExtractor = tsExtractor,
            sharedPreferencesUseCase = sharedPreferencesUseCase,
            context = context,
            orchestratorScope = backgroundScope
        )

        mediaPlaybackOrchestrator.extractTsSegments(rootUrl)

        verify(fileUtilsRepository, times(1)).readFile(eq(rootUrl), any())
        verify(fileUtilsRepository, times(2)).readFile(eq(nestedUrls[0]), any())
        verify(fileUtilsRepository, times(2)).readFile(eq(nestedUrls[1]), any())

        assertEquals(6, mediaPlaybackOrchestrator.getUrlQueueSize())
        for (segment in segments) {
            assertEquals(segment, mediaPlaybackOrchestrator.pollUrl())
        }
    }
}