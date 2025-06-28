package com.example.iptvplayer.integrationTests.domain

import com.example.iptvplayer.data.IjkPlayerFactory
import com.example.iptvplayer.data.media.TsExtractor
import com.example.iptvplayer.data.repositories.MediaPlaybackRepository
import com.example.iptvplayer.domain.archive.ArchiveManager
import com.example.iptvplayer.domain.channels.ChannelsManager
import com.example.iptvplayer.domain.media.GetTsSegmentsUseCase
import com.example.iptvplayer.domain.media.MediaManager
import com.example.iptvplayer.domain.media.MediaPlaybackOrchestrator
import com.example.iptvplayer.domain.sharedPrefs.SharedPreferencesUseCase
import com.example.iptvplayer.retrofit.data.ChannelData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

class MediaPlaybackOrchestratorAndMediaManagerIntegrationTest {
    private var testDispatcher = StandardTestDispatcher()

    @Mock private lateinit var channelsManager: ChannelsManager
    @Mock private lateinit var archiveManager: ArchiveManager
    @Mock private lateinit var getTsSegmentsUseCase: GetTsSegmentsUseCase
    @Mock private lateinit var mediaPlaybackRepository: MediaPlaybackRepository

    private lateinit var mediaPlaybackOrchestrator: MediaPlaybackOrchestrator

    @Mock private lateinit var sharedPreferencesUseCase: SharedPreferencesUseCase
    @Mock private lateinit var ijkPlayerFactory: IjkPlayerFactory
    @Mock private lateinit var tsExtractor: TsExtractor

    private lateinit var mediaManager: MediaManager

    private lateinit var controlledTsSegmentsFlow: MutableStateFlow<String>

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        MockitoAnnotations.openMocks(this)

        controlledTsSegmentsFlow = MutableStateFlow("")

        whenever(channelsManager.currentChannel).thenReturn(MutableStateFlow(ChannelData()))
        whenever(archiveManager.archiveSegmentUrl).thenReturn(MutableStateFlow(""))
        whenever(getTsSegmentsUseCase.extractTsSegments(any(), any(), any())).thenReturn(controlledTsSegmentsFlow)
    }


}
