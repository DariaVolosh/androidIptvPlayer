package com.example.iptvplayer.view.channels

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ChannelsList(
    lazyColumnState: LazyListState,
    setChannelHeight: (Int) -> Unit
) {
    val channelsViewModel: ChannelsViewModel = hiltViewModel()
    val channelsData by channelsViewModel.channelsData.collectAsState()

    LazyColumn(
        modifier = Modifier.padding(15.dp),
        state = lazyColumnState,

        verticalArrangement = Arrangement.spacedBy(17.dp)
    ) {
        items(channelsData.size, {index -> channelsData[index].hashCode()}) { index ->
            val channelData = channelsData[index]
            Log.i("channels list", "displaying $index channel")

            Channel(
                name = channelData.channelScreenName,
                logo = channelData.logo,
                index = index,
                setChannelHeight
            )
        }
    }
}