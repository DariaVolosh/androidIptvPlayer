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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.iptvplayer.data.Utils.formatDate
import com.example.iptvplayer.retrofit.data.Epg
import com.example.iptvplayer.view.channels.ArchiveViewModel
import com.example.iptvplayer.view.channels.ChannelsViewModel
import com.example.iptvplayer.view.channels.MediaViewModel
import com.example.iptvplayer.view.epg.EpgViewModel
import com.example.iptvplayer.view.toast.CustomToast
import kotlinx.coroutines.delay

@Composable
fun ChannelInfo(
    showProgrammeDatePicker: (Boolean) -> Unit,
    showChannelInfo: (Boolean) -> Unit
) {
    val archiveViewModel: ArchiveViewModel = hiltViewModel()
    val mediaViewModel: MediaViewModel = hiltViewModel()
    val epgViewModel: EpgViewModel = hiltViewModel()
    val channelsViewModel: ChannelsViewModel = viewModel()

    val datePattern = "EEEE d MMMM HH:mm:ss"

    var currentFullDate by remember { mutableStateOf("") }
    var secondsNotInteracted by remember { mutableIntStateOf(0) }

    val currentTime by mediaViewModel.currentTime.collectAsState()
    val isLiveProgram by mediaViewModel.isLive.collectAsState()
    val dvrRange by archiveViewModel.dvrRange.collectAsState()

    val currentEpg by epgViewModel.currentEpg.collectAsState()

    val currentChannel by channelsViewModel.currentChannel.collectAsState()
    val currentChannelIndex by channelsViewModel.currentChannelIndex.collectAsState()

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

    LaunchedEffect(currentTime) {
        currentFullDate = formatDate(currentTime, datePattern)
    }

    Box(
        modifier = Modifier
            .zIndex(99f)
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
                        text = currentFullDate.ifEmpty { "No current time" },
                        color = MaterialTheme.colorScheme.onSecondary
                    )

                    Text(
                        modifier = Modifier.padding(top = 10.dp),
                        fontSize = 18.sp,
                        text = if (currentEpg != Epg()) currentEpg.epgVideoName else "No epg",
                        color = MaterialTheme.colorScheme.onSecondary
                    )

                    Log.i("SHIT4", "RECOMPOSED")

                    PlaybackControls(
                        currentChannel.channelUrl,
                        { showChannelInfo(false) },
                        { secondsNotInteracted = 0 },
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
                        text = if (isLiveProgram) "live" else "record",
                        color = Color.Red,
                        fontSize = 20.sp
                    )
                }
            }
        }
    }
}