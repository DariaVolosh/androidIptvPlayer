package com.example.iptvplayer.endToEndTests

import android.content.Context
import app.cash.turbine.turbineScope
import com.example.iptvplayer.data.IjkPlayerFactory
import com.example.iptvplayer.data.media.TsExtractor
import com.example.iptvplayer.data.repositories.FileUtilsRepository
import com.example.iptvplayer.data.repositories.MediaDataSource
import com.example.iptvplayer.data.repositories.MediaPlaybackRepository
import com.example.iptvplayer.domain.archive.ArchiveManager
import com.example.iptvplayer.domain.channels.ChannelsManager
import com.example.iptvplayer.domain.channels.ChannelsOrchestrator
import com.example.iptvplayer.domain.channels.ChannelsState
import com.example.iptvplayer.domain.errors.ErrorManager
import com.example.iptvplayer.domain.media.MediaManager
import com.example.iptvplayer.domain.media.MediaPlaybackOrchestrator
import com.example.iptvplayer.domain.sharedPrefs.SharedPreferencesUseCase
import com.example.iptvplayer.domain.time.IS_LIVE_KEY
import com.example.iptvplayer.retrofit.data.ChannelData
import com.example.iptvplayer.view.archive.CurrentDvrInfoState
import com.example.iptvplayer.view.media.MediaPlaybackViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import tv.danmaku.ijk.media.player.IMediaPlayer
import tv.danmaku.ijk.media.player.IMediaPlayer.OnInfoListener
import tv.danmaku.ijk.media.player.IjkMediaPlayer

@ExtendWith(MockitoExtension::class)
class ChannelPlaybackFlowTest {
    private val testDispatcher = StandardTestDispatcher()

    @Mock private lateinit var channelsOrchestrator: ChannelsOrchestrator
    @Mock private lateinit var archiveManager: ArchiveManager
    private lateinit var mediaPlaybackViewModel: MediaPlaybackViewModel

    @Mock private lateinit var context: Context
    @Mock private lateinit var channelsManager: ChannelsManager
    @Mock private lateinit var sharedPreferencesUseCase: SharedPreferencesUseCase
    private lateinit var mediaPlaybackOrchestrator: MediaPlaybackOrchestrator

    @Mock private lateinit var ijkPlayerFactory: IjkPlayerFactory
    private lateinit var mediaManager: MediaManager

    private lateinit var mediaPlaybackRepository: MediaPlaybackRepository
    private lateinit var tsExtractor: TsExtractor
    private lateinit var errorManager: ErrorManager
    private lateinit var fileUtilsRepository: FileUtilsRepository

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mediaPlaybackRepository = MediaPlaybackRepository()
        fileUtilsRepository = FileUtilsRepository(
            context = context
        )

        errorManager = ErrorManager()

        tsExtractor = TsExtractor(
            fileUtilsRepository = fileUtilsRepository,
            errorManager = errorManager
        )
    }

    @AfterEach
    fun finish() {
        Dispatchers.resetMain()
    }

    //@Test
    fun startPlaybackOnAppStartup_livePlayback_successfullyStartsPlayback() = runTest {
        turbineScope {
            val ijkPlayer = mock<IjkMediaPlayer>()
            val currentChannel = ChannelData(
                name = "name",
                logo = "logo",
                channelScreenName = "channelScreenName",
                epgChannelId = "53",
                channelUrl = "nameChannel.m3u8"
            )
            whenever(ijkPlayerFactory.create()).thenReturn(ijkPlayer)
            whenever(sharedPreferencesUseCase.getBooleanValue(IS_LIVE_KEY)).thenReturn(true)

            whenever(channelsManager.currentChannel).thenReturn(MutableStateFlow(currentChannel))
            whenever(archiveManager.archiveSegmentUrl).thenReturn(MutableStateFlow(""))
            whenever(archiveManager.currentChannelDvrInfoState).thenReturn(MutableStateFlow(CurrentDvrInfoState.LOADING))

            whenever(channelsOrchestrator.channelsState).thenReturn(MutableStateFlow(ChannelsState.FETCHED))

            mediaManager = MediaManager(
                ijkPlayerFactory = ijkPlayerFactory,
                managerScope = backgroundScope
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

            mediaPlaybackViewModel = MediaPlaybackViewModel(
                channelsOrchestrator = channelsOrchestrator,
                mediaPlaybackOrchestrator = mediaPlaybackOrchestrator,
                archiveManager = archiveManager,
                viewModelScope = backgroundScope
            )

            val ijkPlayerCollector = mediaManager.ijkPlayer.testIn(this)
            assertNull(ijkPlayerCollector.awaitItem())

            mediaPlaybackViewModel.startPlayback()

            assertNotNull(ijkPlayerCollector.awaitItem())

            verify(ijkPlayer, times(1)).setOnPreparedListener(any<IMediaPlayer.OnPreparedListener>())
            verify(ijkPlayer, times(1)).setOnInfoListener(any<OnInfoListener>())
            verify(ijkPlayer, times(1)).setDataSource(any<MediaDataSource>())
            verify(ijkPlayer, times(1)).prepareAsync()

            ijkPlayerCollector.cancelAndIgnoreRemainingEvents()
        }
    }
}