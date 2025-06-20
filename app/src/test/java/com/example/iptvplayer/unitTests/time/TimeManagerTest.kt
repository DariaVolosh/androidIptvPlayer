package com.example.iptvplayer.unitTests.time

import android.util.Log
import app.cash.turbine.turbineScope
import com.example.iptvplayer.data.NtpTimeClient
import com.example.iptvplayer.view.time.TimeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations


class TimeManagerTest {
    private var testDispatcher = StandardTestDispatcher()

    private lateinit var timeManager: TimeManager
    @Mock lateinit var ntpTimeClient: NtpTimeClient

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        MockitoAnnotations.openMocks(this)
        Mockito.mockStatic(Log::class.java)

        timeManager = TimeManager(ntpTimeClient)
    }

    @After
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