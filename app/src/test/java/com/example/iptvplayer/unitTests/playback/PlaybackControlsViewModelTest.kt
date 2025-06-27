package com.example.iptvplayer.unitTests.playback

import com.example.iptvplayer.MyApp
import com.example.iptvplayer.domain.archive.ArchiveManager
import com.example.iptvplayer.domain.channels.ChannelsManager
import com.example.iptvplayer.domain.epg.EpgManager
import com.example.iptvplayer.domain.media.MediaPlaybackOrchestrator
import com.example.iptvplayer.domain.time.TimeOrchestrator
import com.example.iptvplayer.view.channelInfo.playbackControls.PlaybackControlsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class PlaybackControlsViewModelTest {
    private var testDispatcher = StandardTestDispatcher()

    @Mock private lateinit var application: MyApp
    @Mock private lateinit var epgManager: EpgManager
    @Mock private lateinit var mediaPlaybackOrchestrator: MediaPlaybackOrchestrator
    @Mock private lateinit var archiveManager: ArchiveManager
    @Mock private lateinit var channelsManager: ChannelsManager
    @Mock private lateinit var timeOrchestrator: TimeOrchestrator

    private lateinit var viewModel: PlaybackControlsViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        viewModel = PlaybackControlsViewModel(
            context = application.applicationContext,
            epgManager =  epgManager,
            mediaPlaybackOrchestrator = mediaPlaybackOrchestrator,
            archiveManager = archiveManager,
            channelsManager = channelsManager,
            timeOrchestrator = timeOrchestrator
        )
    }

    @After
    fun finish() {
        Dispatchers.resetMain()
    }
}