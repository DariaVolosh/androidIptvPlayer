package com.example.iptvplayer.view.epg

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.iptvplayer.retrofit.data.DvrRange
import com.example.iptvplayer.retrofit.data.EpgListItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EpgList(
    modifier: Modifier,
    dvrRange: DvrRange,
    epgItemHeight: Int,
    setIsListMiddle: (Boolean) -> Unit,
    setEpgItemHeight: (Int) -> Unit,
    setBorderYOffset: (Int) -> Unit,
) {
    val listState = rememberLazyListState()

    val epgViewModel: EpgViewModel = hiltViewModel()

    val dayToEpgMap by epgViewModel.dayToEpgList.collectAsState()
    val epgList by epgViewModel.focusedChannelEpgItems.collectAsState()
    val focusedEpgIndex by epgViewModel.focusedEpgIndex.collectAsState()
    val isEpgListFocused by epgViewModel.isEpgListFocused.collectAsState()

    var isInitScrollPerformed by remember { mutableStateOf(false) }
    var epgListHeight by remember { mutableIntStateOf(0) }
    val epgListBorderMiddle = remember {
        derivedStateOf { epgListHeight / 2 - epgItemHeight / 2 }
    }
    val visibleItemsInfo by remember {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo
        }
    }

    var firstVisibleDateIndex by remember {
        mutableIntStateOf(0)
    }
    var firstVisibleDateLastEpgOffset by remember {
        mutableIntStateOf(0)
    }
    var epgDateHeight by remember { mutableIntStateOf(0) }
    var isListStart by remember { mutableStateOf(false) }
    var isListEnd by remember { mutableStateOf(false) }
    var focusedEpgUID by remember { mutableStateOf("") }

    LaunchedEffect(
        focusedEpgIndex,
        dayToEpgMap,
        epgList,
        epgItemHeight,
        epgListHeight,
        epgDateHeight,
        isEpgListFocused
    ) {
        if (dayToEpgMap.isEmpty() || epgList.isEmpty()) {
            firstVisibleDateIndex = 0
            isInitScrollPerformed = false
            isListStart = false
            isListEnd = false
            setIsListMiddle(false)
            return@LaunchedEffect
        }

        Log.i("epg list", "focused index $focusedEpgIndex")
        Log.i("epg list", "epg item height $epgItemHeight")
        Log.i("epg list", "epg list border middle ${epgListBorderMiddle.value}")

        if (
            focusedEpgIndex != -1 &&
            epgItemHeight != 0 &&
            epgListBorderMiddle.value != 0
        ) {

            setBorderYOffset(epgListBorderMiddle.value)
            val epg = (epgViewModel.getEpgItemByIndex(focusedEpgIndex) as EpgListItem.Epg)
            focusedEpgUID = epg.uniqueId

            val focusedItemInfo = visibleItemsInfo.firstOrNull { info ->
                info.index == focusedEpgIndex
            }

            if (focusedItemInfo == null) {
                listState.scrollToItem(focusedEpgIndex)
            }

            focusedItemInfo?.let { focusedInfo ->
                if (visibleItemsInfo[0].index == 0) {
                    // compare offset and middle border offset to determine
                    // if focused element is in the beginning or in the middle of the screen

                    if (
                        focusedInfo.offset < epgListBorderMiddle.value &&
                        visibleItemsInfo[1].offset > 0
                        ) {
                        // show item border, focused element is in the beginning
                        listState.scrollToItem(0)
                        setIsListMiddle(false)
                        isListStart = true
                        return@LaunchedEffect
                    }
                } else if (
                    visibleItemsInfo[visibleItemsInfo.size-1].index == epgList.size - 1
                    ) {
                    if (focusedInfo.offset > epgListBorderMiddle.value) {
                        // show item border, focused element is in the end
                        setIsListMiddle(false)
                        isListEnd = true
                        return@LaunchedEffect
                    }
                }

                // show middle border, focused element is in the middle of the screen
                setIsListMiddle(true)
                isListEnd = false
                val scrollOffset = -epgListBorderMiddle.value
                listState.scrollToItem(focusedEpgIndex, scrollOffset)
            }

            val firstVisibleDateItem = visibleItemsInfo.firstOrNull { info ->
                val item = epgList[info.index]
                item is EpgListItem.Header
            }

            var lastEpgForFirstDate: LazyListItemInfo? = null

            firstVisibleDateItem?.let { firstDateItem ->
                firstVisibleDateIndex = firstDateItem.index
                val epgListForFirstDate = dayToEpgMap[epgList[firstDateItem.index]]
                val lastEpgForFirstDateId = epgListForFirstDate?.last()?.uniqueId

                lastEpgForFirstDate = visibleItemsInfo.firstOrNull { info ->
                    info.key == lastEpgForFirstDateId
                }
            }

            lastEpgForFirstDate?.let { lastEpg ->
                firstVisibleDateLastEpgOffset = lastEpg.offset
            }


            isInitScrollPerformed = true
        }
    }



    Box(
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 15.dp)
                .alpha(if (isInitScrollPerformed) 1f else 0f)
                .onGloballyPositioned { cords ->
                    epgListHeight = cords.size.height
                },
            verticalArrangement = Arrangement.spacedBy(7.dp),
            state = listState,
            userScrollEnabled = false
        ) {
            if (dayToEpgMap.isNotEmpty() && epgList.isNotEmpty()) {
                dayToEpgMap.forEach { (header, epgsInGroup) ->
                    stickyHeader {
                        val isFirstVisibleDate = header.title == (epgList[firstVisibleDateIndex] as EpgListItem.Header).title
                        val headerAlpha = if (isFirstVisibleDate && firstVisibleDateLastEpgOffset < epgDateHeight) {
                            0f
                        } else {
                            1f
                        }

                        Log.i("offsets", "first visible date last epg offset $firstVisibleDateLastEpgOffset epg date height $epgDateHeight")
                        EpgDate(
                            header.title,
                            headerAlpha
                        ) { height -> epgDateHeight = height}
                    }

                    items(epgsInGroup.size, {i -> epgsInGroup[i].uniqueId}) { index ->
                        val item = epgsInGroup[index]
                        val isDvrAvailable =
                            item.epgVideoTimeRangeSeconds.start in dvrRange.from..dvrRange.from + dvrRange.duration

                        EpgItem(
                            item.epgVideoTimeRange,
                            item.epgVideoName,
                            isDvrAvailable,
                            focusedEpgUID == item.uniqueId || epgItemHeight == 0,
                            isEpgListFocused,
                            epgListBorderMiddle.value,
                            isListStart,
                            isListEnd,
                            setEpgItemHeight
                        )
                    }
                }
            }
        }
    }
}