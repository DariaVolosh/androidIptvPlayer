package com.example.iptvplayer.integrationTests.ui

import app.cash.turbine.turbineScope
import com.example.iptvplayer.domain.auth.AuthOrchestrator
import com.example.iptvplayer.domain.auth.AuthState
import com.example.iptvplayer.domain.channels.ChannelsManager
import com.example.iptvplayer.domain.channels.ChannelsOrchestrator
import com.example.iptvplayer.domain.channels.ChannelsState
import com.example.iptvplayer.domain.errors.ErrorManager
import com.example.iptvplayer.domain.sharedPrefs.SharedPreferencesUseCase
import com.example.iptvplayer.retrofit.data.ChannelData
import com.example.iptvplayer.view.init.AppState
import com.example.iptvplayer.view.init.InitViewModel
import kotlinx.coroutines.Dispatchers
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
class InitViewModelAndChannelsOrchestratorTest {
    private val testDispatcher = StandardTestDispatcher()

    @Mock private lateinit var authOrchestrator: AuthOrchestrator
    @Mock private lateinit var channelsManager: ChannelsManager
    @Mock private lateinit var errorManager: ErrorManager
    @Mock private lateinit var sharedPreferencesUseCase: SharedPreferencesUseCase

    private lateinit var initViewModel: InitViewModel
    private lateinit var channelsOrchestrator: ChannelsOrchestrator

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        whenever(channelsManager.currentChannelIndex).thenReturn(MutableStateFlow(-1))
        whenever(channelsManager.channelsData).thenReturn(MutableStateFlow(emptyList()))
    }

    @Test
    fun init_fetchChannelsCalledAndChannelsDataAvailable_appStateChangesToInitializedAndChannelsStateChangesToFetched() = runTest {
        turbineScope {
            val mockChannelsData = List(5) {index -> ChannelData(name = "Channel ${index + 1}") }

            whenever(authOrchestrator.authState).thenReturn(MutableStateFlow(AuthState.AUTHENTICATED))
            whenever(channelsManager.getChannelsData(any<Function2<String, String, Unit>>())).thenReturn(mockChannelsData)

            channelsOrchestrator = ChannelsOrchestrator(
                channelsManager = channelsManager,
                sharedPreferencesUseCase = sharedPreferencesUseCase,
                errorManager =  errorManager,
                orchestratorScope = backgroundScope
            )

            initViewModel = InitViewModel(
                authOrchestrator = authOrchestrator,
                channelsOrchestrator = channelsOrchestrator,
                viewModelScope =  backgroundScope
            )

            val appStateCollector = initViewModel.appState.testIn(this)
            val channelsStateCollector = channelsOrchestrator.channelsState.testIn(this)

            assertEquals(ChannelsState.INITIALIZING, channelsStateCollector.awaitItem())
            assertEquals(AppState.INITIALIZING, appStateCollector.awaitItem())

            assertEquals(ChannelsState.FETCHING, channelsStateCollector.awaitItem())
            assertEquals(ChannelsState.FETCHED, channelsStateCollector.awaitItem())
            assertEquals(AppState.INITIALIZED, appStateCollector.awaitItem())
            advanceTimeBy(1)

            verify(channelsManager, times(1)).getChannelsData(any<Function2<String, String, Unit>>())

            appStateCollector.cancelAndIgnoreRemainingEvents()
            channelsStateCollector.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun init_fetchChannelsCalledAndChannelsDataIsNotAvailable_appStateAndChannelsStateChangeToError() = runTest {
        turbineScope {
            whenever(authOrchestrator.authState).thenReturn(MutableStateFlow(AuthState.AUTHENTICATED))
            whenever(channelsManager.getChannelsData(any<Function2<String, String, Unit>>())).thenReturn(
                emptyList()
            )

            channelsOrchestrator = ChannelsOrchestrator(
                channelsManager = channelsManager,
                sharedPreferencesUseCase = sharedPreferencesUseCase,
                errorManager =  errorManager,
                orchestratorScope = backgroundScope
            )

            initViewModel = InitViewModel(
                authOrchestrator = authOrchestrator,
                channelsOrchestrator = channelsOrchestrator,
                viewModelScope =  backgroundScope
            )

            val appStateCollector = initViewModel.appState.testIn(this)
            val channelsStateCollector = channelsOrchestrator.channelsState.testIn(this)

            assertEquals(ChannelsState.INITIALIZING, channelsStateCollector.awaitItem())
            assertEquals(AppState.INITIALIZING, appStateCollector.awaitItem())

            assertEquals(ChannelsState.FETCHING, channelsStateCollector.awaitItem())
            assertEquals(ChannelsState.ERROR, channelsStateCollector.awaitItem())
            assertEquals(AppState.ERROR, appStateCollector.awaitItem())

            advanceTimeBy(1)

            verify(channelsManager, times(1)).getChannelsData(any<Function2<String, String, Unit>>())

            appStateCollector.cancelAndIgnoreRemainingEvents()
            channelsStateCollector.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun init_authWasNotSuccessfulAndAppStateUpdatedToError_fetchChannelIsNotCalledAndChannelsStateStaysFetching() = runTest {
        turbineScope {
            whenever(authOrchestrator.authState).thenReturn(MutableStateFlow(AuthState.ERROR))

            channelsOrchestrator = ChannelsOrchestrator(
                channelsManager = channelsManager,
                sharedPreferencesUseCase = sharedPreferencesUseCase,
                errorManager =  errorManager,
                orchestratorScope = backgroundScope
            )

            initViewModel = InitViewModel(
                authOrchestrator = authOrchestrator,
                channelsOrchestrator = channelsOrchestrator,
                viewModelScope =  backgroundScope
            )

            val appStateCollector = initViewModel.appState.testIn(this)
            val channelsStateCollector = channelsOrchestrator.channelsState.testIn(this)

            assertEquals(ChannelsState.INITIALIZING, channelsStateCollector.awaitItem())
            assertEquals(AppState.INITIALIZING, appStateCollector.awaitItem())

            channelsStateCollector.expectNoEvents()
            assertEquals(AppState.ERROR, appStateCollector.awaitItem())

            advanceTimeBy(1)

            verify(channelsManager, never()).getChannelsData(any())

            appStateCollector.cancelAndIgnoreRemainingEvents()
            channelsStateCollector.cancelAndIgnoreRemainingEvents()
        }
    }
}