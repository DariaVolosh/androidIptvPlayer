package com.example.iptvplayer.integrationTests

import app.cash.turbine.turbineScope
import com.example.iptvplayer.domain.sharedPrefs.SharedPreferencesUseCase
import com.example.iptvplayer.view.player.MediaManager
import com.example.iptvplayer.view.time.CalendarManager
import com.example.iptvplayer.view.time.DateAndTimeViewModel
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
import org.mockito.ArgumentMatchers.nullable
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.whenever
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

class TimeIntegrationTest {
    private var testDispatcher = StandardTestDispatcher()

    @Mock private lateinit var dateManager: DateManager
    @Mock private lateinit var timeManager: TimeManager
    @Mock private lateinit var mediaManager: MediaManager
    @Mock private lateinit var calendarManager: CalendarManager
    @Mock private lateinit var sharedPreferencesUseCase: SharedPreferencesUseCase

    lateinit var timeOrchestrator: TimeOrchestrator
    lateinit var dateAndTimeViewModel: DateAndTimeViewModel

    private lateinit var liveTimeControlledFlow: MutableStateFlow<Long>
    private lateinit var currentTimeControlledFlow: MutableStateFlow<Long>

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

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

        whenever(dateManager.formatDate(any(), any(), nullable(TimeZone::class.java))).thenAnswer { mock ->
            val date = mock.arguments[0] as Long
            val pattern = mock.arguments[1] as String
            val timeZone = mock.arguments[2] as TimeZone?

            val formatter = SimpleDateFormat(pattern)
            if (timeZone != null) {
                formatter.timeZone = timeZone
            }

            val formattedString = formatter.format(Date(date * 1000))
            formattedString
        }
    }

    @After
    fun finish() {
        Dispatchers.resetMain()
    }

    @Test
    fun timeFlow_updatesViewModel_viaOrchestrator() = runTest {
        turbineScope {
            timeOrchestrator = TimeOrchestrator(
                dateManager = dateManager,
                timeManager = timeManager,
                mediaManager = mediaManager,
                sharedPreferencesUseCase = sharedPreferencesUseCase,
                orchestratorScope = backgroundScope
            )

            dateAndTimeViewModel = DateAndTimeViewModel(
                dateManager = dateManager,
                calendarManager = calendarManager,
                timeOrchestrator = timeOrchestrator,
                coroutineScope = backgroundScope
            )

            val time = 1750089835L
            val currentFullDateCollector = dateAndTimeViewModel.currentFullDate.testIn(this)

            whenever(timeManager.getGmtTime()).thenReturn(time)

            var seconds = 0
            advanceTimeBy(1000)
            assertEquals("Current date not available", currentFullDateCollector.awaitItem())

            timeOrchestrator.initialize(0L)

            assertEquals("Monday 16 June 20:03:55", currentFullDateCollector.awaitItem())

            currentFullDateCollector.cancelAndIgnoreRemainingEvents()
        }
    }
}