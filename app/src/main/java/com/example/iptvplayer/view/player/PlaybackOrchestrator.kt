package com.example.iptvplayer.view.player

import android.os.Looper
import android.util.Log
import com.example.iptvplayer.R
import com.example.iptvplayer.domain.media.GetTsSegmentsUseCase
import com.example.iptvplayer.view.errors.ErrorData
import com.example.iptvplayer.view.errors.ErrorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackOrchestrator @Inject constructor(
    private val mediaManager: MediaManager,
    private val errorManager: ErrorManager,
    private val getTsSegmentsUseCase: GetTsSegmentsUseCase
) {
    private val orchestratorScope = CoroutineScope(SupervisorJob() + Dispatchers.IO) // Own scope

    private val urlQueue = mediaManager.getUrlQueue()
    private var tsJob: Job? = null

    val isLive: StateFlow<Boolean> = mediaManager.isLive.stateIn(
        orchestratorScope, SharingStarted.WhileSubscribed(5000), true
    )

    val isReset: StateFlow<Boolean> = mediaManager.isReset.stateIn(
        orchestratorScope, SharingStarted.Eagerly, true
    )

    val isFirstSegmentRead: StateFlow<Boolean> = mediaManager.isFirstSegmentRead.stateIn(
        orchestratorScope, SharingStarted.Eagerly, false
    )

    fun startTsCollectingJob(url: String) {
        Log.i("start ts!!!", url)
        tsJob = orchestratorScope.launch {
            withContext(Dispatchers.IO) {
                val isOnMainThread = Looper.getMainLooper() == Looper.myLooper()
                Log.i("is on main thread channels list", "set media url $isOnMainThread")
                getTsSegmentsUseCase.extractTsSegments(
                    url,
                    isLive.value
                ) { errorTitle, errorDescription ->
                    errorManager.publishError(
                        ErrorData(errorTitle, errorDescription, R.drawable.error_icon)
                    )
                }.collect { u ->
                    mediaManager.updateLastSegmentFromQueue(u)
                    if (!isFirstSegmentRead.value) {
                        mediaManager.setNextUrl(u)
                    } else {
                        urlQueue.add(u)
                    }
                    /*Log.i("YEA SET PLAYER!", "${mediaManager.ijkPlayer.toString()} just collect url $url")
                    Log.i("collected segment", "$u")
                    if (!mediaManager.isPlayerInstanceAvailable() || isReset.value) {
                        Log.i("YEA SET PLAYER!", "${mediaManager.ijkPlayer.toString()} just CHECK")
                        Log.i("is player reset", isReset.value.toString())
                        mediaManager.setIsPlayerReset(false)
                        mediaManager.setOnPreparedListener()
                        mediaManager.setOnInfoListener()
                        mediaManager.setOnBufferingUpdateListener()
                        mediaManager.setDataSource()

                        IjkMediaPlayer.native_setLogLevel(IjkMediaPlayer.IJK_LOG_DEBUG)
                        mediaManager.setOnSegmentRequestCallback()
                        mediaManager.setNextUrl(u)
                    } else {
                        Log.i("YEA SET PLAYER!", mediaManager.ijkPlayer.toString())
                        mediaManager.updateLastSegmentFromQueue(u)
                        urlQueue.add(u)

                        Log.i("url added to queue", u)
                        Log.i("ijk player!!! instance", mediaManager.ijkPlayer.toString())
                    } */
                }
            }
        }
    }

    fun cancelTsCollectingJob() {
        tsJob?.cancel()
    }
}