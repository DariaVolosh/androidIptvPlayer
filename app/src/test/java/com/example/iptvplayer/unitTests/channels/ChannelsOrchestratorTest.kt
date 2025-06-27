package com.example.iptvplayer.unitTests.channels

import app.cash.turbine.turbineScope
import com.example.iptvplayer.domain.channels.ChannelsManager
import com.example.iptvplayer.domain.channels.ChannelsOrchestrator
import com.example.iptvplayer.domain.errors.ErrorManager
import com.example.iptvplayer.domain.sharedPrefs.SharedPreferencesUseCase
import com.example.iptvplayer.retrofit.data.ChannelData
import com.example.iptvplayer.view.channels.CURRENT_CHANNEL_INDEX_KEY
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
class ChannelsOrchestratorTest {
    private val testDispatcher = StandardTestDispatcher()

    @Mock private lateinit var errorManager: ErrorManager
    @Mock private lateinit var channelsManager: ChannelsManager
    @Mock private lateinit var sharedPreferencesUseCase: SharedPreferencesUseCase
    private lateinit var channelsOrchestrator: ChannelsOrchestrator

    val channelsDataControlledFlow = MutableStateFlow(emptyList<ChannelData>())
    val currentChannelIndexControlledFLow = MutableStateFlow(-1)

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @Test
    fun getCachedChannelIndex_cachedIndexAvailable_returnsCachedIndex() = runTest {
        val mockedCachedValue = 5
        whenever(sharedPreferencesUseCase.getIntValue(CURRENT_CHANNEL_INDEX_KEY)).thenReturn(mockedCachedValue)

        channelsOrchestrator = ChannelsOrchestrator(
            channelsManager = channelsManager,
            sharedPreferencesUseCase = sharedPreferencesUseCase,
            errorManager = errorManager,
            orchestratorScope = backgroundScope
        )

        assertEquals(5, channelsOrchestrator.getCachedChannelIndex())
    }

    @Test
    fun getCachedChannelIndex_cachedIndexNotAvailable_returns0() = runTest {
        val mockedCachedValue = -1
        whenever(sharedPreferencesUseCase.getIntValue(CURRENT_CHANNEL_INDEX_KEY)).thenReturn(mockedCachedValue)

        channelsOrchestrator = ChannelsOrchestrator(
            channelsManager = channelsManager,
            sharedPreferencesUseCase = sharedPreferencesUseCase,
            errorManager = errorManager,
            orchestratorScope = backgroundScope
        )

        assertEquals(0, channelsOrchestrator.getCachedChannelIndex())
    }

    // channel index on initial app launch
    @Test
    fun init_channelsDataAvailableAndCachedChannelIndexNotAvailable_updateChannelIndexCalledAndCurrentChannelIndexUpdatedTo0() = runTest {
        turbineScope {
            val mockChannelDataList = List(5) {index -> ChannelData(name = "channel${index+1}")}

            whenever(channelsManager.channelsData).thenReturn(channelsDataControlledFlow)
            whenever(channelsManager.currentChannelIndex).thenReturn(currentChannelIndexControlledFLow)

            whenever(channelsManager.updateChannelIndex(any(), any())).thenAnswer {  }

            whenever(sharedPreferencesUseCase.getIntValue(CURRENT_CHANNEL_INDEX_KEY)).thenReturn(-1)

            channelsOrchestrator = ChannelsOrchestrator(
                channelsManager = channelsManager,
                sharedPreferencesUseCase = sharedPreferencesUseCase,
                errorManager = errorManager,
                orchestratorScope = backgroundScope
            )

            verify(sharedPreferencesUseCase, never()).getIntValue(CURRENT_CHANNEL_INDEX_KEY)
            channelsDataControlledFlow.value = mockChannelDataList
            advanceTimeBy(1)
            verify(sharedPreferencesUseCase, times(1)).getIntValue(CURRENT_CHANNEL_INDEX_KEY)
            verify(channelsManager, times(1)).updateChannelIndex(0, true)
            verify(channelsManager, times(1)).updateChannelIndex(0, false)
        }
    }

    // channel index on initial app launch
    @Test
    fun init_channelsDataAvailableAndCachedChannelIndexAvailable_updateChannelIndexCalledAndCurrentChannelIndexUpdatedToCachedIndex() = runTest {
        turbineScope {
            val mockChannelDataList = List(5) {index -> ChannelData(name = "channel${index+1}")}

            whenever(channelsManager.channelsData).thenReturn(channelsDataControlledFlow)
            whenever(channelsManager.currentChannelIndex).thenReturn(currentChannelIndexControlledFLow)

            whenever(channelsManager.updateChannelIndex(any(), any())).thenAnswer {  }

            whenever(sharedPreferencesUseCase.getIntValue(CURRENT_CHANNEL_INDEX_KEY)).thenReturn(3)

            channelsOrchestrator = ChannelsOrchestrator(
                channelsManager = channelsManager,
                sharedPreferencesUseCase = sharedPreferencesUseCase,
                errorManager = errorManager,
                orchestratorScope = backgroundScope
            )

            verify(sharedPreferencesUseCase, never()).getIntValue(CURRENT_CHANNEL_INDEX_KEY)
            channelsDataControlledFlow.value = mockChannelDataList
            advanceTimeBy(1)
            verify(sharedPreferencesUseCase, times(1)).getIntValue(CURRENT_CHANNEL_INDEX_KEY)
            verify(channelsManager, times(1)).updateChannelIndex(3, true)
            verify(channelsManager, times(1)).updateChannelIndex(3, false)
        }
    }
}