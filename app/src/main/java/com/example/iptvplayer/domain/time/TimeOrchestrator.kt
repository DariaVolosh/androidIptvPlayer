package com.example.iptvplayer.domain.time

import com.example.iptvplayer.di.IoDispatcher
import com.example.iptvplayer.domain.media.MediaManager
import com.example.iptvplayer.domain.sharedPrefs.SharedPreferencesUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

const val CURRENT_TIME_KEY = "current_time"
const val IS_LIVE_KEY = "is_live"

@Singleton
class TimeOrchestrator @Inject constructor(
    private val timeManager: TimeManager,
    private val dateManager: DateManager,
    private val sharedPreferencesUseCase: SharedPreferencesUseCase,
    private val mediaManager: MediaManager,
    @IoDispatcher private val orchestratorScope: CoroutineScope
) {
    val liveTime: StateFlow<Long> = timeManager.liveTime.stateIn(
        orchestratorScope, SharingStarted.Eagerly, 0L
    )

    val currentTime: StateFlow<Long> = timeManager.currentTime.stateIn(
        orchestratorScope, SharingStarted.Eagerly, 0L
    )

    init {
        orchestratorScope.launch {
            val cachedCurrentTime = sharedPreferencesUseCase.getLongValue(CURRENT_TIME_KEY)
            initialize(cachedCurrentTime)
        }
    }

    fun startLiveTimeUpdate() {
        orchestratorScope.launch {
            val currentLiveTime = timeManager.getGmtTime()
            updateLiveTime(currentLiveTime)

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
        startLiveTimeUpdate()

        val currentLiveTime = getGmtTime()
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

    suspend fun getGmtTime(): Long {
        return timeManager.getGmtTime()
    }
}