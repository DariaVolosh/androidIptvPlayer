package com.example.iptvplayer.unitTests.time

import app.cash.turbine.turbineScope
import com.example.iptvplayer.data.NtpTimeClient
import com.example.iptvplayer.domain.time.TimeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension


@ExtendWith(MockitoExtension::class)
class TimeManagerTest {
    private var testDispatcher = StandardTestDispatcher()

    private lateinit var timeManager: TimeManager
    @Mock lateinit var ntpTimeClient: NtpTimeClient

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        timeManager = TimeManager(ntpTimeClient)
    }

    @AfterEach
    fun finish() {
        Dispatchers.resetMain()
    }

    @Test
    fun updateTime_timeIsAvailable_currentTimeAndLiveTimeAreUpdated() =
        runTest {
            turbineScope {
                val time = 1749935831L
                val liveTimeCollector = timeManager.liveTime.testIn(this)
                val currentTimeCollector = timeManager.currentTime.testIn(this)

                assertEquals(0L, liveTimeCollector.awaitItem())
                assertEquals(0L, currentTimeCollector.awaitItem())

                timeManager.updateLiveTime(time)
                timeManager.updateCurrentTime(time)

                assertEquals(time, liveTimeCollector.awaitItem())
                assertEquals(time, currentTimeCollector.awaitItem())

                liveTimeCollector.cancelAndIgnoreRemainingEvents()
                currentTimeCollector.cancelAndIgnoreRemainingEvents()
            }
        }
}