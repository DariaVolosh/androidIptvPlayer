package com.example.iptvplayer.view.channels

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.iptvplayer.data.PlaylistChannel
import kotlinx.coroutines.launch

@Composable
fun ChannelList(
    modifier: Modifier,
    channels: List<PlaylistChannel>,
    focusedChannel: Int,
    isChannelsListFocused: Boolean,
    isChannelClicked: Boolean,
    channelOnKeyEvent: (Key) -> Unit,
) {
    val localDensity = LocalDensity.current.density

    val lazyColumnState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    var itemHeight by remember { mutableIntStateOf(0) }
    var borderYOffset by remember { mutableIntStateOf(0) }

    var visibleItems by remember { mutableIntStateOf(0) }

    var isVisibleItemsMiddle by remember { mutableStateOf(false) }
    var isListEnd by remember { mutableStateOf(false) }

    LaunchedEffect(channels, isChannelClicked) {
        if (!isChannelClicked) {
            val visibleItemsLocal = lazyColumnState.layoutInfo.visibleItemsInfo.size
            visibleItems = visibleItemsLocal
        }
    }

    LaunchedEffect(focusedChannel, isChannelClicked) {
        if (!isChannelClicked) {

            // end of the list is reached, focus has to be moved down
            if (focusedChannel >= channels.size - visibleItems / 2) {
                Log.i("end of channels list", "true $focusedChannel")

                isListEnd = true
                isVisibleItemsMiddle = false
                coroutineScope.launch {
                    lazyColumnState.scrollToItem(channels.size - 1)
                }

            // fixed in the center focus
            } else if (focusedChannel >= visibleItems / 2) {
                Log.i("middle of channels list", "true $focusedChannel")

                isVisibleItemsMiddle = true
                isListEnd = false

                coroutineScope.launch {
                    // taking into consideration list padding (converting it to pixels)
                    Log.i("border y offset", borderYOffset.toString())
                    lazyColumnState.scrollToItem(focusedChannel, -borderYOffset + (15 * localDensity).toInt())
                }

            // beginning of the list reached, focus has to be moved up
            } else {
                Log.i("beginning of channels list", "true $focusedChannel")
                isVisibleItemsMiddle = false
                // scrolling to the first item by default
                coroutineScope.launch {
                    lazyColumnState.scrollToItem(0)
                }
            }
        }
    }

    LaunchedEffect(isChannelsListFocused, isChannelClicked) {
        if (!isChannelClicked) {
            if (isChannelsListFocused) focusRequester.requestFocus()
            else focusRequester.freeFocus()
        }
    }

    if (!isChannelClicked) {
        Box(
            modifier = modifier
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .focusable(true)
                    .onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) {
                            channelOnKeyEvent(event.key)
                        }

                        true
                    }
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f)
                            )
                        )
                    )
                    .onGloballyPositioned { cords ->
                        Log.i("LIST HEIGHT", cords.size.height.toString())
                    }
                    .padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(17.dp),
                state = lazyColumnState
            ) {
                items(channels.size) { index ->
                    Channel(
                        channels[index].name,
                        channels[index].logo,
                        index
                    ) { height -> itemHeight = height }
                }
            }

            Box(
                modifier = Modifier
                    .offset {
                        if (!isVisibleItemsMiddle) {
                            if (!isListEnd) {
                                IntOffset(
                                    0, ((15 + focusedChannel * itemHeight + focusedChannel * 17) * localDensity).toInt()
                                )
                            } else {
                                val itemsFromBottom = channels.size - focusedChannel - 1
                                Log.i("ITEMS FROM BOTTOM", "items from bottom $itemsFromBottom $focusedChannel")
                                IntOffset(
                                    0, ((-15 - itemsFromBottom * itemHeight - itemsFromBottom * 17) * localDensity).toInt()
                                )
                            }
                        } else {
                            IntOffset(0, 0)
                        }
                    }
                    .alpha(if (!isChannelsListFocused) 0f else 1f)
                    .align(
                        if (isVisibleItemsMiddle) Alignment.Center
                        else {
                            if (isListEnd) {
                                Alignment.BottomCenter
                            } else {
                                Alignment.TopCenter
                            }
                        }
                    )
                    .fillMaxWidth()
                    .padding(horizontal = 7.dp)
                    .height(itemHeight.dp)
                    .border(1.dp, Color.White)
                    .onGloballyPositioned { cords ->
                        borderYOffset = cords.positionOnScreen().y.toInt()
                        Log.i("border y offset", borderYOffset.toString())
                    }
            )
        }
    }
}