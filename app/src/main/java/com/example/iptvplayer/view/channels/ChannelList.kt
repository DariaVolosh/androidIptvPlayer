package com.example.iptvplayer.view.channels

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp

@Composable
fun ChannelList(
    channelsViewModel: ChannelsViewModel,
    modifier: Modifier,
    playMedia: (String) -> Unit
) {
    val channels by channelsViewModel.channels.observeAsState()
    var focusedChannel by remember {
        mutableIntStateOf(0)
    }

    LaunchedEffect(Unit) {
        channelsViewModel.parsePlaylist()
    }

    LazyColumn(
        modifier = modifier
            .background(Brush.horizontalGradient(
                listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.secondary
                )
            ))
            .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(17.dp)
    ) {
        channels?.let { channels ->
            items(channels.size) { index ->
                Channel(
                    channels[index].name,
                    channels[index].logo,
                    index,
                    index == focusedChannel,
                    { ch -> focusedChannel = ch}
                ) {
                    playMedia(channels[index].url)
                }
            }
        }
    }
}