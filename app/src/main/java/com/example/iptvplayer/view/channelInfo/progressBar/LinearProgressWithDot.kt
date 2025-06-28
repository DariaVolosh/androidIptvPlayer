package com.example.iptvplayer.view.channelInfo.progressBar

import android.icu.text.DecimalFormat
import android.icu.text.DecimalFormatSymbols
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.iptvplayer.R
import com.example.iptvplayer.view.channelsAndEpgRow.ArchiveViewModel
import com.example.iptvplayer.view.channelsAndEpgRow.CurrentDvrInfoState
import com.example.iptvplayer.view.epg.EpgViewModel
import com.example.iptvplayer.view.media.MediaPlaybackViewModel
import com.example.iptvplayer.view.media.MediaViewModel
import com.example.iptvplayer.view.time.DateAndTimeViewModel
import java.util.Locale
import kotlin.math.abs

@Composable
fun LinearProgressWithDot(
    modifier: Modifier,
    channelUrl: String
) {
    val decimalFormat = DecimalFormat("#.##", DecimalFormatSymbols(Locale.US))
    val datePattern = "EEEE d MMMM HH:mm:ss"
    val localContext = LocalContext.current

    val archiveViewModel: ArchiveViewModel = hiltViewModel()
    val epgViewModel: EpgViewModel = hiltViewModel()
    val mediaViewModel: MediaViewModel = hiltViewModel()
    val dateAndTimeViewModel: DateAndTimeViewModel = hiltViewModel()
    val mediaPlaybackViewModel: MediaPlaybackViewModel = hiltViewModel()

    val seekSeconds by mediaViewModel.seekSecondsFlow.collectAsState(0)
    val currentTime by dateAndTimeViewModel.currentTime.collectAsState()
    val isSeeking by mediaViewModel.isSeeking.collectAsState()

    val dvrRanges by archiveViewModel.currentChannelDvrRanges.collectAsState()
    val currentDvrInfoState by archiveViewModel.currentChannelDvrInfoState.collectAsState()

    val currentEpg by epgViewModel.currentEpg.collectAsState()
    val currentEpgIndex by epgViewModel.currentEpgIndex.collectAsState()

    var progressBarWidthPx by remember { mutableIntStateOf(0) }
    var currentProgrammeProgress by remember { mutableFloatStateOf(0f) }

    val updateEpgSeekbarProgress: () -> Unit = {
        // checking if new time is within dvr range

        Log.i("REALLY", "UPDATE PROGRAM PROCESS ${currentEpg.toString()}")
        Log.i("epg start seconds", currentEpg.epgVideoTimeRangeSeconds.start.toString())

        val timeElapsedSinceProgrammeStart = currentTime - currentEpg.epgVideoTimeRangeSeconds.start
        Log.i("ELAPSED", currentEpg.toString())
        Log.i("ELAPSED", currentTime.toString())

        if (currentTime < currentEpg.epgVideoTimeRangeSeconds.start) {
            val prevEpgIndex = epgViewModel.findFirstEpgIndexBackward(currentEpgIndex - 1)
            epgViewModel.updateEpgIndex(prevEpgIndex, true)
            epgViewModel.updateEpgIndex(prevEpgIndex, false)
        } else if (currentTime > currentEpg.epgVideoTimeRangeSeconds.stop) {
            val nextEpgIndex = epgViewModel.findFirstEpgIndexForward(currentEpgIndex + 1)
            epgViewModel.updateEpgIndex(nextEpgIndex, true)
            epgViewModel.updateEpgIndex(nextEpgIndex, false)
        } else {
            currentProgrammeProgress =
                decimalFormat.format(
                    timeElapsedSinceProgrammeStart.toFloat() * 100 / (currentEpg.length * 60)
                ).toFloat()

        }

        Log.i("PERCENT", currentProgrammeProgress.toString())
    }

    val updateDvrSeekbarProgress: () -> Unit = {
        val firstDvrRange = dvrRanges[0]
        val lastDvrRange = dvrRanges[dvrRanges.size-1]
        val timeElapsedSinceDvrStart = currentTime - firstDvrRange.from
        val dvrDuration = (lastDvrRange.from + lastDvrRange.duration) - firstDvrRange.from

        Log.i("dvr info in update seekbar: ", "$firstDvrRange $lastDvrRange $currentTime $timeElapsedSinceDvrStart $dvrDuration")

        currentProgrammeProgress =
            decimalFormat.format(
                timeElapsedSinceDvrStart.toFloat() * 100 / dvrDuration
            ).toFloat()

        Log.i("current dvr progress", currentProgrammeProgress.toString())
    }

    val getNewTime: (Int) -> Long = { seek ->
        val currentSeek = if (abs(seek) >= 60) {
            if (seek < 0) -60
            else 60
        } else {
            seek
        }

        val newTime = currentTime + currentSeek
        newTime
    }

    LaunchedEffect(seekSeconds) {
        Log.i("collected seekSeconds", seekSeconds.toString())
        if (channelUrl.isNotEmpty()) {
            if (seekSeconds != 0) {
                val newTime = getNewTime(seekSeconds)

                val isAccessible = archiveViewModel.isStreamWithinDvrRange(newTime)
                Log.i("is accessible", isAccessible.toString())
                if (isAccessible) {
                    mediaViewModel.updateIsSeeking(true)
                    mediaViewModel.updateIsLive(false)
                    mediaViewModel.updateCurrentTime(newTime)
                } else {
                    if (seekSeconds < 0) {
                        archiveViewModel.setRewindError(localContext.getString(R.string.cannot_rewind_back))
                    } else {
                        archiveViewModel.setRewindError(localContext.getString(R.string.cannot_rewind_forward))
                    }
                }
            } else {
                Log.i("is seeking ${mediaViewModel.isSeeking.value}", "real")
                if (isSeeking) {
                    archiveViewModel.getArchiveUrl(channelUrl, currentTime)
                    mediaViewModel.updateIsSeeking(false)
                    mediaPlaybackViewModel.startArchivePlayback()
                }
            }
        }
    }

    LaunchedEffect(currentTime) {
        archiveViewModel.determineCurrentDvrRange(true, currentTime)
        Log.i("currentEpgIndex", currentEpgIndex.toString())
        // epg is available, use epg timestamps
        if (currentEpgIndex != -1) {
            Log.i("DVR OR EPG", "epg")
            updateEpgSeekbarProgress()
            // epg is not available, but dvr is available, use dvr timestamps
        } else if (currentDvrInfoState != CurrentDvrInfoState.LOADING && currentDvrInfoState != CurrentDvrInfoState.NOT_AVAILABLE_GLOBAL) {
            Log.i("DVR OR EPG", "dvr")
            updateDvrSeekbarProgress()
        }
    }

    Box(
        modifier = modifier,
    ) {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .onGloballyPositioned { coordinates ->
                    val xOffset = coordinates.positionInRoot().x
                    Log.i("progress bar offset", xOffset.toString())
                    progressBarWidthPx = coordinates.size.width
                },
            progress = {(currentProgrammeProgress / 100)},
            color = MaterialTheme.colorScheme.onSecondary
        )

        Canvas(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
        ) {
            val progressPosition = progressBarWidthPx * (currentProgrammeProgress / 100)

            drawCircle(
                color = Color.Gray,
                radius = 10f,
                center = Offset(progressPosition, size.height / 2)
            )
        }
    }
}