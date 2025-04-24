package com.example.iptvplayer.view.channels

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.livedata.observeAsState
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
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ChannelList(
    modifier: Modifier,
    token: String,
    setMediaUrl: (String) -> Unit,
    setIsEpgListFocused: (Boolean) -> Unit,
    updateCurrentEpgList: () -> Unit,
    fetchEpg: (List<Pair<Int, Int>>) -> Unit,
) {
    val localDensity = LocalDensity.current.density
    val channelsViewModel: ChannelsViewModel = hiltViewModel()

    //val channels by channelsViewModel.playlistChannels.observeAsState()
    val channelsData by channelsViewModel.channelsData.observeAsState()
    val isChannelsListFocused by channelsViewModel.isChannelsListFocused.observeAsState()
    val focusedChannelIndex by channelsViewModel.focusedChannelIndex.observeAsState()
    val isChannelClicked by channelsViewModel.isChannelClicked.observeAsState()

    val lazyColumnState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    var itemHeight by remember { mutableIntStateOf(0) }
    var borderYOffset by remember { mutableIntStateOf(0) }

    var visibleItems by remember { mutableIntStateOf(0) }

    var isVisibleItemsMiddle by remember { mutableStateOf(false) }
    var isListEnd by remember { mutableStateOf(false) }
    var allChannelsAreVisible by remember { mutableStateOf(false) }

    LaunchedEffect(channelsData, isChannelClicked) {
        val localChannelsData = channelsData

        if (isChannelClicked == false && localChannelsData != null) {
            val visibleItemsLocal = lazyColumnState.layoutInfo.visibleItemsInfo.size
            visibleItems = visibleItemsLocal
            Log.i("channels visible", visibleItemsLocal.toString())

            val epgRequest = mutableListOf<Pair<Int, Int>>()
            for (i in 0..<visibleItemsLocal) {
                epgRequest.add(Pair(localChannelsData[i].channel[0].epgChannelId.toInt(), i))
            }

            fetchEpg(epgRequest)

            if (visibleItemsLocal == channelsData?.size) allChannelsAreVisible = true
        }
    }

    LaunchedEffect(focusedChannelIndex, isChannelClicked) {
        val localChannels = channelsData
        val focusedChannelIndexLocal = focusedChannelIndex

        if (isChannelClicked == false && focusedChannelIndexLocal != null) {
            CoroutineScope(Dispatchers.IO).launch {
                updateCurrentEpgList()
            }

            Log.i("focused stuff", "focused channel $focusedChannelIndex visible ${visibleItems / 2}")

            if (allChannelsAreVisible) return@LaunchedEffect

            // end of the list is reached, focus has to be moved down
            if (localChannels != null && focusedChannelIndexLocal >= localChannels.size - visibleItems / 2) {
                Log.i("end of channels list", "true $focusedChannelIndex")

                isListEnd = true
                isVisibleItemsMiddle = false
                coroutineScope.launch {
                    lazyColumnState.scrollToItem(localChannels.size - 1)
                }

            // fixed in the center focus
            } else if (focusedChannelIndexLocal >= visibleItems / 2) {
                Log.i("middle of channels list", "true $focusedChannelIndexLocal")

                isVisibleItemsMiddle = true
                isListEnd = false

                coroutineScope.launch {
                    // taking into consideration list padding (converting it to pixels)
                    Log.i("border y offset", borderYOffset.toString())
                    lazyColumnState.scrollToItem(focusedChannelIndexLocal, -borderYOffset + (15 * localDensity).toInt())
                }

            // beginning of the list reached, focus has to be moved up
            } else {
                Log.i("beginning of channels list", "true $focusedChannelIndexLocal")
                isVisibleItemsMiddle = false
                // scrolling to the first item by default
                coroutineScope.launch {
                    lazyColumnState.scrollToItem(0)
                }
            }
        }
    }

    LaunchedEffect(isChannelsListFocused, isChannelClicked) {
        if (isChannelClicked == false) {
            if (isChannelsListFocused == true) focusRequester.requestFocus()
            else focusRequester.freeFocus()
        }
    }

    LaunchedEffect(token) {
        if (token.isNotEmpty()) {
            channelsViewModel.fetchChannelsData(token)
        }
    }

    if (isChannelClicked == false) {
        Box(
            modifier = modifier
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester)
                    .focusable(true)
                    .onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) {
                            channelsViewModel.handleChannelOnKeyEvent(
                                event.key,
                                setMediaUrl,
                                setIsEpgListFocused,
                            )
                        }

                        true
                    }
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
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
                items(channelsData?.size ?: 0) { index ->
                    channelsData?.get(index)?.let { channelData ->
                        val channel = channelData.channel[0]
                        Channel(
                            channel.channelScreenName,
                            channel.logo,
                            index
                        ) { height -> itemHeight = height }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .offset {
                        val focusedChannelIndexLocal = focusedChannelIndex

                        if (!isVisibleItemsMiddle && focusedChannelIndexLocal != null) {
                            if (!isListEnd) {
                                IntOffset(
                                    0, ((15 + focusedChannelIndexLocal * itemHeight + focusedChannelIndexLocal * 17) * localDensity).toInt()
                                )
                            } else {
                                val localChannels = channelsData
                                if (localChannels != null) {
                                    val itemsFromBottom = localChannels.size - focusedChannelIndexLocal - 1
                                    Log.i("ITEMS FROM BOTTOM", "items from bottom $itemsFromBottom $focusedChannelIndexLocal")
                                    IntOffset(
                                        0, ((-15 - itemsFromBottom * itemHeight - itemsFromBottom * 17) * localDensity).toInt()
                                    )
                                } else {
                                    IntOffset(0, 0)
                                }
                            }
                        } else {
                            IntOffset(0, 0)
                        }
                    }
                    .alpha(if (isChannelsListFocused == false) 0f else 1f)
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