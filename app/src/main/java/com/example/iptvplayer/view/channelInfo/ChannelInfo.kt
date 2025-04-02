package com.example.iptvplayer.view.channelInfo

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LiveData
import com.example.iptvplayer.data.Utils.formatDate
import com.example.iptvplayer.view.channels.ArchiveViewModel
import com.example.iptvplayer.view.channels.ChannelsViewModel
import com.example.iptvplayer.view.epg.EpgViewModel
import kotlinx.coroutines.delay

@Composable
fun ChannelInfo(
    modifier: Modifier,
    isChannelInfoShown: Boolean,
    isPausedLiveData: LiveData<Boolean>,
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
    var secondsNotInteracted by remember { mutableIntStateOf(0) }

    val currentTime by archiveViewModel.currentTime.observeAsState()
    val isSeeking by archiveViewModel.isSeeking.observeAsState()
    val isLiveProgram by archiveViewModel.isLive.observeAsState()
    val dvrRange by archiveViewModel.dvrRange.observeAsState()

    val focusedEpg by epgViewModel.focusedEpg.observeAsState()

    val focusedChannel by channelsViewModel.focusedChannel.observeAsState()
    val focusedChannelIndex by channelsViewModel.focusedChannelIndex.observeAsState()

    val isPaused by isPausedLiveData.observeAsState()

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isChannelInfoShown) {
        Log.i("IS CHANNEL INFO SHOWN", focusedEpg.toString())
        Log.i("IS CHANNEL INFO SHOWN", isChannelInfoShown.toString())
        if (isChannelInfoShown) {
            focusRequester.requestFocus()
            secondsNotInteracted = 0

            while (secondsNotInteracted < 60) {
                secondsNotInteracted++
                delay(1000)
            }

            Log.i("show channel info", "launched effect true")
            showChannelInfo(false)
        }
    }

    LaunchedEffect(isSeeking, isPaused) {
        Log.i("is paused", isPaused.toString())

        while (isSeeking == false && isPaused == false) {
            archiveViewModel.setCurrentTime(currentTime?.plus(1) ?: 0)
            currentFullDate = formatDate(currentTime ?: 0, datePattern)
            delay(1000)
        }
    }

    if (isChannelInfoShown) {
        Box(
            modifier = modifier
                .zIndex(99f)
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        if (event.key == Key.Back) {
                            Log.i("show channel info", "box on key event info not shown")
                            showChannelInfo(false)
                        }
                    }

                    true
                }
        ) {
            /*if (seekingStarted) {
                PreviewWindow(
                    Modifier
                        .zIndex(999f)
                        .align(Alignment.TopStart),
                    focusedChannel?.id ?: "-1",
                    currentTime ?: 0,
                    progressBarXOffset,
                    dotXOffset
                )
            } */

            Column (
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.secondary.copy(0.8f))
                    .padding(top = 17.dp)
                    .padding(horizontal = 10.dp)
                    .fillMaxWidth()
            ) {
                focusedChannel?.let { focusedChannel ->
                    TimeSeekbarWithTimeMarkers(
                        focusedEpg,
                        dvrRange,
                        focusedChannel,
                    ) { newDate -> currentFullDate = newDate}
                }

                Box(
                    modifier = Modifier
                        //.border(1.dp, Color.Yellow)
                        .fillMaxWidth(),
                ) {
                    ChannelShortInfo(
                        focusedChannelIndex ?: 0,
                        focusedChannel?.name ?: "",
                        focusedChannel?.logo ?: ""
                    )

                    Column(
                        modifier = Modifier
                            //.border(1.dp, Color.Cyan)
                            .align(Alignment.Center)
                            .fillMaxWidth(0.5f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            //modifier = Modifier.border(1.dp, Color.Blue),
                            fontSize = 18.sp,
                            text = currentFullDate,
                            color = MaterialTheme.colorScheme.onSecondary
                        )

                        Text(
                            modifier = Modifier.padding(top = 10.dp),
                            fontSize = 18.sp,
                            text = focusedEpg?.title ?: "No epg",
                            color = MaterialTheme.colorScheme.onSecondary
                        )

                        Log.i("SHIT4", "RECOMPOSED")

                        // archive is available, show playback controls
                        if (dvrRange?.first != 0L) {
                            PlaybackControls(
                                focusedChannel?.url ?: "",
                                { showChannelInfo(false) },
                                { secondsNotInteracted = 0 },
                            ) { showDatePicker -> showProgrammeDatePicker(showDatePicker) }
                        }
                    }

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
                            text = if (isLiveProgram == true) "live" else "record",
                            color = Color.Red,
                            fontSize = 20.sp
                        )
                    }
                }
            }
        }
    }
}