package com.example.iptvplayer.view.epg

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun EpgList(
    modifier: Modifier,
    dvrRange: Pair<Long, Long>,
    epgItemHeight: Int,
    isListMiddle: Boolean,
    setIsListMiddle: (Boolean) -> Unit,
    setEpgItemHeight: (Int) -> Unit,
    setBorderYOffset: (Int) -> Unit,
) {
    val localDensity = LocalDensity.current.density
    val listState = rememberLazyListState()

    val epgViewModel: EpgViewModel = hiltViewModel()

    val epgList by epgViewModel.epgList.collectAsState()
    val dateMap by epgViewModel.dateMap.collectAsState()
    val focusedEpgIndex by epgViewModel.focusedEpgIndex.collectAsState()
    val isEpgListFocused by epgViewModel.isEpgListFocused.collectAsState()

    var isInitScrollPerformed by remember { mutableStateOf(false) }
    var lastVisibleEpgItem by remember { mutableIntStateOf(0) }
    var epgListHeight by remember { mutableIntStateOf(0) }
    val epgListBorderMiddle = remember {
        derivedStateOf { epgListHeight / 2 - epgItemHeight / 2 }
    }
    var epgDateHeight by remember { mutableIntStateOf(0) }
    var isListStart by remember { mutableStateOf(false) }
    var isListEnd by remember { mutableStateOf(false) }

    LaunchedEffect(
        focusedEpgIndex,
        epgList,
        epgItemHeight,
        epgListHeight,
        epgDateHeight
    ) {
        if (epgList.isEmpty()) {
            isInitScrollPerformed = false
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
            // we should be oriented on border y offset, not on visible items, because
            // visible items vary based on sizes of items, but border y is stable

            val focusedItemYOffset = (15 * localDensity + focusedEpgIndex * epgItemHeight + focusedEpgIndex * 7 * localDensity).toInt()
            val lastElementYOffset = (15 * localDensity + (epgList.size - 1) * epgItemHeight + (epgList.size - 1) * 7 * localDensity).toInt()

            Log.i("focused item border y offset", "$focusedItemYOffset")
            Log.i("last item border y offset", "$lastElementYOffset")

            if (focusedItemYOffset < epgListBorderMiddle.value) {
                // list start
                Log.i("list position", "list start")
                listState.scrollToItem(0)
                setBorderYOffset(focusedItemYOffset)
                isListStart = true
                setIsListMiddle(false)
            } else if ((lastElementYOffset - focusedItemYOffset) < epgListBorderMiddle.value){
                // list end
                Log.i("difference", "${lastElementYOffset - focusedItemYOffset} < ${epgListBorderMiddle.value}")
                Log.i("focused epg index", "$focusedEpgIndex")
                Log.i("last visible epg item", "${lastVisibleEpgItem}")
                listState.scrollToItem(epgList.size - 1)
                isListEnd = true
                setIsListMiddle(false)
            } else {
                var scrollOffset = -epgListBorderMiddle.value + 15 * localDensity.toInt()
                if (dateMap.containsKey(focusedEpgIndex)) scrollOffset += epgDateHeight
                listState.scrollToItem(focusedEpgIndex, scrollOffset)
                setIsListMiddle(true)
                isListEnd = false
                isListStart = false
            }

            isInitScrollPerformed = true
        }
    }
    Log.i("vertical distance between items", (7 * localDensity).toString())
    Log.i("top padding", (15 * localDensity).toString())

    Log.i("epg list", "epg list height $epgListHeight")
    Log.i("epg list", "epg item height $epgItemHeight")
    Log.i("epg list", "epg date height $epgDateHeight")
    Log.i("epg list", "$epgList")

    Box(
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (isInitScrollPerformed) 1f else 0f)
                .padding(15.dp)
                .onGloballyPositioned { cords ->
                    epgListHeight = cords.size.height
                },
            verticalArrangement = Arrangement.spacedBy(7.dp),
            state = listState,
            userScrollEnabled = false
        ) {
            if (epgList.isNotEmpty() && dateMap.isNotEmpty()) {
                items(epgList.size, { index -> index }) { index ->
                    val item = epgList[index]
                    val date = dateMap[index]

                    val isDvrAvailable =
                        item.epgVideoTimeRangeSeconds.start in dvrRange.first..dvrRange.second

                    if (date != null) {
                        EpgDate(
                            index,
                            date
                        ) { height -> epgDateHeight = height}
                    }

                    EpgItem(
                        item.epgVideoTimeRange,
                        item.epgVideoName,
                        isDvrAvailable,
                        index == focusedEpgIndex || epgItemHeight == 0,
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