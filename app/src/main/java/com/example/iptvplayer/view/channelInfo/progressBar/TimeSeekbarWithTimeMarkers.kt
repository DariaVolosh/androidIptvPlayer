package com.example.iptvplayer.view.channelInfo.progressBar

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.iptvplayer.retrofit.data.ChannelData
import com.example.iptvplayer.retrofit.data.DvrRange
import com.example.iptvplayer.retrofit.data.EpgListItem
import com.example.iptvplayer.view.time.DateAndTimeViewModel
import com.example.iptvplayer.view.time.DateType

@Composable
fun TimeSeekbarWithTimeMarkers(
    currentEpg: EpgListItem.Epg,
    currentEpgIndex: Int,
    dvrRanges: List<DvrRange>,
    focusedChannel: ChannelData
) {
    val timePattern = "HH:mm"
    val datePattern = "dd MMMM HH:mm:ss"

    val dateAndTimeViewModel: DateAndTimeViewModel = hiltViewModel()

    val startTime by dateAndTimeViewModel.startTime.collectAsState()
    val stopTime by dateAndTimeViewModel.stopTime.collectAsState()

    LaunchedEffect(currentEpg, dvrRanges) {
        Log.i("dvr ranges", dvrRanges.toString())
        Log.i("time seekbar", "$currentEpg $dvrRanges")
        Log.i("time seekbar", "current epg index $currentEpgIndex")
        if (currentEpgIndex != -1) {
            dateAndTimeViewModel.formatDate(currentEpg.epgVideoTimeRangeSeconds.start, timePattern, DateType.START_TIME)
            dateAndTimeViewModel.formatDate(currentEpg.epgVideoTimeRangeSeconds.stop, timePattern, DateType.STOP_TIME)
        } else if (dvrRanges.isNotEmpty()) {
            Log.i("dvr is not 0", "true")
            dateAndTimeViewModel.formatDate(dvrRanges[0].from, datePattern, DateType.START_TIME)
            dateAndTimeViewModel.formatDate(dvrRanges[dvrRanges.size-1].from + dvrRanges[dvrRanges.size-1].duration, datePattern, DateType.STOP_TIME)
        } else {
            dateAndTimeViewModel.resetDate(DateType.START_TIME)
            dateAndTimeViewModel.resetDate(DateType.STOP_TIME)
        }
    }

    Row(
        modifier = Modifier
            .padding(bottom = 10.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Text(
            text = startTime,
            fontSize = 19.sp,
            color = MaterialTheme.colorScheme.onSecondary
        )

        LinearProgressWithDot(
            Modifier.weight(1f),
            focusedChannel.channelUrl
        )

        Text(
            text = stopTime,
            fontSize = 19.sp,
            color = MaterialTheme.colorScheme.onSecondary
        )
    }
}