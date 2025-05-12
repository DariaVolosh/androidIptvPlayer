package com.example.iptvplayer.view.channelInfo

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.iptvplayer.data.Utils.formatDate
import com.example.iptvplayer.retrofit.data.ChannelData
import com.example.iptvplayer.retrofit.data.Epg

@Composable
fun TimeSeekbarWithTimeMarkers(
    currentEpg: Epg,
    currentEpgIndex: Int,
    dvrRange: Pair<Long, Long>,
    focusedChannel: ChannelData
) {
    val timePattern = "HH:mm"
    val datePattern = "dd MMMM HH:mm:ss"

    var startTime by remember { mutableStateOf("") }
    var stopTime by remember { mutableStateOf("") }

    LaunchedEffect(currentEpg, dvrRange) {
        Log.i("time seekbar", "$currentEpg $dvrRange")
        if (currentEpgIndex != -1) {
            startTime = formatDate(currentEpg.epgVideoTimeRangeSeconds.start, timePattern)
            stopTime = formatDate(currentEpg.epgVideoTimeRangeSeconds.stop, timePattern)
        } else if (dvrRange.first != 0L) {
            Log.i("dvr is not 0", "true")
            startTime = formatDate(dvrRange.first, datePattern)
            stopTime = formatDate(dvrRange.second, datePattern)
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