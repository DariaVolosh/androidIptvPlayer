package com.example.iptvplayer.unitTests.playbackControls

import com.example.iptvplayer.MyApp
import com.example.iptvplayer.view.channelInfo.playbackControls.PlaybackControlsViewModel
import com.example.iptvplayer.view.channels.ChannelsManager
import com.example.iptvplayer.view.channelsAndEpgRow.ArchiveManager
import com.example.iptvplayer.view.epg.EpgManager
import com.example.iptvplayer.view.player.MediaManager
import com.example.iptvplayer.view.time.TimeOrchestrator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class PlaybackControlsViewModelTest {
    private var testDispatcher = StandardTestDispatcher()


    @Mock private lateinit var application: MyApp
    @Mock private lateinit var epgManager: EpgManager
    @Mock private lateinit var mediaManager: MediaManager
    @Mock private lateinit var archiveManager: ArchiveManager
    @Mock private lateinit var channelsManager: ChannelsManager
    @Mock private lateinit var timeOrchestrator: TimeOrchestrator

    private lateinit var viewModel: PlaybackControlsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        MockitoAnnotations.openMocks(this)

        viewModel = PlaybackControlsViewModel(
            context = application.applicationContext,
            epgManager =  epgManager,
            mediaManager = mediaManager,
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