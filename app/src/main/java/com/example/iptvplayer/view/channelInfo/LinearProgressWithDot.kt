package com.example.iptvplayer.view.channelInfo

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
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.iptvplayer.data.Utils
import com.example.iptvplayer.data.Utils.formatDate
import com.example.iptvplayer.retrofit.data.Epg
import com.example.iptvplayer.view.channels.ArchiveViewModel
import com.example.iptvplayer.view.channels.MediaViewModel
import com.example.iptvplayer.view.epg.EpgViewModel
import java.util.Locale
import kotlin.math.abs

@Composable
fun LinearProgressWithDot(
    modifier: Modifier,
    channelUrl: String
) {
    val decimalFormat = DecimalFormat("#.##", DecimalFormatSymbols(Locale.US))
    val datePattern = "EEEE d MMMM HH:mm:ss"

    val archiveViewModel: ArchiveViewModel = hiltViewModel()
    val epgViewModel: EpgViewModel = hiltViewModel()
    val mediaViewModel: MediaViewModel = hiltViewModel()

    val seekSeconds by mediaViewModel.seekSecondsFlow.collectAsState(0)
    val currentTime by mediaViewModel.currentTime.collectAsState()
    val dvrRange by archiveViewModel.dvrRange.collectAsState()

    val currentEpg by epgViewModel.currentEpg.collectAsState()
    val currentEpgIndex by epgViewModel.currentEpgIndex.collectAsState()

    var progressBarWidthPx by remember { mutableIntStateOf(0) }
    var currentProgrammeProgress by remember { mutableFloatStateOf(0f) }

    val updateEpgSeekbarProgress: () -> Unit = {
        // checking if new time is within dvr range
        Log.i("NEW TIME", "${Utils.formatDate(currentTime, datePattern)}")

        Log.i("REALLY", "UPDATE PROGRAM PROCESS ${currentEpg.toString()}")
        Log.i("epg start seconds", currentEpg.epgVideoTimeRangeSeconds.start.toString())

        val timeElapsedSinceProgrammeStart = currentTime - currentEpg.epgVideoTimeRangeSeconds.start
        Log.i("ELAPSED", timeElapsedSinceProgrammeStart.toString())
        Log.i("ELAPSED", currentEpg.toString())
        Log.i("ELAPSED", currentTime.toString())

        if (currentTime < currentEpg.epgVideoTimeRangeSeconds.start) {
            epgViewModel.updateEpgIndex(currentEpgIndex - 1, true)
            epgViewModel.updateEpgIndex(currentEpgIndex - 1, false)
        } else if (currentTime > currentEpg.epgVideoTimeRangeSeconds.stop) {
            epgViewModel.updateEpgIndex(currentEpgIndex + 1, true)
            epgViewModel.updateEpgIndex(currentEpgIndex + 1, false)
        } else {
            currentProgrammeProgress =
                decimalFormat.format(
                    timeElapsedSinceProgrammeStart.toFloat() * 100 / (currentEpg.length * 60)
                ).toFloat()

        }

        Log.i(
            "CURRENT TIME IN FUNCTIONS",
            "${formatDate(currentTime, datePattern)} $timeElapsedSinceProgrammeStart"
        )
        Log.i("PERCENT", currentProgrammeProgress.toString())
    }

    val updateDvrSeekbarProgress: () -> Unit = {
        val timeElapsedSinceDvrStart = currentTime - dvrRange.first
        val dvrDuration = dvrRange.second - dvrRange.first

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
        if (channelUrl.isNotEmpty()) {
            if (seekSeconds != 0) {
                val newTime = getNewTime(seekSeconds)
                Log.i("get new time", Utils.formatDate(newTime, datePattern))

                val isAccessible = archiveViewModel.isStreamWithinDvrRange(newTime)
                Log.i("is accessible", isAccessible.toString())
                if (isAccessible) {
                    mediaViewModel.updateIsSeeking(true)
                    mediaViewModel.updateIsLive(false)
                    mediaViewModel.setCurrentTime(newTime)
                } else {
                    if (seekSeconds < 0) {
                        archiveViewModel.setRewindError("Cannot rewind back")
                    } else {
                        archiveViewModel.setRewindError("Cannot rewind forward")
                    }
                }
            } else {
                Log.i("is seeking ${mediaViewModel.isSeeking.value}", "real")
                if (mediaViewModel.isSeeking.value) {
                    archiveViewModel.getArchiveUrl(channelUrl, currentTime)
                    mediaViewModel.updateIsSeeking(false)
                }
            }
        }
    }

    LaunchedEffect(currentTime) {
        // epg is available, use epg timestamps
        if (currentEpg != Epg() && currentEpgIndex != -1) {
            Log.i("DVR OR EPG", "epg")
            updateEpgSeekbarProgress()
            // epg is not available, but dvr is available, use dvr timestamps
        } else if (dvrRange.first > 0) {
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