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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.iptvplayer.domain.media.StreamTypeState
import com.example.iptvplayer.retrofit.data.ChannelData
import com.example.iptvplayer.view.archive.ArchiveViewModel
import com.example.iptvplayer.view.archive.CurrentDvrInfoState
import com.example.iptvplayer.view.channels.ChannelsViewModel
import com.example.iptvplayer.view.errors.ErrorViewModel
import com.example.iptvplayer.view.media.MediaPlaybackViewModel
import com.example.iptvplayer.view.media.MediaViewModel
import com.example.iptvplayer.view.player.playerOverlays.ExitConfirmationData
import com.example.iptvplayer.view.player.playerOverlays.PlayerOverlay
import com.example.iptvplayer.view.player.playerOverlays.PlayerOverlayState
import com.example.iptvplayer.view.player.playerOverlays.StreamRewindData
import com.example.iptvplayer.view.time.DateAndTimeViewModel
import java.util.TimeZone
import java.util.regex.Pattern

@Composable
fun PlayerView(
    isBackPressed: Boolean,
    stayInsideApp: () -> Unit,
    exitApp: () -> Unit
) {
    val context = LocalContext.current
    val mediaViewModel: MediaViewModel = hiltViewModel()
    val mediaPlaybackViewModel: MediaPlaybackViewModel = hiltViewModel()
    val archiveViewModel: ArchiveViewModel = hiltViewModel()
    val channelsViewModel: ChannelsViewModel = hiltViewModel()
    val errorViewModel: ErrorViewModel = hiltViewModel()
    val dateAndTimeViewModel: DateAndTimeViewModel = hiltViewModel()

    val isDataSourceSet by mediaViewModel.isDataSourceSet.collectAsState()
    val isPlaybackStarted by mediaViewModel.isPlaybackStarted.collectAsState()
    val isSeeking by mediaViewModel.isSeeking.collectAsState()
    val newSegmentsNeeded by mediaViewModel.newSegmentsNeeded.collectAsState()
    val streamType by mediaPlaybackViewModel.streamType.collectAsState()
    val isPaused by mediaPlaybackViewModel.isPaused.collectAsState()

    val archiveSegmentUrl by archiveViewModel.archiveSegmentUrl.collectAsState()
    val currentDvrInfoState by archiveViewModel.currentChannelDvrInfoState.collectAsState()
    val currentDvrRange by archiveViewModel.currentChannelDvrRange.collectAsState()

    val currentChannel by channelsViewModel.currentChannel.collectAsState()
    val currentTime by dateAndTimeViewModel.currentTime.collectAsState()

    val currentError by errorViewModel.currentError.collectAsState()

    var surfaceHolder by remember { mutableStateOf<SurfaceHolder?>(null) }
    var playerOverlayState by remember { mutableStateOf(PlayerOverlayState.SHOW_LOADING_PROGRESS_BAR) }
    var isPlayerOverlayDisplayed by remember { mutableStateOf(true) }

    LaunchedEffect(isBackPressed) {
        isPlayerOverlayDisplayed = isBackPressed
        if (isBackPressed) playerOverlayState = PlayerOverlayState.SHOW_EXIT_CONFIRMATION
    }

    LaunchedEffect(isDataSourceSet) {
        Log.i("is data source set", isDataSourceSet.toString())
    }

    LaunchedEffect(isPlayerOverlayDisplayed) {
        Log.i("is player overlay displayed", isPlayerOverlayDisplayed.toString())
    }

    LaunchedEffect(isPlaybackStarted, currentError, isSeeking) {
        Log.i("current error collected", "$currentError")
        Log.i("is playback started", isPlaybackStarted.toString())
        Log.i("is seeking", isSeeking.toString())

        if (isPlaybackStarted && currentDvrInfoState != CurrentDvrInfoState.GAP_DETECTED_AND_WAITING) {
            Log.i("is playback started! if", "true")
            if (!isSeeking) {
                Log.i("is seeking! if", "false")
                surfaceHolder?.surface?.let { surface ->
                    mediaPlaybackViewModel.setPlayerSurface(surface)
                }
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

    val getLastSegmentUrlEndTime: (String) -> Long = { url ->
        Log.i("last segment from queue", url.toString())
        val regex = "\\d{4}/\\d{2}/\\d{2}/\\d{2}/\\d{2}/\\d{2}-\\d{5}"
        val pattern = Pattern.compile(regex)
        val matcher = pattern.matcher(url)

        val datePattern = "yyyy/MM/d/HH/mm/ss"

        if (matcher.find()) {
            val matchedString = matcher.group(0)

            matchedString?.let { string ->
                var segmentStartTime = dateAndTimeViewModel.parseDate(string.substring(0, string.length-6), datePattern, TimeZone.getTimeZone("UTC"))
                val segmentLength = string.substring(string.length-5, string.length - 3)
                Log.i("segment length", segmentLength)
                segmentStartTime += if (segmentLength.startsWith("0")) {
                    segmentLength[1].toString().toInt()
                } else {
                    segmentLength.substring(0, 2).toInt()
                }

                segmentStartTime
            } ?: 0
        } else {
            0
        }
    }

    LaunchedEffect(newSegmentsNeeded, streamType, currentChannel, isPaused, currentDvrRange) {
        if (newSegmentsNeeded && streamType == StreamTypeState.ARCHIVE && currentChannel != ChannelData() && !isPaused && currentDvrRange != -1) {
            println("new segments needed")
            val lastSegment = mediaPlaybackViewModel.getLastSegmentFromQueue()
            val nextSegmentsStartTime = getLastSegmentUrlEndTime(lastSegment)
            println("last segment $lastSegment")

            Log.i("next segments start time", nextSegmentsStartTime.toString())

            if (lastSegment.isEmpty()) {
                archiveViewModel.getArchiveUrl(currentChannel.channelUrl, dateAndTimeViewModel.currentTime.value)
            } else {
                archiveViewModel.getArchiveUrl(currentChannel.channelUrl, nextSegmentsStartTime)
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

      if (isPlayerOverlayDisplayed) {
           Log.i("is overlay displayed", isPlayerOverlayDisplayed.toString())
           PlayerOverlay(
               playerOverlayState,
               ExitConfirmationData(stayInsideApp, exitApp),
               StreamRewindData(currentChannel.name, currentTime),
               currentError
           )
       } else {
           Log.i("is overlay displayed", isPlayerOverlayDisplayed.toString())
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
                            surfaceHolder = holder
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

            }
        )
    }
}