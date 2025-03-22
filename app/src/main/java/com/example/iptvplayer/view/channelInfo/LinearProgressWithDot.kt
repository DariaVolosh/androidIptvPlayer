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
import androidx.compose.ui.platform.LocalDensity
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.iptvplayer.data.Utils.formatDate
import com.example.iptvplayer.view.channels.ArchiveViewModel
import com.example.iptvplayer.view.epg.EpgViewModel
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.abs

@Composable
fun LinearProgressWithDot(
    modifier: Modifier,
    seekingStarted: Boolean,
    isPaused: Boolean,
    setProgressBarXOffset: (Int) -> Unit,
    setDotXOffset: (Int) -> Unit,
    updateIsLiveProgramme: (Boolean) -> Unit,
    updateCurrentDate: (String) -> Unit
) {
    val decimalFormat = DecimalFormat("#.##", DecimalFormatSymbols(Locale.US))
    val datePattern = "EEEE d MMMM HH:mm:ss"

    val archiveViewModel: ArchiveViewModel = hiltViewModel()
    val epgViewModel: EpgViewModel = hiltViewModel()

    val seekSeconds by archiveViewModel.seekSeconds.observeAsState()
    val currentTime by archiveViewModel.currentTime.observeAsState()

    val focusedEpg by epgViewModel.focusedEpg.observeAsState()
    val focusedEpgIndex by epgViewModel.focusedEpgIndex.observeAsState()

    var progressBarWidthPx by remember { mutableIntStateOf(0) }
    var currentProgrammeProgress by remember { mutableFloatStateOf(0f) }

    val localDensity = LocalDensity.current.density

    val updateProgrammeProgress = {
        var currentSeek = 0

        currentTime?.let { time ->
            seekSeconds?.let { seek ->
                if (seekingStarted) {
                    currentSeek = if (abs(seek) >= 64) {
                        if (seek < 0) -64
                        else 64
                    } else {
                        seek / 2
                    }

                    archiveViewModel.setCurrentTime(time + currentSeek)
                }
            }

            Log.i("seek channel info", currentSeek.toString())
            Log.i("seek channel info", formatDate(time, datePattern))
            Log.i("REALLY", "UPDATE PROGRAM PROCESS ${focusedEpg.toString()}")

            val localFocusedEpg = focusedEpg
            val localFocusedEpgIndex = focusedEpgIndex

            if (localFocusedEpg != null && localFocusedEpgIndex != null) {
                val timeElapsedSinceProgrammeStart = time - localFocusedEpg.startTime
                Log.i("ELAPSED", timeElapsedSinceProgrammeStart.toString())
                Log.i("ELAPSED", focusedEpg.toString())
                Log.i("ELAPSED", time.toString())

                if (time < localFocusedEpg.startTime) {
                    epgViewModel.updateFocusedEpgIndex(localFocusedEpgIndex - 1)
                } else if (time > localFocusedEpg.stopTime) {
                    epgViewModel.updateFocusedEpgIndex(localFocusedEpgIndex + 1)
                } else {
                    currentProgrammeProgress =
                        decimalFormat.format(
                            timeElapsedSinceProgrammeStart.toFloat() * 100 / localFocusedEpg.duration
                        ).toFloat()

                }

                Log.i("CURRENT TIME IN FUNCTIONS", "${formatDate(time, datePattern)} $timeElapsedSinceProgrammeStart")
                Log.i("PERCENT", currentProgrammeProgress.toString())
            }
        }
    }

    LaunchedEffect(seekingStarted, isPaused, focusedEpg) {
        val progressUpdatePeriod = 10 // in seconds
        var secondsPassed = 0

        while (!seekingStarted && !isPaused) {
            if (secondsPassed == 0) updateProgrammeProgress()
            delay(1000)

            // converting milliseconds to seconds
            archiveViewModel.setCurrentTime(currentTime?.plus(1) ?: 0)
            secondsPassed += 1

            if (secondsPassed == progressUpdatePeriod) secondsPassed = 0
            updateCurrentDate(formatDate(currentTime ?: 0, datePattern))
        }
    }

    LaunchedEffect(seekSeconds, focusedEpg) {
        seekSeconds?.let { seek ->
            if (seek != 0) {
                updateIsLiveProgramme(false)
                updateProgrammeProgress()
                updateCurrentDate(formatDate(currentTime ?: 0, datePattern))
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
                    setProgressBarXOffset((xOffset / localDensity).toInt())
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
            setDotXOffset((progressPosition / density).toInt())

            drawCircle(
                color = Color.Gray,
                radius = 10f,
                center = Offset(progressPosition, size.height / 2)
            )
        }
    }
}