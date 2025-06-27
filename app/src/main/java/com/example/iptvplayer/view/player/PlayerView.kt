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
import com.example.iptvplayer.R
import com.example.iptvplayer.retrofit.data.ChannelData
import com.example.iptvplayer.view.channels.ChannelsViewModel
import com.example.iptvplayer.view.channelsAndEpgRow.ArchiveViewModel
import com.example.iptvplayer.view.channelsAndEpgRow.CurrentDvrInfoState
import com.example.iptvplayer.view.errors.ErrorData
import com.example.iptvplayer.view.errors.ErrorViewModel
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
    val archiveViewModel: ArchiveViewModel = hiltViewModel()
    val channelsViewModel: ChannelsViewModel = hiltViewModel()
    val errorViewModel: ErrorViewModel = hiltViewModel()
    val dateAndTimeViewModel: DateAndTimeViewModel = hiltViewModel()

    val isDataSourceSet by mediaViewModel.isDataSourceSet.collectAsState()
    val isPlaybackStarted by mediaViewModel.isPlaybackStarted.collectAsState()
    val isSeeking by mediaViewModel.isSeeking.collectAsState()
    val newSegmentsNeeded by mediaViewModel.newSegmentsNeeded.collectAsState()
    val isLive by mediaViewModel.isLive.collectAsState()

    val archiveSegmentUrl by archiveViewModel.archiveSegmentUrl.collectAsState()
    val currentDvrInfoState by archiveViewModel.currentChannelDvrInfoState.collectAsState()

    val currentChannel by channelsViewModel.currentChannel.collectAsState()
    val currentTime by dateAndTimeViewModel.currentTime.collectAsState()

    val currentError by errorViewModel.currentError.collectAsState()

    var surfaceHolder by remember { mutableStateOf<SurfaceHolder?>(null) }
    var playerOverlayState by remember { mutableStateOf(PlayerOverlayState.SHOW_LOADING_PROGRESS_BAR) }
    var isPlayerOverlayDisplayed by remember { mutableStateOf(true) }

    LaunchedEffect(archiveSegmentUrl) {
        if (archiveSegmentUrl.isNotEmpty()) {
            Log.i("archive segment url", archiveSegmentUrl)
            mediaViewModel.startCollectingSegments(archiveSegmentUrl)
        }
    }

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

        if (isPlaybackStarted) {
            Log.i("is playback started! if", "true")
            if (!isSeeking) {
                Log.i("is seeking! if", "false")
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

    LaunchedEffect(newSegmentsNeeded, isLive, currentChannel) {
        if (newSegmentsNeeded && !isLive && currentChannel != ChannelData()) {
            val lastSegment = mediaViewModel.getLastSegmentFromQueue()
            val nextSegmentsStartTime = getLastSegmentUrlEndTime(lastSegment)

            Log.i("next segments start time", nextSegmentsStartTime.toString())

            if (lastSegment.isEmpty()) {
                //Log.i("new segments time", dateAndTimeViewModel.formatDate(nextSegmentsStartTime, "EEEE d MMMM HH:mm:ss"))
                archiveViewModel.getArchiveUrl(currentChannel.channelUrl, dateAndTimeViewModel.currentTime.value)
            } else {

                archiveViewModel.getArchiveUrl(currentChannel.channelUrl, nextSegmentsStartTime)
            }
        }
    }

    LaunchedEffect(currentDvrInfoState) {
        if (currentDvrInfoState == CurrentDvrInfoState.GAP_DETECTED_AND_WAITING) {
            errorViewModel.publishError(ErrorData(
                context.getString(R.string.no_archive),
                context.getString(R.string.no_archive_descr),
                R.drawable.error_icon
            ))
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
                            Log.i("surface created?", holder.toString())
                            mediaViewModel.ijkPlayer.value?.setSurface(holder.surface)
                            mediaViewModel.updateIsSurfaceAttached(true)
                        }

                        override fun surfaceChanged(
                            holder: SurfaceHolder, format: Int,
                            width: Int, height: Int
                        ) {
                            Log.i("SURFACE SIZE", "$format $width $height")
                            mediaViewModel.ijkPlayer.value?.setSurface(holder.surface)
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