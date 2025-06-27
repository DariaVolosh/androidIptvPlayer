package com.example.iptvplayer.integrationTests.domain

import app.cash.turbine.turbineScope
import com.example.iptvplayer.data.repositories.ChannelsRepository
import com.example.iptvplayer.domain.channels.ChannelsManager
import com.example.iptvplayer.domain.channels.ChannelsOrchestrator
import com.example.iptvplayer.domain.channels.ChannelsState
import com.example.iptvplayer.domain.errors.ErrorManager
import com.example.iptvplayer.domain.sharedPrefs.SharedPreferencesUseCase
import com.example.iptvplayer.retrofit.data.ChannelData
import com.example.iptvplayer.retrofit.data.StreamUrlTemplate
import com.example.iptvplayer.view.channels.CURRENT_CHANNEL_INDEX_KEY
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
class ChannelsOrchestratorAndChannelsManagerTest {
    private val testDispatcher = StandardTestDispatcher()

    @Mock private lateinit var sharedPreferencesUseCase: SharedPreferencesUseCase
    @Mock private lateinit var errorManager: ErrorManager
    @Mock private lateinit var channelsRepository: ChannelsRepository
    private lateinit var channelsOrchestrator: ChannelsOrchestrator
    private lateinit var channelsManager: ChannelsManager

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @Test
    fun fetchChannelsData_channelsDataAndStreamsUrlTemplatesAvailable_channelsDataUpdatedAndChannelsStateChangesToFetched() = runTest {
        turbineScope {
            val mockChannelsData = List(5) {index -> ChannelData(name = "Channel ${index + 1}") }
            val mockUrlTemplates = List(2) {index -> StreamUrlTemplate("Template ${index + 1}")}
            whenever(channelsRepository.getStreamsUrlTemplates(any())).thenReturn(mockUrlTemplates)
            whenever(channelsRepository.parseChannelsData(any(), any())).thenReturn(mockChannelsData)

            channelsManager = ChannelsManager(
                channelsRepository = channelsRepository
            )

            channelsOrchestrator = ChannelsOrchestrator(
                channelsManager = channelsManager,
                sharedPreferencesUseCase = sharedPreferencesUseCase,
                errorManager = errorManager,
                orchestratorScope = backgroundScope
            )

            val channelsDataCollector = channelsManager.channelsData.testIn(this)
            val channelsStateCollector = channelsOrchestrator.channelsState.testIn(this)

            assertEquals(emptyList<ChannelData>(), channelsDataCollector.awaitItem())
            assertEquals(ChannelsState.INITIALIZING, channelsStateCollector.awaitItem())

            channelsOrchestrator.fetchChannelsData()

            advanceTimeBy(1)

            verify(channelsRepository, times(1)).getStreamsUrlTemplates(any())
            verify(channelsRepository, times(1)).parseChannelsData(any(), any())

            assertEquals(mockChannelsData, channelsDataCollector.awaitItem())
            assertEquals(ChannelsState.FETCHING, channelsStateCollector.awaitItem())
            assertEquals(ChannelsState.FETCHED, channelsStateCollector.awaitItem())

            channelsDataCollector.cancelAndIgnoreRemainingEvents()
            channelsStateCollector.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun fetchChannelsData_channelsDataAndStreamsUrlTemplatesNotAvailable_channelsDataStaysEmptyAndChannelsStateChangesToError() = runTest {
        turbineScope {
            val mockUrlTemplates = emptyList<StreamUrlTemplate>()
            whenever(channelsRepository.getStreamsUrlTemplates(any())).thenReturn(mockUrlTemplates)

            channelsManager = ChannelsManager(
                channelsRepository = channelsRepository
            )

            channelsOrchestrator = ChannelsOrchestrator(
                channelsManager = channelsManager,
                sharedPreferencesUseCase = sharedPreferencesUseCase,
                errorManager = errorManager,
                orchestratorScope = backgroundScope
            )

            val channelsDataCollector = channelsManager.channelsData.testIn(this)
            val channelsStateCollector = channelsOrchestrator.channelsState.testIn(this)

            assertEquals(emptyList<ChannelData>(), channelsDataCollector.awaitItem())
            assertEquals(ChannelsState.INITIALIZING, channelsStateCollector.awaitItem())

            channelsOrchestrator.fetchChannelsData()

            advanceTimeBy(1)

            verify(channelsRepository, times(1)).getStreamsUrlTemplates(any())
            verify(channelsRepository, never()).parseChannelsData(any(), any())

            channelsDataCollector.expectNoEvents()
            assertEquals(ChannelsState.FETCHING, channelsStateCollector.awaitItem())
            assertEquals(ChannelsState.ERROR, channelsStateCollector.awaitItem())

            channelsDataCollector.cancelAndIgnoreRemainingEvents()
            channelsStateCollector.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun fetchChannelsData_channelsDataNotAvailableButStreamsUrlTemplatesAvailable_channelsDataStaysEmptyAndChannelsStateChangesToError() = runTest {
        turbineScope {
            val mockUrlTemplates = List(2) {index -> StreamUrlTemplate("Template ${index + 1}")}
            whenever(channelsRepository.getStreamsUrlTemplates(any())).thenReturn(mockUrlTemplates)
            whenever(channelsRepository.parseChannelsData(any(), any())).thenReturn(emptyList())

            channelsManager = ChannelsManager(
                channelsRepository = channelsRepository
            )

            channelsOrchestrator = ChannelsOrchestrator(
                channelsManager = channelsManager,
                sharedPreferencesUseCase = sharedPreferencesUseCase,
                errorManager = errorManager,
                orchestratorScope = backgroundScope
            )

            val channelsDataCollector = channelsManager.channelsData.testIn(this)
            val channelsStateCollector = channelsOrchestrator.channelsState.testIn(this)

            assertEquals(emptyList<ChannelData>(), channelsDataCollector.awaitItem())
            assertEquals(ChannelsState.INITIALIZING, channelsStateCollector.awaitItem())

            channelsOrchestrator.fetchChannelsData()
            assertEquals(ChannelsState.FETCHING, channelsStateCollector.awaitItem())

            advanceTimeBy(1)

            verify(channelsRepository, times(1)).getStreamsUrlTemplates(any())
            verify(channelsRepository, times(1)).parseChannelsData(any(), any())

            channelsDataCollector.expectNoEvents()
            assertEquals(ChannelsState.ERROR, channelsStateCollector.awaitItem())

            channelsDataCollector.cancelAndIgnoreRemainingEvents()
            channelsStateCollector.cancelAndIgnoreRemainingEvents()
        }
    }


    @Test
    fun updateChannelIndex_withCachedIndex_currentAndFocusedChannelIndicesUpdate() = runTest {
        turbineScope {
            val mockChannelIndex = 3
            val mockChannelDataList = List(5) {index -> ChannelData(name = "channel${index+1}")}
            whenever(sharedPreferencesUseCase.getIntValue(CURRENT_CHANNEL_INDEX_KEY)).thenReturn(mockChannelIndex)

            channelsManager = ChannelsManager(
                channelsRepository = channelsRepository
            )

            channelsOrchestrator = ChannelsOrchestrator(
                channelsManager = channelsManager,
                sharedPreferencesUseCase = sharedPreferencesUseCase,
                errorManager = errorManager ,
                orchestratorScope = backgroundScope
            )

            val currentChannelIndexCollector = channelsOrchestrator.currentChannelIndex.testIn(this)
            val focusedChannelIndex = channelsManager.focusedChannelIndex.testIn(this)

            assertEquals(-1, currentChannelIndexCollector.awaitItem())
            assertEquals(-1, focusedChannelIndex.awaitItem())

            channelsOrchestrator.updateChannelsData(mockChannelDataList)

            verify(sharedPreferencesUseCase, never()).getIntValue(CURRENT_CHANNEL_INDEX_KEY)
            advanceTimeBy(1)
            verify(sharedPreferencesUseCase, times(1)).getIntValue(CURRENT_CHANNEL_INDEX_KEY)

            assertEquals(mockChannelIndex, currentChannelIndexCollector.awaitItem())
            assertEquals(mockChannelIndex, focusedChannelIndex.awaitItem())

            currentChannelIndexCollector.cancelAndIgnoreRemainingEvents()
            focusedChannelIndex.cancelAndIgnoreRemainingEvents()
        }
    }
}