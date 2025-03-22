package com.example.iptvplayer.view.channelInfo

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ChannelShortInfo(
    focusedChannelIndex: Int,
    channelName: String
) {
    Row() {
        Text(
            modifier = Modifier.padding(end = 7.dp),
            text = "${focusedChannelIndex + 1}.",
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.onSecondary
        )

        Text(
            text = channelName,
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.onSecondary
        )
    }
}