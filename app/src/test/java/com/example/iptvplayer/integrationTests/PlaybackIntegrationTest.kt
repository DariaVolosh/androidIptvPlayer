package com.example.iptvplayer.integrationTests

import android.content.Context
import com.example.iptvplayer.data.IjkPlayerFactory
import com.example.iptvplayer.data.InputStreamProvider
import com.example.iptvplayer.data.repositories.AuthRepository
import com.example.iptvplayer.data.repositories.ChannelsRepository
import com.example.iptvplayer.data.repositories.FileUtilsRepository
import com.example.iptvplayer.data.repositories.M3U8PlaylistRepository
import com.example.iptvplayer.data.repositories.MediaDataSource
import com.example.iptvplayer.data.repositories.MediaPlaybackRepository
import com.example.iptvplayer.domain.archive.ArchiveManager
import com.example.iptvplayer.domain.auth.AuthManager
import com.example.iptvplayer.domain.auth.AuthOrchestrator
import com.example.iptvplayer.domain.channels.ChannelsManager
import com.example.iptvplayer.domain.errors.ErrorManager
import com.example.iptvplayer.domain.media.GetMediaDataSourceUseCase
import com.example.iptvplayer.domain.media.GetTsSegmentsUseCase
import com.example.iptvplayer.domain.media.MediaManager
import com.example.iptvplayer.domain.media.MediaPlaybackOrchestrator
import com.example.iptvplayer.domain.sharedPrefs.SharedPreferencesUseCase
import com.example.iptvplayer.domain.time.DateManager
import com.example.iptvplayer.domain.time.TimeOrchestrator
import com.example.iptvplayer.retrofit.services.AuthService
import com.example.iptvplayer.retrofit.services.ChannelsAndEpgService
import com.example.iptvplayer.view.media.MediaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class PlaybackIntegrationTest {
    private var testDispatcher = StandardTestDispatcher()

    // mock of context needed by repositories
    @Mock private lateinit var context: Context

    // media view model mocks, that we are not interested in this certain test
    @Mock private lateinit var mockSharedPreferencesUseCase: SharedPreferencesUseCase
    @Mock private lateinit var mockTimeOrchestrator: TimeOrchestrator
    @Mock private lateinit var mockDateManager: DateManager
    // media view model real instance, since we want to test it
    private lateinit var mediaViewModel: MediaViewModel

    // playback orchestrator mocks, that we are not interested in this certain test
    @Mock private lateinit var mockErrorManager: ErrorManager
    @Mock private lateinit var mockArchiveManager: ArchiveManager
    // playback orchestrator and get ts segments use case real instances
    private lateinit var getTsSegmentsUseCase: GetTsSegmentsUseCase
    @Mock private lateinit var mediaPlaybackOrchestrator: MediaPlaybackOrchestrator

    // ijk player mock
    @Mock private lateinit var ijkPlayerFactory: IjkPlayerFactory
    // real instances of media manager and its dependencies, since they are all needed for our test
    private lateinit var getMediaDataSourceUseCase: GetMediaDataSourceUseCase
    private lateinit var mediaManager: MediaManager

    // real instance of playlist repository
    private lateinit var m3U8PlaylistRepository: M3U8PlaylistRepository

    // real instance of file utils repository
    private lateinit var fileUtilsRepository: FileUtilsRepository

    // real instance of media repository
    private lateinit var mediaPlaybackRepository: MediaPlaybackRepository

    // mock of input stream provider
    @Mock private lateinit var inputStreamProvider: InputStreamProvider
    // real instance of media data source
    private lateinit var mediaDataSource: MediaDataSource

    // real instance of auth orchestrator
    private lateinit var authOrchestrator: AuthOrchestrator

    @Mock private lateinit var authService: AuthService
    @Mock private lateinit var channelsAndEpgService: ChannelsAndEpgService
    // real instance of auth repository
    private lateinit var authRepository: AuthRepository
    // real instance of auth manager
    private lateinit var authManager: AuthManager

    private lateinit var channelsRepository: ChannelsRepository
    private lateinit var channelsManager: ChannelsManager

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        // instantiation of all real instances before each test
        // repositories and media data source
        authRepository = AuthRepository(
            context = context,
            authService = authService
        )
        mediaDataSource = MediaDataSource(
            inputStreamProvider = inputStreamProvider
        )
        mediaPlaybackRepository = MediaPlaybackRepository(

        )
        m3U8PlaylistRepository = M3U8PlaylistRepository()
        fileUtilsRepository = FileUtilsRepository(
            context = context
        )

        channelsRepository = ChannelsRepository(
            context = context,
            channelsAndEpgService = channelsAndEpgService
        )

        // use cases
        getMediaDataSourceUseCase = GetMediaDataSourceUseCase(
            mediaPlaybackRepository = mediaPlaybackRepository
        )
        getTsSegmentsUseCase = GetTsSegmentsUseCase(
            playlistRepository = m3U8PlaylistRepository,
            fileUtilsRepository = fileUtilsRepository
        )

        // managers
        authManager = AuthManager(
            authRepository = authRepository
        )

        channelsManager = ChannelsManager(
            channelsRepository = channelsRepository
        )

        whenever(mockSharedPreferencesUseCase.getLongValue(any())).thenReturn(0)
        whenever(mockSharedPreferencesUseCase.getBooleanValue(any())).thenReturn(true)
        whenever(mockSharedPreferencesUseCase.saveBooleanValue(any(), any())).thenAnswer {  }
        whenever(mockArchiveManager.archiveSegmentUrl).thenReturn(MutableStateFlow(""))
    }

    /*@Test
    fun successfulPlaybackStartup_allComponentsInteractCorrectly_playbackStarts() = runTest {
        turbineScope {
            // managers
            mediaManager = MediaManager(
                sharedPreferencesUseCase = mockSharedPreferencesUseCase,
                getMediaDataSourceUseCase = getMediaDataSourceUseCase,
                handleNextSegmentRequestedUseCase = handleNextSegmentRequestedUseCase,
                setMediaUrlUseCase = setMediaUrlUseCase,
                ijkPlayerFactory = ijkPlayerFactory,
                managerScope = backgroundScope
            )

            // orchestrators
            authOrchestrator = AuthOrchestrator(
                errorManager =  mockErrorManager,
                authManager = authManager,
                orchestratorScope = backgroundScope
            )

            channelsOrchestrator = ChannelsOrchestrator(
                channelsManager = channelsManager,
                sharedPreferencesUseCase = mockSharedPreferencesUseCase,
                errorManager = mockErrorManager,
                orchestratorScope = backgroundScope
            )

            playbackOrchestrator = PlaybackOrchestrator(
                mediaManager = mediaManager,
                channelsManager = channelsManager,
                errorManager = mockErrorManager,
                archiveManager = mockArchiveManager,
                getTsSegmentsUseCase = getTsSegmentsUseCase,
                orchestratorScope = backgroundScope
            )

            // view models
            mediaViewModel = MediaViewModel(
                sharedPreferencesUseCase = mockSharedPreferencesUseCase,
                playbackOrchestrator = playbackOrchestrator,
                mediaManager = mediaManager,
                timeOrchestrator = mockTimeOrchestrator,
                viewModelScope = backgroundScope
            )

            initViewModel = InitViewModel(
                authOrchestrator = authOrchestrator,
                channelsOrchestrator = channelsOrchestrator,
                viewModelScope = backgroundScope
            )

            val mockToken = "mockToken"
            val mockTokenResponse = ChannelsAndEpgAuthResponse(
                data = AuthInfo(
                    token = mockToken
                )
            )

            whenever(mockTimeOrchestrator.initialize(any())).thenAnswer {  }
            whenever(authService.getAuthToken(any())).thenReturn(mockTokenResponse)

            // checking if auth was performed successfully
            val appStateCollector = initViewModel.appState.testIn(this)
            val authStateCollector = authOrchestrator.authState.testIn(this)

            assertEquals(AppState.INITIALIZING, appStateCollector.awaitItem())
            assertEquals(AuthState.AUTHENTICATING, authStateCollector.awaitItem())

            // app state should not change
            appStateCollector.expectNoEvents()
            // auth state should change to authenticated
            assertEquals(AuthState.AUTHENTICATED, authStateCollector.awaitItem())

            appStateCollector.cancelAndIgnoreRemainingEvents()
            authStateCollector.cancelAndIgnoreRemainingEvents()
        }
    } */
}