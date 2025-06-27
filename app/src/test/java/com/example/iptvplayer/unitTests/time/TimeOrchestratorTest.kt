package com.example.iptvplayer.unitTests.time

import app.cash.turbine.turbineScope
import com.example.iptvplayer.domain.media.MediaManager
import com.example.iptvplayer.domain.sharedPrefs.SharedPreferencesUseCase
import com.example.iptvplayer.domain.time.DateManager
import com.example.iptvplayer.domain.time.TimeManager
import com.example.iptvplayer.domain.time.TimeOrchestrator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
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
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.whenever


@ExtendWith(MockitoExtension::class)
class TimeOrchestratorTest {
    private var testDispatcher = StandardTestDispatcher()

    @Mock private lateinit var sharedPreferencesUseCase: SharedPreferencesUseCase
    @Mock private lateinit var timeManager: TimeManager
    @Mock private lateinit var dateManager: DateManager
    @Mock private lateinit var mediaManager: MediaManager

    private lateinit var timeOrchestrator: TimeOrchestrator

    private lateinit var liveTimeControlledFlow: MutableStateFlow<Long>
    private lateinit var currentTimeControlledFlow: MutableStateFlow<Long>

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        liveTimeControlledFlow = MutableStateFlow(0L)
        currentTimeControlledFlow = MutableStateFlow(0L)

        whenever(timeManager.liveTime).thenReturn(liveTimeControlledFlow)
        whenever(timeManager.currentTime).thenReturn(currentTimeControlledFlow)

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

    @AfterEach
    fun finish() {
        Dispatchers.resetMain()
    }

    @Test
    fun updateTime_liveTimeAvailable_liveTimeUpdatedEverySecond() = runTest {
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

            assertEquals(0L, liveTimeCollector.awaitItem())

            timeOrchestrator.initialize(0L)

            assertEquals(time, liveTimeCollector.awaitItem())

            var seconds = 0

            while (seconds < 50) {
                advanceTimeBy(1000)
                seconds++
                assertEquals(time + seconds, liveTimeCollector.awaitItem())
            }

            liveTimeCollector.cancelAndIgnoreRemainingEvents()
        }
    }
}