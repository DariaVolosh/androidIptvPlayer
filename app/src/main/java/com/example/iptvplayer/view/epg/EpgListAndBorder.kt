package com.example.iptvplayer.view.epg

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.iptvplayer.retrofit.data.ChannelData
import com.example.iptvplayer.retrofit.data.EpgListItem
import com.example.iptvplayer.view.archive.ArchiveViewModel
import com.example.iptvplayer.view.channels.ChannelsViewModel
import com.example.iptvplayer.view.channelsAndEpgRow.ItemBorder
import com.example.iptvplayer.view.media.MediaViewModel
import com.example.iptvplayer.view.time.DateAndTimeViewModel
import kotlinx.coroutines.delay

data class EpgToBeFetched(
    val epgId: Int,
    val channelIndex: Int
)

@Composable
fun EpgListAndBorder(
    modifier: Modifier,
    token: String = "",
    setCurrentTime: (Long) -> Unit
) {

    val epgViewModel: EpgViewModel = hiltViewModel()
    val archiveViewModel: ArchiveViewModel = hiltViewModel()
    val channelsViewModel: ChannelsViewModel = hiltViewModel()
    val mediaViewModel: MediaViewModel = hiltViewModel()
    val dateAndTimeViewModel: DateAndTimeViewModel = hiltViewModel()

    val isEpgListFocused by epgViewModel.isEpgListFocused.collectAsState()
    val focusedEpgIndex by epgViewModel.focusedEpgIndex.collectAsState()

    val isChannelClicked by channelsViewModel.isChannelClicked.collectAsState()
    val currentChannel by channelsViewModel.currentChannel.collectAsState()
    val currentChannelIndex by channelsViewModel.currentChannelIndex.collectAsState()
    val focusedChannelIndex by channelsViewModel.focusedChannelIndex.collectAsState()
    val channelsData by channelsViewModel.channelsData.collectAsState()

    val dvrRanges by archiveViewModel.focusedChannelDvrRanges.collectAsState()
    val currentDvrRange by archiveViewModel.currentChannelDvrRange.collectAsState()

    val focusRequester = remember { FocusRequester() }

    // hoisted states from EpgList
    var epgItemHeight by remember { mutableIntStateOf(0) }
    var isListMiddle by remember { mutableStateOf(false) }

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
                val nextEpgIndex = epgViewModel.findFirstEpgIndexForward(focusedEpgIndex + 1)
                epgViewModel.updateEpgIndex(nextEpgIndex, false)
            }

            Key.DirectionUp -> {
                val previousEpgIndex = epgViewModel.findFirstEpgIndexBackward(focusedEpgIndex -1)

                epgViewModel.updateEpgIndex(previousEpgIndex, false)
            }

            Key.DirectionLeft -> {
                epgViewModel.setIsEpgListFocused(false)
                channelsViewModel.setIsChannelsListFocused(true)
            }

            Key.DirectionCenter -> {
                val focusedEpg = epgViewModel.getEpgItemByIndex(focusedEpgIndex) as EpgListItem.Epg

                archiveViewModel.determineCurrentDvrRange(false, focusedEpg.epgVideoTimeRangeSeconds.start)

                if (
                    currentDvrRange != -1 &&
                    focusedEpg.epgVideoTimeRangeSeconds.start in dvrRanges[currentDvrRange].from..dvrRanges[currentDvrRange].from + dvrRanges[currentDvrRange].duration
                ) {
                    epgViewModel.updateEpgIndex(focusedEpgIndex, true)
                    channelsViewModel.updateChannelIndex(channelsViewModel.focusedChannelIndex.value, true)

                    mediaViewModel.updateIsLive(false)

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

    LaunchedEffect(channelsData, focusedChannelIndex) {
        archiveViewModel.focusedChannelDvrCollectionJob?.cancel()
        epgViewModel.focusedEpgListUpdateJob?.cancel()
        delay(500)

        if (focusedChannelIndex != -1 && channelsData.isNotEmpty()) {
            val focusedChannel = channelsData[focusedChannelIndex]
            archiveViewModel.startDvrCollectionJob(false, focusedChannel.name)

            var focusedChannelEpg: List<EpgListItem.Epg> = emptyList()

            while (focusedChannelEpg.isEmpty()) {
                focusedChannelEpg = epgViewModel.getCachedEpg(focusedChannel.epgChannelId.toInt())
                delay(100)
            }

            val liveTime = dateAndTimeViewModel.liveTime.value
            val currentTime = dateAndTimeViewModel.currentTime.value

            if (focusedChannelIndex == currentChannelIndex) {
                epgViewModel.updateFocusedEpgList(focusedChannelEpg, liveTime, currentTime)
            } else {
                epgViewModel.updateFocusedEpgList(focusedChannelEpg, liveTime, liveTime)
            }
        }
    }

    LaunchedEffect(channelsData, currentChannelIndex) {
        archiveViewModel.currentChannelDvrCollectionJob?.cancel()
        epgViewModel.currentEpgListUpdateJob?.cancel()
        delay(500)

        if (currentChannel != ChannelData() && channelsData.isNotEmpty()) {
            archiveViewModel.startDvrCollectionJob(true, currentChannel.name)
            var currentChannelEpg: List<EpgListItem.Epg> = emptyList()

            while (currentChannelEpg.isEmpty()) {
                currentChannelEpg = epgViewModel.getCachedEpg(currentChannel.epgChannelId.toInt())
                delay(100)
            }

            val liveTime = dateAndTimeViewModel.liveTime.value
            val currentTime = dateAndTimeViewModel.currentTime.value

            epgViewModel.updateCurrentEpgList(currentChannelEpg, liveTime, currentTime)
        }
    }

    // launched effect that handles epg fetching
    LaunchedEffect(focusedChannelIndex) {
        epgViewModel.epgFetchJob?.cancel()
        delay(500)
        if (focusedChannelIndex != -1) {
            var previousChannelsAdded = false
            var channelToFetchEpgFor = focusedChannelIndex

            val localChannelsData = channelsData

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
                archiveViewModel.getDvrRange(false),
                epgItemHeight,
                {isMiddle -> isListMiddle = isMiddle},
                {height -> epgItemHeight = height},
            ) { borderOffset -> borderYOffset = borderOffset }

            if (isEpgListFocused && isListMiddle) {
                ItemBorder(
                    Modifier
                        .fillMaxWidth(0.97f)
                        .border(1.dp, Color.White)
                        .padding(horizontal = 10.dp),
                    borderYOffset,
                    epgItemHeight
                )
            }
        }
    }
}