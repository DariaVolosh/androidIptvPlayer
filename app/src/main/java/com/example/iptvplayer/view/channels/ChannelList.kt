package com.example.iptvplayer.view.channels

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.iptvplayer.data.PlaylistChannel

@Composable
fun ChannelList(
    modifier: Modifier,
    channels: List<PlaylistChannel>,
    focusedChannel: Int,
    onChannelClicked: () -> Unit,
    onFocusedChannel: (Int) -> Unit,
    playMedia: (String) -> Unit
) {
    LazyColumn(
        modifier = modifier
            .background(Brush.horizontalGradient(
                listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                )
            ))
            .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(17.dp)
    ) {
        items(channels.size) { index ->
            Channel(
                channels[index].name,
                channels[index].logo,
                index,
                index == focusedChannel,
                onChannelClicked,
                {ch -> onFocusedChannel(ch)}
            ) {
                playMedia(channels[index].url)
            }
        }
    }
}