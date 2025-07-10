package com.example.iptvplayer.domain.time

import android.content.Context
import com.example.iptvplayer.R
import com.example.iptvplayer.di.IoDispatcher
import com.example.iptvplayer.domain.errors.ErrorManager
import com.example.iptvplayer.domain.media.MediaManager
import com.example.iptvplayer.domain.sharedPrefs.SharedPreferencesUseCase
import com.example.iptvplayer.view.errors.ErrorData
import com.example.iptvplayer.view.errors.ErrorDismissButtonData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

const val CURRENT_TIME_KEY = "current_time"
const val IS_LIVE_KEY = "is_live"

@Singleton
class TimeOrchestrator @Inject constructor(
    private val dateManager: DateManager,
    private val timeManager: TimeManager,
    private val calendarManager: CalendarManager,
    private val errorManager: ErrorManager,
    private val mediaManager: MediaManager,
    private val sharedPreferencesUseCase: SharedPreferencesUseCase,
    @ApplicationContext private val context: Context,
    @IoDispatcher private val orchestratorScope: CoroutineScope
) {
    val liveTime: StateFlow<Long> = timeManager.liveTime.stateIn(
        orchestratorScope, SharingStarted.Eagerly, 0L
    )

    val currentTime: StateFlow<Long> = timeManager.currentTime.stateIn(
        orchestratorScope, SharingStarted.Eagerly, 0L
    )

    // 60 seconds
    private val CLOCK_SKEW_TOLERANCE = 60

    init {
        orchestratorScope.launch {
            val cachedCurrentTime = sharedPreferencesUseCase.getLongValue(CURRENT_TIME_KEY)
            initialize(cachedCurrentTime)
        }
    }

    fun isClockMisconfigured(
        networkCurrentTime: Long,
        deviceCurrentTime: Long
    ): Boolean {
        return abs(networkCurrentTime - deviceCurrentTime) > CLOCK_SKEW_TOLERANCE
    }

    fun startLiveTimeUpdate(isClockMisconfigured: Boolean) {
        orchestratorScope.launch {
            if (isClockMisconfigured) {
                val errorDismissButtonData = ErrorDismissButtonData(
                    R.string.continue_button_label
                ) { errorManager.resetError() }

                errorManager.publishError(
                    ErrorData(
                        errorTitle = context.getString(R.string.timezone_misconfig),
                        errorDescription = context.getString(R.string.timezone_misconfig_description),
                        errorIcon = R.drawable.warning_icon,
                        errorDismissButton = errorDismissButtonData
                    )
                )
            }

            while (true) {
                delay(1000)
                updateLiveTime(liveTime.value + 1)
            }
        }
    }


    fun updateCurrentTime(time: Long) {
        if (time != 0L) {
            val datePattern = "EEEE d MMMM HH:mm:ss"
            //Log.i("current time", "current: ${Utils.formatDate(time, datePattern)}")
            sharedPreferencesUseCase.saveLongValue(CURRENT_TIME_KEY, time)
            timeManager.updateCurrentTime(time)
        }
    }

    fun updateLiveTime(time: Long) {
        timeManager.updateLiveTime(time)
    }

    suspend fun initialize(cachedCurrentTime: Long) {
        val currentLiveTime = timeManager.getNetworkCurrentTime()
        val deviceCurrentTime = timeManager.getDeviceCurrentTime()
        val isClockMisconfigured = isClockMisconfigured(currentLiveTime, deviceCurrentTime)
        startLiveTimeUpdate(isClockMisconfigured)
        updateLiveTime(currentLiveTime)

        if (cachedCurrentTime == 0L) {
            // current time was not fetched from cache, set it to live time
            updateCurrentTime(currentLiveTime)
        } else {
            // current time is available from cache, set it
            updateCurrentTime(cachedCurrentTime)
        }
    }

    fun cancelTimeUpdate() {
        orchestratorScope.cancel()
    }

    fun getCalendarHour(date: Long): Int {
        val calendar = calendarManager.getCalendar(date)
        val calendarHour = calendarManager.getCalendarHour(calendar)
        return calendarHour
    }

    fun getCalendarMinute(date: Long): Int {
        val calendar = calendarManager.getCalendar(date)
        val calendarMinute = calendarManager.getCalendarMinute(calendar)
        return calendarMinute
    }
}