package com.example.iptvplayer.unitTests.time

import app.cash.turbine.turbineScope
import com.example.iptvplayer.view.time.CalendarManager
import com.example.iptvplayer.view.time.DateAndTimeViewModel
import com.example.iptvplayer.view.time.DateManager
import com.example.iptvplayer.view.time.TimeOrchestrator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
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
import org.mockito.kotlin.whenever
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

class DateAndTimeViewModelTest {
    private var testDispatcher = StandardTestDispatcher()

    @Mock private lateinit var dateManager: DateManager
    @Mock private lateinit var calendarManager: CalendarManager
    @Mock private lateinit var timeOrchestrator: TimeOrchestrator

    private lateinit var dateAndTimeViewModel: DateAndTimeViewModel

    private lateinit var liveTimeControlledFlow: MutableStateFlow<Long>
    private lateinit var currentTimeControlledFlow: MutableStateFlow<Long>
    private lateinit var isLiveControlledFlow: MutableStateFlow<Boolean>

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        MockitoAnnotations.openMocks(this)

        liveTimeControlledFlow = MutableStateFlow(0L)
        currentTimeControlledFlow = MutableStateFlow(0L)
        isLiveControlledFlow = MutableStateFlow(true)

        whenever(timeOrchestrator.liveTime).thenReturn(liveTimeControlledFlow)
        whenever(timeOrchestrator.currentTime).thenReturn(currentTimeControlledFlow)
        whenever(timeOrchestrator.isLive).thenReturn(isLiveControlledFlow)

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
    fun collectTime_isLive_updatesCurrentFullDateUsingLiveTime() = runTest {
        turbineScope {
            dateAndTimeViewModel = DateAndTimeViewModel(
                dateManager = dateManager,
                calendarManager = calendarManager,
                timeOrchestrator = timeOrchestrator,
                coroutineScope = backgroundScope
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
}