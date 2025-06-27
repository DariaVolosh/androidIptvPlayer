package com.example.iptvplayer.integrationTests.ui

import app.cash.turbine.turbineScope
import com.example.iptvplayer.domain.media.MediaManager
import com.example.iptvplayer.domain.media.MediaPlaybackOrchestrator
import com.example.iptvplayer.domain.media.StreamTypeState
import com.example.iptvplayer.domain.sharedPrefs.SharedPreferencesUseCase
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
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.AfterEach
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
class DateAndTimeViewModelAndTimeOrchestratorTest {
    private var testDispatcher = StandardTestDispatcher()

    @Mock private lateinit var dateManager: DateManager
    @Mock private lateinit var timeManager: TimeManager
    @Mock private lateinit var mediaManager: MediaManager
    @Mock private lateinit var mediaPlaybackOrchestrator: MediaPlaybackOrchestrator
    @Mock private lateinit var calendarManager: CalendarManager
    @Mock private lateinit var sharedPreferencesUseCase: SharedPreferencesUseCase

    lateinit var timeOrchestrator: TimeOrchestrator
    lateinit var dateAndTimeViewModel: DateAndTimeViewModel

    private lateinit var liveTimeControlledFlow: MutableStateFlow<Long>
    private lateinit var currentTimeControlledFlow: MutableStateFlow<Long>

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        liveTimeControlledFlow = MutableStateFlow(0L)
        currentTimeControlledFlow = MutableStateFlow(0L)

        whenever(mediaPlaybackOrchestrator.isPaused).thenReturn(MutableStateFlow(false))
        whenever(mediaPlaybackOrchestrator.isSeeking).thenReturn(MutableStateFlow(false))
        whenever(timeManager.liveTime).thenReturn(liveTimeControlledFlow)
        whenever(timeManager.currentTime).thenReturn(currentTimeControlledFlow)
        whenever(mediaPlaybackOrchestrator.streamTypeState).thenReturn(MutableStateFlow(
            StreamTypeState.LIVE))

        whenever(timeManager.updateLiveTime(any())).doAnswer { mock ->
            val liveTime = mock.arguments[0] as Long
            liveTimeControlledFlow.value = liveTime
        }
        whenever(timeManager.updateCurrentTime(any())).doAnswer { mock ->
            val currentTime = mock.arguments[0] as Long
            currentTimeControlledFlow.value = currentTime
        }

        whenever(sharedPreferencesUseCase.saveLongValue(any(), any())).doAnswer {  }

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
    }

    @AfterEach
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
                mediaPlaybackOrchestrator = mediaPlaybackOrchestrator,
                viewModelScope = backgroundScope
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