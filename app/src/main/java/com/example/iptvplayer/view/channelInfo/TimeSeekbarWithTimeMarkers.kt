package com.example.iptvplayer.view.channelInfo

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
import com.example.iptvplayer.data.Epg
import com.example.iptvplayer.data.PlaylistChannel
import com.example.iptvplayer.data.Utils.formatDate

@Composable
fun TimeSeekbarWithTimeMarkers(
    currentEpg: Epg?,
    dvrRange: Pair<Long, Long>?,
    focusedChannel: PlaylistChannel,
    updateCurrentDate: (String) -> Unit
) {
    val timePattern = "HH:mm"
    val datePattern = "dd MMMM HH:mm:ss"

    var startTime by remember { mutableStateOf("") }
    var stopTime by remember { mutableStateOf("") }

    LaunchedEffect(currentEpg, dvrRange) {
        if (currentEpg != null) {
            startTime = formatDate(currentEpg.startTime, timePattern)
            stopTime = formatDate(currentEpg.stopTime, timePattern)
        } else if (dvrRange != null && dvrRange.first != 0L) {
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
            focusedChannel.url,
            updateCurrentDate
        )

        Text(
            text = stopTime,
            fontSize = 19.sp,
            color = MaterialTheme.colorScheme.onSecondary
        )
    }
}