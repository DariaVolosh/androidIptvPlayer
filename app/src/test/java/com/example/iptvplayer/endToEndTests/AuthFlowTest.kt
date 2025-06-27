package com.example.iptvplayer.endToEndTests

import android.content.Context
import app.cash.turbine.turbineScope
import com.example.iptvplayer.data.repositories.AuthRepository
import com.example.iptvplayer.data.repositories.ChannelsRepository
import com.example.iptvplayer.domain.auth.AuthManager
import com.example.iptvplayer.domain.auth.AuthOrchestrator
import com.example.iptvplayer.domain.channels.ChannelsManager
import com.example.iptvplayer.domain.channels.ChannelsOrchestrator
import com.example.iptvplayer.domain.errors.ErrorManager
import com.example.iptvplayer.domain.sharedPrefs.SharedPreferencesUseCase
import com.example.iptvplayer.retrofit.data.AuthInfo
import com.example.iptvplayer.retrofit.data.BackendInfoResponse
import com.example.iptvplayer.retrofit.data.ChannelData
import com.example.iptvplayer.retrofit.data.ChannelsAndEpgAuthResponse
import com.example.iptvplayer.retrofit.data.ChannelsBackendInfoResponse
import com.example.iptvplayer.retrofit.data.StreamUrlTemplate
import com.example.iptvplayer.retrofit.data.StreamUrlTemplates
import com.example.iptvplayer.retrofit.data.StreamUrlTemplatesResponse
import com.example.iptvplayer.retrofit.services.AuthService
import com.example.iptvplayer.retrofit.services.ChannelsAndEpgService
import com.example.iptvplayer.view.init.AppState
import com.example.iptvplayer.view.init.InitViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class AuthFlowTest {
    private val testDispatcher = StandardTestDispatcher()

    @Mock private lateinit var context: Context
    @Mock private lateinit var authService: AuthService
    @Mock private lateinit var channelsAndEpgService: ChannelsAndEpgService

    private lateinit var authRepository: AuthRepository
    private lateinit var channelsRepository: ChannelsRepository

    @Mock private lateinit var sharedPreferencesUseCase: SharedPreferencesUseCase
    @Mock private lateinit var errorManager: ErrorManager
    private lateinit var channelsManager: ChannelsManager
    private lateinit var authManager: AuthManager

    private lateinit var authOrchestrator: AuthOrchestrator
    private lateinit var channelsOrchestrator: ChannelsOrchestrator

    private lateinit var initViewModel: InitViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        authRepository = AuthRepository(
            context = context,
            authService = authService
        )

        channelsRepository = ChannelsRepository(
            context = context,
            channelsAndEpgService = channelsAndEpgService
        )

        authManager = AuthManager(
            authRepository = authRepository
        )

        channelsManager = ChannelsManager(
            channelsRepository =  channelsRepository
        )
    }

    @Test
    fun fullAuthFlow_successfulAuthenticationAndChannelsFetch_appStateChangesToInitialized() = runTest {
        val mockToken = "mockToken"
        val mockAuthResponse = ChannelsAndEpgAuthResponse(
            data = AuthInfo(
                token = mockToken
            )
        )

        whenever(authService.getAuthToken(any())).thenReturn(mockAuthResponse)

        val mockChannelsData = List(5) {index -> BackendInfoResponse(
            channel = listOf(
                ChannelData(
                    name = "Channel ${index + 1}"
                )
            )
        ) }

        val mockTemplatesResponse = StreamUrlTemplatesResponse(
            data = StreamUrlTemplates(
                templates = List(2) {index -> StreamUrlTemplate("Template ${index + 1}") }
            )
        )
        val mockChannelsResponse = ChannelsBackendInfoResponse(
            data = mockChannelsData
        )

        whenever(channelsAndEpgService.getStreamsUrlTemplates(any())).thenReturn(mockTemplatesResponse)
        whenever(channelsAndEpgService.getChannelsInfo()).thenReturn(mockChannelsResponse)

        turbineScope {
            authOrchestrator = AuthOrchestrator(
                errorManager = errorManager,
                authManager = authManager,
                orchestratorScope = backgroundScope
            )

            channelsOrchestrator = ChannelsOrchestrator(
                channelsManager = channelsManager,
                sharedPreferencesUseCase = sharedPreferencesUseCase,
                errorManager =  errorManager,
                orchestratorScope = backgroundScope
            )

            initViewModel = InitViewModel(
                authOrchestrator = authOrchestrator,
                channelsOrchestrator =  channelsOrchestrator,
                viewModelScope = backgroundScope
            )

            val appStateCollector = initViewModel.appState.testIn(this)
            assertEquals(AppState.INITIALIZING, appStateCollector.awaitItem())

            advanceTimeBy(1)
            verify(authService, times(1)).getAuthToken(any())
            verify(channelsAndEpgService, times(1)).getStreamsUrlTemplates(any())
            verify(channelsAndEpgService, times(1)).getChannelsInfo()
            assertEquals(AppState.INITIALIZED, appStateCollector.awaitItem())

            appStateCollector.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun fullAuthFlow_authenticationFails_appStateChangesToError() = runTest {
        turbineScope {
            val mockToken = ""
            val mockAuthResponse = ChannelsAndEpgAuthResponse(
                data = AuthInfo(
                    token = mockToken
                )
            )

            whenever(authService.getAuthToken(any())).thenReturn(mockAuthResponse)

            turbineScope {
                authOrchestrator = AuthOrchestrator(
                    errorManager = errorManager,
                    authManager = authManager,
                    orchestratorScope = backgroundScope
                )

                channelsOrchestrator = ChannelsOrchestrator(
                    channelsManager = channelsManager,
                    sharedPreferencesUseCase = sharedPreferencesUseCase,
                    errorManager = errorManager,
                    orchestratorScope = backgroundScope
                )

                initViewModel = InitViewModel(
                    authOrchestrator = authOrchestrator,
                    channelsOrchestrator = channelsOrchestrator,
                    viewModelScope = backgroundScope
                )

                val appStateCollector = initViewModel.appState.testIn(this)
                assertEquals(AppState.INITIALIZING, appStateCollector.awaitItem())

                advanceTimeBy(1)
                verify(authService, times(1)).getAuthToken(any())
                verify(channelsAndEpgService, never()).getStreamsUrlTemplates(any())
                verify(channelsAndEpgService, never()).getChannelsInfo()
                assertEquals(AppState.ERROR, appStateCollector.awaitItem())

                appStateCollector.cancelAndIgnoreRemainingEvents()
            }
        }
    }
}