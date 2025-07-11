package com.example.iptvplayer.view.channelInfo

import android.util.Log
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.iptvplayer.domain.media.StreamTypeState
import com.example.iptvplayer.view.channelInfo.playbackControls.PlaybackControls
import com.example.iptvplayer.view.channelInfo.progressBar.TimeSeekbarWithTimeMarkers
import com.example.iptvplayer.view.channels.ChannelsViewModel
import com.example.iptvplayer.view.archive.ArchiveViewModel
import com.example.iptvplayer.view.epg.EpgViewModel
import com.example.iptvplayer.view.media.MediaPlaybackViewModel
import com.example.iptvplayer.view.media.MediaViewModel
import com.example.iptvplayer.view.time.DateAndTimeViewModel
import com.example.iptvplayer.view.toast.CustomToast
import kotlinx.coroutines.delay

@Composable
fun ChannelInfo(
    isChannelsInfoFullyVisible: Boolean,
    showProgrammeDatePicker: (Boolean) -> Unit,
    switchChannel: (Boolean) -> Unit,
    showChannelInfo: (Boolean) -> Unit
) {
    val archiveViewModel: ArchiveViewModel = hiltViewModel()
    val mediaViewModel: MediaViewModel = hiltViewModel()
    val mediaPlaybackViewModel: MediaPlaybackViewModel = hiltViewModel()
    val epgViewModel: EpgViewModel = hiltViewModel()
    val channelsViewModel: ChannelsViewModel = viewModel()
    val dateAndTimeViewModel: DateAndTimeViewModel = hiltViewModel()

    val datePattern = "EEEE d MMMM HH:mm:ss"

    var secondsNotInteracted by remember { mutableIntStateOf(0) }

    val currentTime by dateAndTimeViewModel.currentTime.collectAsState()
    val streamType by mediaPlaybackViewModel.streamType.collectAsState()
    val dvrRange by archiveViewModel.currentChannelDvrRanges.collectAsState()

    val currentEpgIndex by epgViewModel.currentEpgIndex.collectAsState()
    val currentEpg by epgViewModel.currentEpg.collectAsState()

    val currentChannel by channelsViewModel.currentChannel.collectAsState()
    val currentChannelIndex by channelsViewModel.currentChannelIndex.collectAsState()

    val currentFullDate by dateAndTimeViewModel.currentFullDate.collectAsState()

    LaunchedEffect(currentFullDate) {
        Log.i("Current full date", currentFullDate)
    }

    LaunchedEffect(currentChannel) {
        Log.i("current channel", currentChannel.toString())
    }

    LaunchedEffect(Unit) {
        Log.i("IS CHANNEL INFO SHOWN", currentEpg.toString())
        secondsNotInteracted = 0

        while (secondsNotInteracted < 60) {
            secondsNotInteracted++
            delay(1000)
        }

        Log.i("show channel info", "launched effect true")
        showChannelInfo(false)
    }

    Box(
        modifier = Modifier
            .zIndex(100f)
    ) {
        Column (
            modifier = Modifier
                .background(MaterialTheme.colorScheme.secondary.copy(0.8f))
                .padding(top = 17.dp)
                .padding(horizontal = 10.dp)
                .fillMaxWidth()
        ) {
            TimeSeekbarWithTimeMarkers(
                currentEpg,
                currentEpgIndex,
                dvrRange,
                currentChannel
            )

            Box(
                modifier = Modifier
                    //.border(1.dp, Color.Yellow)
                    .fillMaxWidth(),
            ) {
                ChannelShortInfo(
                    currentChannelIndex,
                    currentChannel.name,
                    currentChannel.logo,
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
                        text = if (currentEpgIndex != -1) currentEpg.epgVideoName else "No epg",
                        color = MaterialTheme.colorScheme.onSecondary
                    )

                    Log.i("SHIT4", "RECOMPOSED")

                    PlaybackControls(
                        currentChannel.channelUrl,
                        isChannelsInfoFullyVisible,
                        { showChannelInfo(false) },
                        { secondsNotInteracted = 0 },
                        switchChannel,
                    ) { showDatePicker -> showProgrammeDatePicker(showDatePicker) }

                    CustomToast(
                        modifier = Modifier.padding(bottom = 15.dp)
                    )
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
                        text = when(streamType) {
                            StreamTypeState.LIVE -> {
                                "live"
                            }
                            StreamTypeState.ARCHIVE -> {
                                "record"
                            }
                            StreamTypeState.INITIALIZING, StreamTypeState.ERROR -> {
                                ""
                            }
                        },
                        color = Color.Red,
                        fontSize = 20.sp
                    )
                }
            }
        }
    }
}