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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
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
import com.example.iptvplayer.data.Epg
import com.example.iptvplayer.data.Utils
import com.example.iptvplayer.data.Utils.formatDate
import com.example.iptvplayer.view.channels.ArchiveViewModel
import com.example.iptvplayer.view.epg.EpgViewModel
import java.util.Locale
import kotlin.math.abs

@Composable
fun LinearProgressWithDot(
    modifier: Modifier,
    channelUrl: String,
    updateCurrentDate: (String) -> Unit
) {
    val decimalFormat = DecimalFormat("#.##", DecimalFormatSymbols(Locale.US))
    val datePattern = "EEEE d MMMM HH:mm:ss"

    val archiveViewModel: ArchiveViewModel = hiltViewModel()
    val epgViewModel: EpgViewModel = hiltViewModel()

    val seekSecondsFlow = archiveViewModel.seekSecondsFlow
    val currentTime by archiveViewModel.currentTime.observeAsState()
    val dvrRange by archiveViewModel.dvrRange.observeAsState()

    val focusedEpg by epgViewModel.focusedEpg.observeAsState()
    val focusedEpgIndex by epgViewModel.focusedEpgIndex.observeAsState()

    var progressBarWidthPx by remember { mutableIntStateOf(0) }
    var currentProgrammeProgress by remember { mutableFloatStateOf(0f) }

    val updateEpgSeekbarProgress: (Long, Epg, Int) -> Unit = { newTime, epg, epgIndex ->
        // checking if new time is within dvr range
        Log.i("NEW TIME", "${Utils.formatDate(newTime, datePattern)}")

        Log.i("REALLY", "UPDATE PROGRAM PROCESS ${focusedEpg.toString()}")

        val timeElapsedSinceProgrammeStart = newTime - epg.startTime
        Log.i("ELAPSED", timeElapsedSinceProgrammeStart.toString())
        Log.i("ELAPSED", focusedEpg.toString())
        Log.i("ELAPSED", newTime.toString())

        if (newTime < epg.startTime) {
            epgViewModel.updateFocusedEpgIndex(epgIndex - 1)
        } else if (newTime > epg.stopTime) {
            epgViewModel.updateFocusedEpgIndex(epgIndex + 1)
        } else {
            currentProgrammeProgress =
                decimalFormat.format(
                    timeElapsedSinceProgrammeStart.toFloat() * 100 / epg.duration
                ).toFloat()

        }

        Log.i(
            "CURRENT TIME IN FUNCTIONS",
            "${formatDate(newTime, datePattern)} $timeElapsedSinceProgrammeStart"
        )
        Log.i("PERCENT", currentProgrammeProgress.toString())
    }

    val updateDvrSeekbarProgress: (Long, Pair<Long, Long>) -> Unit = { newTime, dvrRange ->
        val timeElapsedSinceDvrStart = newTime - dvrRange.first
        val dvrDuration = dvrRange.second - dvrRange.first

        currentProgrammeProgress =
            decimalFormat.format(
                timeElapsedSinceDvrStart.toFloat() * 100 / dvrDuration
            ).toFloat()
    }

    val getNewTime: (Int) -> Long = { seek ->
        currentTime?.let { time ->
            val currentSeek = if (abs(seek) >= 60) {
                if (seek < 0) -60
                else 60
            } else {
                seek
            }


            val newTime = time + currentSeek
            newTime
        } ?: 0
    }

    LaunchedEffect(currentTime) {
        currentTime?.let { currentTime ->
            val localFocusedEpg = focusedEpg
            val localFocusedEpgIndex = focusedEpgIndex
            val localDvrRange = dvrRange

            // epg is available, use epg timestamps
            if (localFocusedEpg != null && localFocusedEpgIndex != null && localFocusedEpgIndex != -1) {
                updateEpgSeekbarProgress(currentTime, localFocusedEpg, localFocusedEpgIndex)
            // epg is not available, but dvr is available, use dvr timestamps
            } else if (localDvrRange != null && localDvrRange.first != 0L) {
                updateDvrSeekbarProgress(currentTime, localDvrRange)
            }
        }
    }

    LaunchedEffect(Unit) {
        seekSecondsFlow.collect { seek ->
            Log.i("collected seekSeconds", seek.toString())

            if (channelUrl.isNotEmpty()) {
                if (seek != 0) {
                    val newTime = getNewTime(seek)
                    Log.i("get new time", Utils.formatDate(newTime, datePattern))

                    val isAccessible = archiveViewModel.isStreamWithinDvrRange(newTime)
                    Log.i("is accessible", isAccessible.toString())
                    if (isAccessible) {
                        archiveViewModel.updateIsLive(false)
                        archiveViewModel.setCurrentTime(newTime)
                        updateCurrentDate(formatDate(newTime, datePattern))
                    }
                } else {
                    archiveViewModel.getArchiveUrl(channelUrl)
                    archiveViewModel.updateIsSeeking(false)
                }
            }
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