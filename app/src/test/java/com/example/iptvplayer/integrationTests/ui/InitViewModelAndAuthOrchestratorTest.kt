package com.example.iptvplayer.integrationTests.ui

import app.cash.turbine.turbineScope
import com.example.iptvplayer.domain.auth.AuthManager
import com.example.iptvplayer.domain.auth.AuthOrchestrator
import com.example.iptvplayer.domain.auth.AuthState
import com.example.iptvplayer.domain.channels.ChannelsOrchestrator
import com.example.iptvplayer.domain.channels.ChannelsState
import com.example.iptvplayer.domain.errors.ErrorManager
import com.example.iptvplayer.view.init.AppState
import com.example.iptvplayer.view.init.InitViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
class InitViewModelAndAuthOrchestratorTest {
    private val testDispatcher = StandardTestDispatcher()

    @Mock private lateinit var channelsOrchestrator: ChannelsOrchestrator
    @Mock private lateinit var errorManager: ErrorManager
    @Mock private lateinit var authManager: AuthManager

    private lateinit var initViewModel: InitViewModel
    private lateinit var authOrchestrator: AuthOrchestrator


    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        whenever(channelsOrchestrator.channelsState).thenReturn(MutableStateFlow(ChannelsState.FETCHING))
    }

    @Test
    fun init_authStateChangedToAuthenticated_fetchChannelsCalledAndAuthStateUpdatedToAuthenticated() = runTest {
        turbineScope {
            whenever(authManager.getAuthToken(any(), any())).thenReturn("mockToken")

            authOrchestrator = AuthOrchestrator(
                errorManager = errorManager,
                authManager = authManager,
                orchestratorScope = backgroundScope
            )

            initViewModel = InitViewModel(
                authOrchestrator = authOrchestrator,
                channelsOrchestrator = channelsOrchestrator,
                viewModelScope = backgroundScope
            )

            val appStateCollector = initViewModel.appState.testIn(this)
            val authStateCollector = authOrchestrator.authState.testIn(this)

            assertEquals(AppState.INITIALIZING, appStateCollector.awaitItem())
            assertEquals(AuthState.AUTHENTICATING, authStateCollector.awaitItem())
            assertEquals(AuthState.AUTHENTICATED, authStateCollector.awaitItem())

            advanceTimeBy(1)

            appStateCollector.expectNoEvents()
            verify(authManager, times(1)).getAuthToken(any(), any())
            verify(channelsOrchestrator, times(1)).fetchChannelsData()

            appStateCollector.cancelAndIgnoreRemainingEvents()
            authStateCollector.cancelAndIgnoreRemainingEvents()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun init_authStateChangedToError_fetchChannelsIsNotCalledAndAuthStateWithAppStateUpdatedToError() = runTest {
        turbineScope {
            whenever(authManager.getAuthToken(any(), any())).thenReturn("")

            authOrchestrator = AuthOrchestrator(
                errorManager = errorManager,
                authManager = authManager,
                orchestratorScope = backgroundScope
            )

            initViewModel = InitViewModel(
                authOrchestrator = authOrchestrator,
                channelsOrchestrator = channelsOrchestrator,
                viewModelScope = backgroundScope
            )

            val appStateCollector = initViewModel.appState.testIn(this)
            val authStateCollector = authOrchestrator.authState.testIn(this)
            assertEquals(AppState.INITIALIZING, appStateCollector.awaitItem())
            assertEquals(AuthState.AUTHENTICATING, authStateCollector.awaitItem())

            assertEquals(AppState.ERROR, appStateCollector.awaitItem())
            assertEquals(AuthState.ERROR, authStateCollector.awaitItem())

            advanceTimeBy(1)

            verify(authManager, times(1)).getAuthToken(any(), any())
            verify(channelsOrchestrator, never()).fetchChannelsData()

            appStateCollector.cancelAndIgnoreRemainingEvents()
            authStateCollector.cancelAndIgnoreRemainingEvents()
        }
    }
}