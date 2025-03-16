package com.example.iptvplayer.view.channelInfo

import android.icu.text.DecimalFormat
import android.icu.text.DecimalFormatSymbols
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.example.iptvplayer.R
import com.example.iptvplayer.data.Utils
import com.example.iptvplayer.data.Utils.formatDate
import com.example.iptvplayer.view.channels.ArchiveViewModel
import com.example.iptvplayer.view.channels.ChannelsViewModel
import com.example.iptvplayer.view.channels.MediaViewModel
import com.example.iptvplayer.view.epg.EpgViewModel
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.abs

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun ChannelInfo(
    modifier: Modifier,
    isChannelInfoShown: Boolean,
    showProgrammeDatePicker: (Boolean) -> Unit,
    showChannelInfo: (Boolean) -> Unit
) {
    val archiveViewModel: ArchiveViewModel = hiltViewModel()
    val mediaViewModel: MediaViewModel = hiltViewModel()
    // injecting epgViewModel and channelsViewModel because i need to use 2 methods from them
    val epgViewModel: EpgViewModel = hiltViewModel()
    val channelsViewModel: ChannelsViewModel = hiltViewModel()

    val timePattern = "HH:mm"
    val datePattern = "EEEE d MMMM HH:mm:ss"

    val decimalFormat = DecimalFormat("#.##", DecimalFormatSymbols(Locale.US))

    // current programme progress in percents
    var currentProgrammeProgress by remember { mutableFloatStateOf(0f) }
    var currentFullDate by remember { mutableStateOf("") }

    var timeFetched by remember { mutableStateOf(false) }
    var seekingStarted by remember { mutableStateOf(false) }
    var secondsNotInteracted by remember { mutableIntStateOf(0) }
    var isLiveProgramme by remember { mutableStateOf(false) }

    val seekSeconds by archiveViewModel.seekSeconds.observeAsState()
    val currentTime by archiveViewModel.currentTime.observeAsState()
    val liveTime by archiveViewModel.liveTime.observeAsState()

    val focusedEpg by epgViewModel.focusedEpg.observeAsState()
    val focusedEpgIndex by epgViewModel.focusedEpgIndex.observeAsState()

    val focusedChannel by channelsViewModel.focusedChannel.observeAsState()
    val focusedChannelIndex by channelsViewModel.focusedChannelIndex.observeAsState()

    val isPaused by mediaViewModel.isPaused.observeAsState()

    Log.i("REALLY", "${focusedEpg} CHANNEL INFO")

    // epg closure from outside scope, that is why passed as parameter
    val updateProgrammeProgress = {
        var currentSeek = 0

        currentTime?.let { time ->
            seekSeconds?.let { seek ->
                if (seekingStarted) {
                    currentSeek = if (abs(seek) >= 256) {
                        if (seek < 0) -256
                        else 256
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

                Log.i("PROGRAMME", "${formatDate(time, datePattern)} $timeElapsedSinceProgrammeStart")
                Log.i("PERCENT", currentProgrammeProgress.toString())
            }
        }
    }

    LaunchedEffect(Unit) {
        archiveViewModel.setLiveTime(Utils.getGmtTime())
        timeFetched = true

        while (true) {
            liveTime?.let { time ->
                archiveViewModel.setLiveTime(time + 1)
            }

            delay(1000)
        }
    }

    LaunchedEffect(isChannelInfoShown) {
        Log.i("IS CHANNEL INFO SHOWN", focusedEpg.toString())
        Log.i("IS CHANNEL INFO SHOWN", isChannelInfoShown.toString())
        if (isChannelInfoShown) {
            secondsNotInteracted = 0

            while (secondsNotInteracted < 4) {
                secondsNotInteracted++
                delay(1000)
            }

            showChannelInfo(false)
        }
    }

    LaunchedEffect(seekingStarted, timeFetched, isPaused, focusedEpg) {
        val progressUpdatePeriod = 10 // in seconds
        var secondsPassed = 0

        while (!seekingStarted && timeFetched && isPaused == false) {
            if (secondsPassed == 0) updateProgrammeProgress()
            delay(1000)

            // converting milliseconds to seconds
            archiveViewModel.setCurrentTime(currentTime?.plus(1) ?: 0)
            secondsPassed += 1

            if (secondsPassed == progressUpdatePeriod) secondsPassed = 0
            currentFullDate = formatDate(currentTime ?: 0, datePattern)
            Log.i("current full date", currentFullDate)
        }
    }

    LaunchedEffect(seekSeconds, focusedEpg) {
        seekSeconds?.let { seek ->
            if (seek != 0) {
                isLiveProgramme = false
                updateProgrammeProgress()
                currentFullDate = formatDate(currentTime ?: 0, datePattern)
            }
        }
    }

    if (isChannelInfoShown) {
        Column (
            modifier = modifier
                .background(MaterialTheme.colorScheme.secondary.copy(0.8f))
                .padding(bottom = 20.dp, top = 10.dp)
                .padding(horizontal = 10.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.padding(end = 15.dp),
                    text = formatDate(focusedEpg?.startTime ?: 0, timePattern),
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onSecondary
                )

                LinearProgressWithDot(Modifier.weight(1f), currentProgrammeProgress)

                Text(
                    modifier = Modifier.padding(start = 15.dp),
                    text = formatDate(focusedEpg?.stopTime ?: 0, timePattern),
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onSecondary
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {

                Column(
                    modifier = Modifier
                        .padding(top = 17.dp)
                        .align(Alignment.TopStart)
                ) {
                    Row(

                    ) {
                        Text(
                            modifier = Modifier.padding(end = 7.dp),
                            text = "${focusedChannelIndex?.plus(1) ?: 1}.",
                            fontSize = 24.sp,
                            color = MaterialTheme.colorScheme.onSecondary
                        )

                        Text(
                            text = focusedChannel?.name ?: "",
                            fontSize = 24.sp,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    }

                    GlideImage(
                        model = focusedChannel?.logo ?: "",
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .size(50.dp),
                        contentScale = ContentScale.Fit,
                        contentDescription = stringResource(R.string.channel_logo)
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(0.6f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 25.dp)
                    ) {
                        Text(
                            modifier = Modifier.padding(end = 15.dp),
                            fontSize = 22.sp,
                            text = currentFullDate,
                            color = MaterialTheme.colorScheme.onSecondary
                        )

                        Text(
                            fontSize = 22.sp,
                            text = focusedEpg?.title ?: "",
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    }

                    Log.i("SHIT4", "RECOMPOSED")

                    focusedChannel?.let { focused ->
                        PlaybackControls(
                            focusedChannel?.url ?: "",
                            { started -> seekingStarted = started },
                            { secondsNotInteracted = 0 },
                            {isLive -> isLiveProgramme = isLive}
                        ) { showDatePicker -> showProgrammeDatePicker(showDatePicker) }
                    }
                }

                Row(
                    modifier = Modifier
                        .padding(top = 17.dp)
                        .align(Alignment.TopEnd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .padding(end = 7.dp)
                            .size(12.dp)
                            .background(Color.Red, shape = CircleShape)
                    )

                    Text(
                        text = if (isLiveProgramme) "live" else "record",
                        color = Color.Red,
                        fontSize = 24.sp
                    )
                }
            }
        }
    }
}