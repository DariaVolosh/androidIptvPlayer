package com.example.iptvplayer.unitTests.init

import app.cash.turbine.turbineScope
import com.example.iptvplayer.domain.auth.AuthOrchestrator
import com.example.iptvplayer.domain.auth.AuthState
import com.example.iptvplayer.domain.channels.ChannelsOrchestrator
import com.example.iptvplayer.domain.channels.ChannelsState
import com.example.iptvplayer.view.init.AppState
import com.example.iptvplayer.view.init.InitViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class InitViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @Mock private lateinit var authOrchestrator: AuthOrchestrator
    @Mock private lateinit var channelsOrchestrator: ChannelsOrchestrator

    private val authStateControlledFlow = MutableStateFlow(AuthState.AUTHENTICATING)
    private val channelsStateControlledFlow = MutableStateFlow(ChannelsState.FETCHING)

    private lateinit var initViewModel: InitViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        whenever(authOrchestrator.authState).thenReturn(authStateControlledFlow)
        whenever(channelsOrchestrator.channelsState).thenReturn(channelsStateControlledFlow)
    }

    @Test
    fun init_authenticationAndChannelsFetchingSuccessful_appStateUpdatedToInitialized() = runTest {
        turbineScope {
            initViewModel = InitViewModel(
                authOrchestrator = authOrchestrator,
                channelsOrchestrator = channelsOrchestrator,
                viewModelScope =  backgroundScope
            )

            whenever(authOrchestrator.getBackendToken()).doAnswer {
                authStateControlledFlow.value = AuthState.AUTHENTICATED
            }

            whenever(channelsOrchestrator.fetchChannelsData()).doAnswer {
                channelsStateControlledFlow.value = ChannelsState.FETCHED
            }

            val appStateCollector = initViewModel.appState.testIn(this)
            assertEquals(AppState.INITIALIZING, appStateCollector.awaitItem())
            assertEquals(AppState.INITIALIZED, appStateCollector.awaitItem())

            verify(authOrchestrator, times(1)).getBackendToken()
            verify(channelsOrchestrator, times(1)).fetchChannelsData()

            appStateCollector.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun init_authenticationFailed_appStateUpdatedToError() = runTest {
        turbineScope {
            initViewModel = InitViewModel(
                authOrchestrator = authOrchestrator,
                channelsOrchestrator = channelsOrchestrator,
                viewModelScope =  backgroundScope
            )

            whenever(authOrchestrator.getBackendToken()).doAnswer {
                authStateControlledFlow.value = AuthState.ERROR
            }

            val appStateCollector = initViewModel.appState.testIn(this)
            assertEquals(AppState.INITIALIZING, appStateCollector.awaitItem())
            assertEquals(AppState.ERROR, appStateCollector.awaitItem())

            verify(authOrchestrator, times(1)).getBackendToken()
            verify(channelsOrchestrator, never()).fetchChannelsData()

            appStateCollector.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun init_channelsFetchingFailed_appStateUpdatedToError() = runTest {
        turbineScope {
            initViewModel = InitViewModel(
                authOrchestrator = authOrchestrator,
                channelsOrchestrator = channelsOrchestrator,
                viewModelScope =  backgroundScope
            )

            whenever(authOrchestrator.getBackendToken()).doAnswer {
                authStateControlledFlow.value = AuthState.AUTHENTICATED
            }

            whenever(channelsOrchestrator.fetchChannelsData()).doAnswer {
                channelsStateControlledFlow.value = ChannelsState.ERROR
            }

            val appStateCollector = initViewModel.appState.testIn(this)
            assertEquals(AppState.INITIALIZING, appStateCollector.awaitItem())
            assertEquals(AppState.ERROR, appStateCollector.awaitItem())

            verify(authOrchestrator, times(1)).getBackendToken()
            verify(channelsOrchestrator, times(1)).fetchChannelsData()

            appStateCollector.cancelAndIgnoreRemainingEvents()
        }
    }
}