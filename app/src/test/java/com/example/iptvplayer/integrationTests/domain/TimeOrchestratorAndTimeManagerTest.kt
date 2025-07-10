package com.example.iptvplayer.integrationTests.domain

import android.content.Context
import app.cash.turbine.turbineScope
import com.example.iptvplayer.data.NtpTimeClient
import com.example.iptvplayer.domain.errors.ErrorManager
import com.example.iptvplayer.domain.media.MediaManager
import com.example.iptvplayer.domain.sharedPrefs.SharedPreferencesUseCase
import com.example.iptvplayer.domain.time.CalendarManager
import com.example.iptvplayer.domain.time.DateManager
import com.example.iptvplayer.domain.time.TimeManager
import com.example.iptvplayer.domain.time.TimeOrchestrator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class TimeOrchestratorAndTimeManagerTest {
    private val testDispatcher = StandardTestDispatcher()

    @Mock private lateinit var context: Context
    @Mock private lateinit var sharedPreferencesUseCase: SharedPreferencesUseCase
    @Mock private lateinit var calendarManager: CalendarManager
    @Mock private lateinit var errorManager: ErrorManager
    @Mock private lateinit var dateManager: DateManager
    @Mock private lateinit var mediaManager: MediaManager

    private lateinit var timeOrchestrator: TimeOrchestrator

    @Mock private lateinit var ntpTimeClient: NtpTimeClient
    private lateinit var timeManager: TimeManager

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        timeManager = TimeManager(
            ntpClient = ntpTimeClient
        )

        whenever(context.getString(any())).thenReturn("")
    }

    @AfterEach
    fun finish() {
        Dispatchers.resetMain()
    }

    @Test
    fun startLiveTimeUpdate_clockSkewDetected_warningMessageShownAndLiveTimeUpdatedToNetworkTime() = runTest {
        turbineScope {
            // date in the past (skew is more than 1 minute)
            val networkTime = 1751206298L
            whenever(timeManager.getNetworkCurrentTime()).thenReturn(networkTime)

            timeOrchestrator = TimeOrchestrator(
                dateManager = dateManager,
                timeManager = timeManager,
                calendarManager = calendarManager,
                errorManager = errorManager,
                mediaManager = mediaManager,
                sharedPreferencesUseCase = sharedPreferencesUseCase,
                context = context,
                orchestratorScope = backgroundScope
            )

            val orchestratorLiveTimeCollector = timeOrchestrator.liveTime.testIn(this)
            val managerLiveTimeCollector = timeManager.liveTime.testIn(this)

            assertEquals(0L, orchestratorLiveTimeCollector.awaitItem())
            assertEquals(0L, managerLiveTimeCollector.awaitItem())

            advanceTimeBy(1)

            verify(errorManager, times(1)).publishError(any())

            assertEquals(networkTime, orchestratorLiveTimeCollector.awaitItem())
            assertEquals(networkTime, managerLiveTimeCollector.awaitItem())

            orchestratorLiveTimeCollector.cancelAndIgnoreRemainingEvents()
            managerLiveTimeCollector.cancelAndIgnoreRemainingEvents()
        }
    }
}