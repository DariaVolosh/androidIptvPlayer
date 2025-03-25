package com.example.iptvplayer.view.channels

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.iptvplayer.data.PlaylistChannel
import kotlinx.coroutines.launch

@Composable
fun ChannelList(
    modifier: Modifier,
    channels: List<PlaylistChannel>,
    focusedChannel: Int,
    isChannelsListFocused: Boolean,
    channelOnKeyEvent: (Key) -> Unit,
    playMedia: (String) -> Unit
) {

    val lazyColumnState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var itemHeight by remember { mutableIntStateOf(0) }

    var visibleItems by remember { mutableIntStateOf(0) }
    var borderYOffset by remember { mutableIntStateOf(0) }

    LaunchedEffect(isChannelsListFocused) {
        if (isChannelsListFocused) {
            visibleItems = lazyColumnState.layoutInfo.visibleItemsInfo.size
            coroutineScope.launch {
                Log.i("SCROLL", "-$borderYOffset")
                lazyColumnState.scrollToItem(focusedChannel, -borderYOffset + 31)
            }
        }
    }

    val localDensity = LocalDensity.current.density

    Box(
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                    )
                ))
                .onGloballyPositioned { cords ->
                    Log.i("LIST HEIGHT", (cords.size.height / localDensity).toString())
                }
                .padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(17.dp),
            userScrollEnabled = false,
            state = lazyColumnState
        ) {
            items(channels.size) { index ->
                Channel(
                    channels[index].name,
                    channels[index].logo,
                    index,
                    index == focusedChannel,
                    index >= visibleItems / 2,
                    {key -> channelOnKeyEvent(key)},
                    {
                        Log.i("FOCUSED", "CALLED $index")
                        coroutineScope.launch {
                            Log.i("SCROLL", "-$borderYOffset")
                            lazyColumnState.scrollToItem(index, -borderYOffset + 31)
                        }
                    },
                    { height -> itemHeight = height }
                ) {
                    playMedia(channels[index].url)
                }
            }
        }

        if (focusedChannel >= visibleItems / 2) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(itemHeight.dp)
                    .border(1.dp, Color.White)
                    .onGloballyPositioned { cords ->
                        borderYOffset = cords.positionInParent().y.toInt()
                    }
            )
        }
    }
}