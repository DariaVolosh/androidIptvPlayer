package com.example.iptvplayer.view.channelInfo

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.iptvplayer.data.Utils.formatDate
import com.example.iptvplayer.view.channels.ArchiveViewModel
import com.example.iptvplayer.view.channels.ChannelsViewModel
import com.example.iptvplayer.view.epg.EpgViewModel
import kotlinx.coroutines.delay

@Composable
fun ChannelInfo(
    modifier: Modifier,
    isChannelInfoShown: Boolean,
    isPaused: Boolean,
    showProgrammeDatePicker: (Boolean) -> Unit,
    showChannelInfo: (Boolean) -> Unit
) {
    val archiveViewModel: ArchiveViewModel = hiltViewModel()
    // injecting epgViewModel and channelsViewModel because i need to use 2 methods from them
    val epgViewModel: EpgViewModel = hiltViewModel()
    val channelsViewModel: ChannelsViewModel = hiltViewModel()

    val timePattern = "HH:mm"
    val datePattern = "EEEE d MMMM HH:mm:ss"

    var currentFullDate by remember { mutableStateOf("") }

    var seekingStarted by remember { mutableStateOf(false) }
    var secondsNotInteracted by remember { mutableIntStateOf(0) }
    var isLiveProgramme by remember { mutableStateOf(false) }

    var progressBarXOffset by remember { mutableIntStateOf(0) }
    var dotXOffset by remember { mutableIntStateOf(0) }

    val currentTime by archiveViewModel.currentTime.observeAsState()

    val focusedEpg by epgViewModel.focusedEpg.observeAsState()

    val focusedChannel by channelsViewModel.focusedChannel.observeAsState()
    val focusedChannelIndex by channelsViewModel.focusedChannelIndex.observeAsState()

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

    LaunchedEffect(focusedEpg) {
        if (focusedEpg == null || focusedEpg?.isDvrAvailable == false) {
            val progressUpdatePeriod = 10 // in seconds
            var secondsPassed = 0

            while (!seekingStarted && !isPaused) {
                delay(1000)

                // converting milliseconds to seconds
                archiveViewModel.setCurrentTime(currentTime?.plus(1) ?: 0)
                secondsPassed += 1

                if (secondsPassed == progressUpdatePeriod) secondsPassed = 0
                currentFullDate = formatDate(currentTime ?: 0, datePattern)
            }
        }
    }

    if (isChannelInfoShown) {
        Box(
            modifier = modifier
        ) {
            if (seekingStarted) {
                PreviewWindow(
                    Modifier
                        .zIndex(999f)
                        .align(Alignment.TopStart),
                    focusedChannel?.id ?: "-1",
                    currentTime ?: 0,
                    progressBarXOffset,
                    dotXOffset
                )
            }

            Column (
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.secondary.copy(0.8f))
                    .padding(bottom = 20.dp, top = 10.dp)
                    .padding(horizontal = 10.dp)
                    .fillMaxWidth()
            ) {
                val localFocusedEpg = focusedEpg

                if (localFocusedEpg != null && localFocusedEpg.isDvrAvailable) {
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

                        LinearProgressWithDot(
                            Modifier.weight(1f),
                            seekingStarted,
                            isPaused,
                            {x -> progressBarXOffset = x},
                            {x -> dotXOffset = x},
                            { liveProgramme -> isLiveProgramme = liveProgramme},
                        ) { newCurrentDate -> currentFullDate = newCurrentDate}

                        Text(
                            modifier = Modifier.padding(start = 15.dp),
                            text = formatDate(focusedEpg?.stopTime ?: 0, timePattern),
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .padding(top = 15.dp)
                        .fillMaxWidth(),
                ) {

                    if (localFocusedEpg != null && localFocusedEpg.isDvrAvailable) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            ChannelShortInfo(focusedChannelIndex ?: -1, focusedChannel?.name ?: "")
                            ChannelLogo(focusedChannel?.logo ?: "")
                        }
                    } else {
                       Row(
                           verticalAlignment = Alignment.CenterVertically,
                           horizontalArrangement = Arrangement.spacedBy(13.dp)
                       ) {
                           ChannelLogo(focusedChannel?.logo ?: "")
                           ChannelShortInfo(focusedChannelIndex ?: -1, focusedChannel?.name ?: "")
                       }
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth(0.6f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(bottom = if (focusedEpg != null) 25.dp else 0.dp)
                        ) {
                            Text(
                                modifier = Modifier.padding(end = 15.dp),
                                fontSize = 22.sp,
                                text = currentFullDate,
                                color = MaterialTheme.colorScheme.onSecondary
                            )

                            Text(
                                fontSize = 22.sp,
                                text = focusedEpg?.title ?: "No epg",
                                color = MaterialTheme.colorScheme.onSecondary
                            )
                        }

                        Log.i("SHIT4", "RECOMPOSED")

                        if (localFocusedEpg != null && localFocusedEpg.isDvrAvailable) {
                            PlaybackControls(
                                focusedChannel?.url ?: "",
                                { started -> seekingStarted = started },
                                { secondsNotInteracted = 0 },
                                {isLive -> isLiveProgramme = isLive}
                            ) { showDatePicker -> showProgrammeDatePicker(showDatePicker) }
                        }
                    }

                    if (localFocusedEpg != null && localFocusedEpg.isDvrAvailable) {
                        Row(
                            modifier = Modifier.align(Alignment.TopEnd),
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
    }
}