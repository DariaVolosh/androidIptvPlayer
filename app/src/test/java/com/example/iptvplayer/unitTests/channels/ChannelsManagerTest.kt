package com.example.iptvplayer.unitTests.channels

import app.cash.turbine.turbineScope
import com.example.iptvplayer.data.repositories.ChannelsRepository
import com.example.iptvplayer.domain.channels.ChannelsManager
import com.example.iptvplayer.retrofit.data.ChannelData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class ChannelsManagerTest {
    private val testDispatcher = StandardTestDispatcher()

    @Mock private lateinit var channelsRepository: ChannelsRepository
    private lateinit var channelsManager: ChannelsManager

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        MockitoAnnotations.openMocks(this)

        channelsManager = ChannelsManager(
            channelsRepository = channelsRepository
        )
    }

    @Test
    fun updateChannelIndex_focusedChannelIndexAndCurrentChannelIndexAvailableChannelsDataNotAvailable_doesNotUpdateIndices() = runTest {
        turbineScope {
            val currentChannelIndexCollector = channelsManager.currentChannelIndex.testIn(this)
            val focusedChannelIndexCollector = channelsManager.focusedChannelIndex.testIn(this)

            assertEquals(-1, currentChannelIndexCollector.awaitItem())
            assertEquals(-1, focusedChannelIndexCollector.awaitItem())

            channelsManager.updateChannelIndex(5, true)
            channelsManager.updateChannelIndex(5, false)

            currentChannelIndexCollector.expectNoEvents()
            focusedChannelIndexCollector.expectNoEvents()

            currentChannelIndexCollector.cancelAndIgnoreRemainingEvents()
            focusedChannelIndexCollector.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun updateChannelIndex_focusedChannelIndexAndCurrentChannelIndexAvailableChannelsDataAvailable_updatesBoth() = runTest {
        turbineScope {
            val currentChannelIndexCollector = channelsManager.currentChannelIndex.testIn(this)
            val focusedChannelIndexCollector = channelsManager.focusedChannelIndex.testIn(this)

            val mockChannelsData = List(5) {_ -> ChannelData()}
            channelsManager.updateChannelsData(mockChannelsData)

            assertEquals(-1, currentChannelIndexCollector.awaitItem())
            assertEquals(-1, focusedChannelIndexCollector.awaitItem())

            channelsManager.updateChannelIndex(2, true)
            channelsManager.updateChannelIndex(2, false)

            assertEquals(2, currentChannelIndexCollector.awaitItem())
            assertEquals(2, focusedChannelIndexCollector.awaitItem())

            channelsManager.updateChannelIndex(1, true)
            channelsManager.updateChannelIndex(3, false)

            assertEquals(1, currentChannelIndexCollector.awaitItem())
            assertEquals(3, focusedChannelIndexCollector.awaitItem())

            currentChannelIndexCollector.cancelAndIgnoreRemainingEvents()
            focusedChannelIndexCollector.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun updateChannelIndex_currentChannelIndexAvailableChannelsDataAvailable_updatesCurrentChannelAndCurrentIndex() = runTest {
        turbineScope {
            val currentChannelIndexCollector = channelsManager.currentChannelIndex.testIn(this)
            val currentChannelDataCollector = channelsManager.currentChannel.testIn(this)

            assertEquals(-1, currentChannelIndexCollector.awaitItem())
            assertEquals(ChannelData(), currentChannelDataCollector.awaitItem())

            val mockChannelsData = List(5) {index -> ChannelData(name = "$index")}
            channelsManager.updateChannelsData(mockChannelsData)

            channelsManager.updateChannelIndex(2, true)
            assertEquals(2, currentChannelIndexCollector.awaitItem())
            assertEquals(mockChannelsData[2], currentChannelDataCollector.awaitItem())

            currentChannelIndexCollector.cancelAndIgnoreRemainingEvents()
            currentChannelDataCollector.cancelAndIgnoreRemainingEvents()
        }
    }
}