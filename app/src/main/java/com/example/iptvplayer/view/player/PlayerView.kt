package com.example.iptvplayer.view.player

import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.iptvplayer.view.channels.ChannelsViewModel
import com.example.iptvplayer.view.channelsAndEpgRow.ArchiveViewModel
import com.example.iptvplayer.view.errors.ErrorViewModel
import com.example.iptvplayer.view.player.playerOverlays.ExitConfirmationData
import com.example.iptvplayer.view.player.playerOverlays.PlayerOverlay
import com.example.iptvplayer.view.player.playerOverlays.PlayerOverlayState
import com.example.iptvplayer.view.player.playerOverlays.StreamRewindData

@Composable
fun PlayerView(
    isBackPressed: Boolean,
    stayInsideApp: () -> Unit,
    exitApp: () -> Unit
) {
    val mediaViewModel: MediaViewModel = hiltViewModel()
    val archiveViewModel: ArchiveViewModel = hiltViewModel()
    val channelsViewModel: ChannelsViewModel = hiltViewModel()
    val errorViewModel: ErrorViewModel = hiltViewModel()

    val isDataSourceSet by mediaViewModel.isDataSourceSet.collectAsState()
    val isPlaybackStarted by mediaViewModel.isPlaybackStarted.collectAsState()
    val isSeeking by mediaViewModel.isSeeking.collectAsState()

    val archiveSegmentUrl by archiveViewModel.archiveSegmentUrl.collectAsState()

    val currentChannel by channelsViewModel.currentChannel.collectAsState()
    val currentTime by mediaViewModel.currentTime.collectAsState()

    val currentError by errorViewModel.currentError.collectAsState()

    var surfaceHolder by remember { mutableStateOf<SurfaceHolder?>(null) }
    var playerOverlayState by remember { mutableStateOf(PlayerOverlayState.SHOW_LOADING_PROGRESS_BAR) }
    var isPlayerOverlayDisplayed by remember { mutableStateOf(true) }

    LaunchedEffect(archiveSegmentUrl) {
        if (archiveSegmentUrl.isNotEmpty()) {
            Log.i("archive segment url", archiveSegmentUrl)
            val resetEmittedTsSegments = !mediaViewModel.isDataSourceSet.value
            mediaViewModel.startTsCollectingJob(archiveSegmentUrl, resetEmittedTsSegments)
        }
    }

    LaunchedEffect(isBackPressed) {
        isPlayerOverlayDisplayed = isBackPressed
        if (isBackPressed) playerOverlayState = PlayerOverlayState.SHOW_EXIT_CONFIRMATION
    }

    LaunchedEffect(isPlaybackStarted, currentError, isSeeking) {
        Log.i("current error collected", "$currentError")
        Log.i("is playback started", isPlaybackStarted.toString())
        Log.i("is seeking", isSeeking.toString())

        if (isPlaybackStarted) {
            if (!isSeeking) {
                isPlayerOverlayDisplayed = false
                errorViewModel.resetError()
                return@LaunchedEffect
            }
        }

        isPlayerOverlayDisplayed = true
        if (currentError.errorTitle.isEmpty()) {
            if (isSeeking) {
                playerOverlayState = PlayerOverlayState.SHOW_STREAM_REWIND_FRAME
            } else {
                playerOverlayState = PlayerOverlayState.SHOW_LOADING_PROGRESS_BAR
            }
        } else {
            playerOverlayState = PlayerOverlayState.SHOW_ERROR
        }
    }


    Box(
        modifier = Modifier.fillMaxSize()
    ) {

       if (isPlayerOverlayDisplayed) {
           PlayerOverlay(
               playerOverlayState,
               ExitConfirmationData(stayInsideApp, exitApp),
               StreamRewindData(currentChannel.name, currentTime),
               currentError
           )
       }

        Log.i("player recomposed", "is data source set ${isDataSourceSet} surface holder ${surfaceHolder}")

        AndroidView(
            factory = { context ->
                SurfaceView(context).apply {
                    holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: SurfaceHolder) {
                            surfaceHolder = holder
                        }

                        override fun surfaceChanged(
                            holder: SurfaceHolder, format: Int,
                            width: Int, height: Int
                        ) {
                            Log.i("SURFACE SIZE", "$format $width $height")
                        }

                        override fun surfaceDestroyed(holder: SurfaceHolder) {
                            surfaceHolder = null
                            Log.i("is surface destroyed", "yes")
                        }
                    })
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = {
                if (isDataSourceSet && surfaceHolder != null) {
                    Log.i("set display?", "set")
                    mediaViewModel.ijkPlayer?.setDisplay(surfaceHolder)
                }
            }
        )
    }
}