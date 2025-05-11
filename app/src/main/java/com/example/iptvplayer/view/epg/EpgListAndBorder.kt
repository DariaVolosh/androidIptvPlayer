package com.example.iptvplayer.view.epg

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.platform.LocalDensity
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.iptvplayer.retrofit.data.ChannelData
import com.example.iptvplayer.retrofit.data.Epg
import com.example.iptvplayer.view.channels.ArchiveViewModel
import com.example.iptvplayer.view.channels.ChannelsViewModel
import com.example.iptvplayer.view.channels.ItemBorder
import com.example.iptvplayer.view.channels.MediaViewModel
import kotlinx.coroutines.delay

data class EpgToBeFetched(
    val epgId: Int,
    val channelIndex: Int
)

@Composable
fun EpgListAndBorder(
    modifier: Modifier,
    token: String,
    setCurrentTime: (Long) -> Unit
) {
    val density = LocalDensity.current.density

    val epgViewModel: EpgViewModel = hiltViewModel()
    val archiveViewModel: ArchiveViewModel = hiltViewModel()
    val channelsViewModel: ChannelsViewModel = hiltViewModel()
    val mediaViewModel: MediaViewModel = hiltViewModel()
    val coroutineScope = rememberCoroutineScope()
    val epgListState = rememberLazyListState()

    val isEpgListFocused by epgViewModel.isEpgListFocused.collectAsState()
    val focusedEpgIndex by epgViewModel.focusedEpgIndex.collectAsState()
    val dateMap by epgViewModel.dateMap.collectAsState()
    val epgList by epgViewModel.epgList.collectAsState()

    val isChannelClicked by channelsViewModel.isChannelClicked.collectAsState()
    val currentChannel by channelsViewModel.currentChannel.collectAsState()
    val currentChannelIndex by channelsViewModel.currentChannelIndex.collectAsState()
    val focusedChannelIndex by channelsViewModel.focusedChannelIndex.collectAsState()
    val channelsData by channelsViewModel.channelsData.collectAsState()

    val allChannelsEpg by epgViewModel.allChannelsEpg.collectAsState()

    val dvrRange by archiveViewModel.dvrRange.collectAsState()

    val focusRequester = remember { FocusRequester() }

    // hoisted states from EpgList
    var epgItemHeight by remember { mutableIntStateOf(0) }

    // hoisted states from ItemBorder
    var borderYOffset by remember { mutableIntStateOf(15) }

    LaunchedEffect(isEpgListFocused, isChannelClicked) {
        if (isEpgListFocused && !isChannelClicked) {
            focusRequester.requestFocus()
        }
    }

    val handleKeyEvent: (key: Key) -> Unit = { key ->
        when (key) {
            Key.DirectionDown -> {
                epgViewModel.updateEpgIndex(focusedEpgIndex + 1, false)
            }

            Key.DirectionUp -> {
                epgViewModel.updateEpgIndex(focusedEpgIndex - 1, false)
            }

            Key.DirectionLeft -> {
                epgViewModel.setIsEpgListFocused(false)
                channelsViewModel.setIsChannelsListFocused(true)
            }

            Key.DirectionCenter -> {
                val focusedEpg = epgViewModel.getEpgByIndex(focusedEpgIndex)
                if (
                    focusedEpg != null &&
                    focusedEpg.epgVideoTimeRangeSeconds.start in dvrRange.first..dvrRange.second
                ) {
                    epgViewModel.updateEpgIndex(focusedEpgIndex, true)

                    channelsViewModel.updateChannelIndex(channelsViewModel.focusedChannelIndex.value, true)
                    val currentChannel = channelsViewModel.currentChannel.value

                    setCurrentTime(focusedEpg.epgVideoTimeRangeSeconds.start)
                    archiveViewModel.getArchiveUrl(
                        currentChannel.channelUrl,
                        focusedEpg.epgVideoTimeRangeSeconds.start
                    )

                    channelsViewModel.setIsChannelClicked(true)
                }
            }

            Key.Back -> {
                channelsViewModel.setIsChannelClicked(true)
            }
        }
    }

    // launched effect that handles dvr collection job and epg list update
    LaunchedEffect(
        isChannelClicked,
        currentChannel,
        currentChannelIndex,
        focusedChannelIndex,
        channelsData,
    ) {
        Log.i("current channel", "$currentChannelIndex")
        archiveViewModel.dvrCollectionJob?.cancel()
        epgViewModel.currentEpgUpdateJob?.cancel()
        delay(500)

        if (isChannelClicked) {
            if (currentChannel != ChannelData()) {
                archiveViewModel.startDvrCollectionJob(currentChannel.name)
            }

            if (currentChannelIndex != -1) {
                var currentChannelEpg: List<Epg> = emptyList()

                while (currentChannelEpg.isEmpty()) {
                    currentChannelEpg = epgViewModel.getCachedEpg(currentChannel.epgChannelId.toInt())
                    delay(100)
                }

                val liveTime = mediaViewModel.liveTime.value
                val currentTime = mediaViewModel.currentTime.value

                epgViewModel.updateCurrentEpgList(currentChannelEpg, liveTime, currentTime)
            }
        } else {
            if (focusedChannelIndex != -1 && channelsData.isNotEmpty()) {
                val focusedChannel = channelsData[focusedChannelIndex]
                archiveViewModel.startDvrCollectionJob(focusedChannel.name)

                var focusedChannelEpg: List<Epg> = emptyList()

                while (focusedChannelEpg.isEmpty()) {
                    focusedChannelEpg = epgViewModel.getCachedEpg(focusedChannel.epgChannelId.toInt())
                    delay(100)
                }

                val liveTime = mediaViewModel.liveTime.value
                val currentTime = mediaViewModel.currentTime.value

                if (focusedChannelIndex == currentChannelIndex) {
                    epgViewModel.updateCurrentEpgList(focusedChannelEpg, liveTime, currentTime)
                } else {
                    epgViewModel.updateCurrentEpgList(focusedChannelEpg, liveTime, liveTime)
                }
            }
        }
    }

    // launched effect that handles epg fetching
    LaunchedEffect(focusedChannelIndex) {
        epgViewModel.epgFetchJob?.cancel()
        delay(500)
        if (focusedChannelIndex != -1) {
            var previousChannelsAdded = false
            var channelToFetchEpgFor = focusedChannelIndex

            val localChannelsData = channelsData ?: return@LaunchedEffect

            val epgRequest = mutableListOf<EpgToBeFetched>()
            while (epgRequest.size < 10 && channelToFetchEpgFor < localChannelsData.size) {
                Log.i("channel index channels list", focusedChannelIndex.toString())
                Log.i("channel index channels list", "epg request size ${epgRequest.size}")
                epgRequest.add(EpgToBeFetched(localChannelsData[channelToFetchEpgFor].epgChannelId.toInt(), channelToFetchEpgFor))

                if (!previousChannelsAdded && channelToFetchEpgFor - 1 >= 0 && epgRequest.size < 5) {
                    channelToFetchEpgFor--
                } else {
                    if (!previousChannelsAdded) {
                        channelToFetchEpgFor = focusedChannelIndex
                        previousChannelsAdded = true
                    }
                    channelToFetchEpgFor++
                }
            }

            epgViewModel.fetchEpg(epgRequest, token)
        }
    }

    AnimatedVisibility(
        visible = !isChannelClicked,
        enter = fadeIn(),
        exit = fadeOut()
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
        ) {
            EpgList(
                Modifier.fillMaxWidth(),
                dvrRange,
                epgItemHeight,
                {height -> epgItemHeight = height},
            ) { borderOffset -> borderYOffset = borderOffset }

            if (isEpgListFocused) {
                ItemBorder(
                    borderYOffset,
                    epgItemHeight
                )
            }
        }
    }
}