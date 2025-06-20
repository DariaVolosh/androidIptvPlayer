package com.example.iptvplayer.unitTests.time

import android.util.Log
import app.cash.turbine.turbineScope
import com.example.iptvplayer.domain.sharedPrefs.SharedPreferencesUseCase
import com.example.iptvplayer.view.player.MediaManager
import com.example.iptvplayer.view.time.DateManager
import com.example.iptvplayer.view.time.TimeManager
import com.example.iptvplayer.view.time.TimeOrchestrator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.whenever


class TimeOrchestratorTest {
    private var testDispatcher = StandardTestDispatcher()

    @Mock private lateinit var sharedPreferencesUseCase: SharedPreferencesUseCase
    @Mock private lateinit var timeManager: TimeManager
    @Mock private lateinit var dateManager: DateManager
    @Mock private lateinit var mediaManager: MediaManager

    private lateinit var timeOrchestrator: TimeOrchestrator

    private lateinit var liveTimeControlledFlow: MutableStateFlow<Long>
    private lateinit var currentTimeControlledFlow: MutableStateFlow<Long>

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        MockitoAnnotations.openMocks(this)
        Mockito.mockStatic(Log::class.java)

        liveTimeControlledFlow = MutableStateFlow(0L)
        currentTimeControlledFlow = MutableStateFlow(0L)

        whenever(mediaManager.isPaused).thenReturn(MutableStateFlow(false))
        whenever(mediaManager.isSeeking).thenReturn(MutableStateFlow(false))
        whenever(timeManager.liveTime).thenReturn(liveTimeControlledFlow)
        whenever(timeManager.currentTime).thenReturn(currentTimeControlledFlow)
        whenever(mediaManager.isLive).thenReturn(MutableStateFlow(true))

       whenever(timeManager.updateLiveTime(any())).doAnswer { mock ->
            val liveTime = mock.arguments[0] as Long
            liveTimeControlledFlow.value = liveTime
        }
        whenever(timeManager.updateCurrentTime(any())).doAnswer { mock ->
            val currentTime = mock.arguments[0] as Long
            currentTimeControlledFlow.value = currentTime
        }

        whenever(sharedPreferencesUseCase.saveLongValue(any(), any())).doAnswer {  }
    }

    @After
    fun finish() {
        Dispatchers.resetMain()
    }

    @Test
    fun updateTime_liveTimeAndCurrentTimeAvailable_liveAndCurrentTimeAreUpdatedEverySecond() = runTest {
        turbineScope {
            timeOrchestrator = TimeOrchestrator(
                timeManager =  timeManager,
                dateManager = dateManager,
                mediaManager = mediaManager,
                sharedPreferencesUseCase = sharedPreferencesUseCase,
                orchestratorScope = backgroundScope
            )

            val time = 1749935831L
            whenever(timeManager.getGmtTime()).thenReturn(time)
            val liveTimeCollector = timeOrchestrator.liveTime.testIn(this)
            val currentTimeCollector = timeOrchestrator.currentTime.testIn(this)

            assertEquals(0L, liveTimeCollector.awaitItem())
            assertEquals(0L, currentTimeCollector.awaitItem())

            timeOrchestrator.initialize(0L)

            assertEquals(time, liveTimeCollector.awaitItem())
            assertEquals(time, currentTimeCollector.awaitItem())

            var seconds = 0

            while (seconds < 50) {
                advanceTimeBy(1000)
                seconds++
                assertEquals(time + seconds, liveTimeCollector.awaitItem())
                assertEquals(time + seconds, currentTimeCollector.awaitItem())
            }

            liveTimeCollector.cancelAndIgnoreRemainingEvents()
            currentTimeCollector.cancelAndIgnoreRemainingEvents()
        }
    }
}