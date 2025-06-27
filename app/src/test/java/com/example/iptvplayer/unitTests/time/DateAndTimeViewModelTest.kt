package com.example.iptvplayer.unitTests.time

import app.cash.turbine.turbineScope
import com.example.iptvplayer.domain.media.MediaManager
import com.example.iptvplayer.domain.media.MediaPlaybackOrchestrator
import com.example.iptvplayer.domain.media.StreamTypeState
import com.example.iptvplayer.domain.time.CalendarManager
import com.example.iptvplayer.domain.time.DateManager
import com.example.iptvplayer.domain.time.TimeManager
import com.example.iptvplayer.domain.time.TimeOrchestrator
import com.example.iptvplayer.view.time.DateAndTimeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.whenever
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

@ExtendWith(MockitoExtension::class)
class DateAndTimeViewModelTest {
    private var testDispatcher = StandardTestDispatcher()

    @Mock private lateinit var dateManager: DateManager
    @Mock private lateinit var calendarManager: CalendarManager
    @Mock private lateinit var timeOrchestrator: TimeOrchestrator
    @Mock private lateinit var timeManager: TimeManager
    @Mock private lateinit var mediaManager: MediaManager
    @Mock private lateinit var mediaPlaybackOrchestrator: MediaPlaybackOrchestrator

    private lateinit var dateAndTimeViewModel: DateAndTimeViewModel

    private lateinit var liveTimeControlledFlow: MutableStateFlow<Long>
    private lateinit var currentTimeControlledFlow: MutableStateFlow<Long>
    private lateinit var isLiveControlledFlow: MutableStateFlow<Boolean>

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        liveTimeControlledFlow = MutableStateFlow(0L)
        currentTimeControlledFlow = MutableStateFlow(0L)
        isLiveControlledFlow = MutableStateFlow(true)

        whenever(timeOrchestrator.liveTime).thenReturn(liveTimeControlledFlow)
        whenever(timeOrchestrator.currentTime).thenReturn(currentTimeControlledFlow)
    }

    @After
    fun finish() {
        Dispatchers.resetMain()
    }

    @Test
    fun collectTime_isLive_updatesCurrentFullDateUsingLiveTime() = runTest {
        turbineScope {
            whenever(dateManager.formatDate(any(), any(), any())).thenAnswer { mock ->
                val date = mock.arguments[0] as Long
                val pattern = mock.arguments[1] as String
                val timeZone = mock.arguments[2] as TimeZone

                val formatter = SimpleDateFormat(pattern)
                if (timeZone != null) {
                    formatter.timeZone = timeZone
                }

                val formattedString = formatter.format(Date(date * 1000))
                formattedString
            }

            whenever(mediaPlaybackOrchestrator.isPaused).thenReturn(MutableStateFlow(false))
            whenever(mediaPlaybackOrchestrator.isSeeking).thenReturn(MutableStateFlow(false))
            whenever(mediaPlaybackOrchestrator.streamTypeState).thenReturn(MutableStateFlow(
                StreamTypeState.LIVE))

            dateAndTimeViewModel = DateAndTimeViewModel(
                dateManager = dateManager,
                calendarManager = calendarManager,
                timeOrchestrator = timeOrchestrator,
                mediaPlaybackOrchestrator = mediaPlaybackOrchestrator,
                viewModelScope = backgroundScope
            )

            val testCases = listOf(
                1750089835L to "Monday 16 June 20:03:55",
                1750089838L to "Monday 16 June 20:03:58",
                1750089841L to "Monday 16 June 20:04:01"
            )

            val currentFullDateCollector = dateAndTimeViewModel.currentFullDate.testIn(this)

            assertEquals("Current date not available", currentFullDateCollector.awaitItem())

            for (testCase in testCases) {
                liveTimeControlledFlow.value = testCase.first
                assertEquals(testCase.second, currentFullDateCollector.awaitItem())
            }

            currentFullDateCollector.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun updateTime_currentTimeAvailable_currentTimeUpdatedEverySecond() = runTest {
        turbineScope {
            whenever(timeOrchestrator.updateCurrentTime(any<Long>())).doAnswer { args ->
                val time = args.arguments[0] as Long
                currentTimeControlledFlow.value = time
            }

            whenever(mediaPlaybackOrchestrator.isPaused).thenReturn(MutableStateFlow(false))
            whenever(mediaPlaybackOrchestrator.isSeeking).thenReturn(MutableStateFlow(false))
            whenever(mediaPlaybackOrchestrator.streamTypeState).thenReturn(MutableStateFlow(
                StreamTypeState.LIVE))

            dateAndTimeViewModel = DateAndTimeViewModel(
                dateManager = dateManager,
                calendarManager = calendarManager,
                timeOrchestrator = timeOrchestrator,
                mediaPlaybackOrchestrator = mediaPlaybackOrchestrator,
                viewModelScope = backgroundScope
            )

            val currentTimeCollector = dateAndTimeViewModel.currentTime.testIn(this)

            assertEquals(0L, currentTimeCollector.awaitItem())
            val time = 1749935831L
            currentTimeControlledFlow.value = time
            assertEquals(time, currentTimeCollector.awaitItem())

            var seconds = 0

            while (seconds < 50) {
                advanceTimeBy(1000)
                seconds++
                assertEquals(time + seconds, currentTimeCollector.awaitItem())
            }

            currentTimeCollector.cancelAndIgnoreRemainingEvents()
        }
    }
}