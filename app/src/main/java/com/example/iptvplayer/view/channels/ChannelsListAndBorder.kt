package com.example.iptvplayer.view.channels

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.iptvplayer.view.channelsAndEpgRow.ItemBorder
import com.example.iptvplayer.view.epg.EpgViewModel
import com.example.iptvplayer.view.player.MediaViewModel
import kotlinx.coroutines.launch

@Composable
fun ChannelsListAndBorder(
    modifier: Modifier,
    token: String
) {
    val localDensity = LocalDensity.current.density

    val channelsViewModel: ChannelsViewModel = hiltViewModel()
    val mediaViewModel: MediaViewModel = hiltViewModel()
    val epgViewModel: EpgViewModel = hiltViewModel()

    val isChannelClicked by channelsViewModel.isChannelClicked.collectAsState()
    val focusedChannelIndex by channelsViewModel.focusedChannelIndex.collectAsState()
    val currentChannelIndex by channelsViewModel.currentChannelIndex.collectAsState()
    val channelsData by channelsViewModel.channelsData.collectAsState()
    val isChannelsListFocused by channelsViewModel.isChannelsListFocused.collectAsState()

    // states that belong to ChannelsListAndBorder
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    var channelsListHeight by remember { mutableIntStateOf(0) }

    // hoisted states from ItemBorder
    var borderYOffset by remember { mutableIntStateOf(15) }

    // hoisted states from ChannelsList
    var channelHeight by remember { mutableIntStateOf(0) }
    val channelsListState = rememberLazyListState()
    var visibleChannelsAmount by remember { mutableIntStateOf(0) }

    // channel is not clicked => focus on channels list
    LaunchedEffect(isChannelClicked, isChannelsListFocused) {
        Log.i("is channel clicked", "channels list and border $isChannelClicked")
        if (!isChannelClicked && isChannelsListFocused) {
            focusRequester.requestFocus()
        }
    }

    val handleKeyEvent: (Key) -> Unit = { key ->
        when (key) {
            Key.DirectionDown -> {
               channelsViewModel.updateChannelIndex(focusedChannelIndex + 1, false)
            }
            Key.DirectionUp -> {
                channelsViewModel.updateChannelIndex(focusedChannelIndex - 1, false)
            }

            Key.DirectionRight -> {
                channelsViewModel.setIsChannelsListFocused(false)
                epgViewModel.setIsEpgListFocused(true)
            }

            Key.DirectionCenter -> {
                coroutineScope.launch {
                    if (currentChannelIndex != focusedChannelIndex || !mediaViewModel.isLive.value) {
                        mediaViewModel.setCurrentTime(mediaViewModel.liveTime.value)
                        mediaViewModel.updateIsLive(true)
                        channelsViewModel.updateChannelIndex(focusedChannelIndex, true)

                        val focusedChannel = channelsViewModel.getChannelByIndex(focusedChannelIndex)
                        focusedChannel?.let { channel ->
                            mediaViewModel.setMediaUrl(channel.channelUrl)
                        }
                    }
                }
            }

            Key.Back -> {
                channelsViewModel.setIsChannelClicked(true)
            }
        }
    }

    // fetching channels data
    LaunchedEffect(token) {
        if (token.isNotEmpty()) {
            Log.i("received token", token)
            channelsViewModel.fetchChannelsData(token)
        }
    }

    // determining border offset and performing scroll of the list to align item with border
    LaunchedEffect(focusedChannelIndex, channelsListHeight, channelHeight) {
        if (channelHeight != 0 && channelsListHeight != 0) {
            Log.i("focused stuff", "focused channel $focusedChannelIndex channel height $channelHeight")
            visibleChannelsAmount = channelsListHeight / (channelHeight + 17 * localDensity).toInt()

            if (focusedChannelIndex < visibleChannelsAmount / 2) {
                borderYOffset = (15 * localDensity + focusedChannelIndex * channelHeight + focusedChannelIndex * 17 * localDensity).toInt()

                coroutineScope.launch {
                    channelsListState.animateScrollToItem(0)
                }
            } else if (focusedChannelIndex >= channelsData.size - visibleChannelsAmount / 2) {
                val itemsFromBottom = channelsData.size - focusedChannelIndex
                Log.i("channels list and border", "items from bottom $itemsFromBottom")

                borderYOffset = (channelsListHeight - (15 * localDensity + itemsFromBottom * channelHeight + (itemsFromBottom - 1) * 17 * localDensity)).toInt()
                Log.i("channels list and border", "border y offset $borderYOffset")
                Log.i("channels list and border", "channels list height $channelsListHeight")

                coroutineScope.launch {
                    channelsListState.animateScrollToItem(channelsData.size-1)
                }
            } else {
                borderYOffset = (channelsListHeight / 2 - channelHeight / 2)
                Log.i("channels list and border", "border y offset $borderYOffset")

                coroutineScope.launch {
                    channelsListState.animateScrollToItem(focusedChannelIndex, (-borderYOffset + 15 * localDensity).toInt())
                }
            }
        }
    }

    LaunchedEffect(channelsData) {
        if (channelsData.isNotEmpty()) {
            Log.i("channels data", "$channelsData")
            val cachedChannelIndex = channelsViewModel.getCachedChannelIndex()
            channelsViewModel.updateChannelIndex(cachedChannelIndex, true)
            channelsViewModel.updateChannelIndex(cachedChannelIndex, false)
        }
    }

    AnimatedVisibility(
        visible = !isChannelClicked,
        enter = slideInHorizontally(),
        exit = slideOutHorizontally()
    ) {
        Box(
            modifier = modifier
                .focusRequester(focusRequester)
                .focusable(true)
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        handleKeyEvent(event.key)
                    }

                    true
                }
                .onGloballyPositioned { cords ->
                    channelsListHeight = cords.size.height
                }
        ) {
            ChannelsList(channelsListState) {
                height -> channelHeight = height
            }

            if (isChannelsListFocused) {
                ItemBorder(
                    borderYOffset,
                    channelHeight
                )
            }
        }
    }
}