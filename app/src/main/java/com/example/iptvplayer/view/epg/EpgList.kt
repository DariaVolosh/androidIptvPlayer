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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
    setEpgItemHeight: (Int) -> Unit,
    setBorderYOffset: (Int) -> Unit,
) {
    val localDensity = LocalDensity.current.density
    val listState = rememberLazyListState()

    val epgViewModel: EpgViewModel = hiltViewModel()

    val epgList by epgViewModel.epgList.collectAsState()
    val dateMap by epgViewModel.dateMap.collectAsState()
    val focusedEpgIndex by epgViewModel.focusedEpgIndex.collectAsState()

    var isInitScrollPerformed by remember { mutableStateOf(false) }
    var visibleEpgItems by remember { mutableIntStateOf(0) }
    var epgListHeight by remember { mutableIntStateOf(0) }
    var epgDateHeight by remember { mutableIntStateOf(0) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.size }
            .collect { size ->
                if (size != 0) {
                    visibleEpgItems = size
                }
            }
    }

    LaunchedEffect(
        focusedEpgIndex,
        epgList,
        epgItemHeight,
        epgListHeight,
        epgDateHeight,
        visibleEpgItems
    ) {
        if (epgList.isEmpty()) {
            isInitScrollPerformed = false
            return@LaunchedEffect
        }

        if (
            focusedEpgIndex != -1 &&
            epgItemHeight != 0 &&
            epgListHeight != 0 &&
            epgDateHeight != 0 &&
            visibleEpgItems != 0
        ) {
            Log.i("epg list and border", "$visibleEpgItems")
            Log.i("epg list and border", "condition ${focusedEpgIndex} ${visibleEpgItems / 2}")

            if (focusedEpgIndex < visibleEpgItems / 2) {
                val isDateDisplayed = dateMap.keys.any { dateIndex ->
                    dateIndex in 0.. focusedEpgIndex
                }
                Log.i("epg list and border", "is date displayed $isDateDisplayed")
                var borderYOffset = (15 * localDensity + focusedEpgIndex * epgItemHeight + focusedEpgIndex * 7 * localDensity).toInt()
                if (isDateDisplayed) borderYOffset += (epgDateHeight + 7 * localDensity).toInt()
                setBorderYOffset(borderYOffset)
                Log.i("epg list and border", "border y offset $borderYOffset")

            } else if (focusedEpgIndex >= epgList.size - visibleEpgItems / 2) {

            } else {
                val isDateDisplayed = dateMap.keys.any { dateIndex ->
                    dateIndex in focusedEpgIndex-visibleEpgItems/2..focusedEpgIndex
                }

                Log.i("epg list", "epg list height $epgListHeight")
                Log.i("epg list", "epg item height $epgItemHeight")
                Log.i("epg list", "epg date height $epgDateHeight")

                val borderYOffset = (epgListHeight / 2 - epgItemHeight / 2)
                var scrollOffset = -borderYOffset + 15 * localDensity
                if (isDateDisplayed) scrollOffset += epgDateHeight

                setBorderYOffset(borderYOffset)

                if (!isInitScrollPerformed) {
                    listState.scrollToItem(focusedEpgIndex, scrollOffset.toInt())
                    isInitScrollPerformed = true
                } else {
                    listState.animateScrollToItem(focusedEpgIndex, scrollOffset.toInt())
                }
            }
        }
    }
    Log.i("vertical distance between items", (7 * localDensity).toString())
    Log.i("top padding", (15 * localDensity).toString())

    Log.i("epg list recomposed", "${epgList.toString()}")

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
                        index,
                        item.epgVideoTimeRange,
                        item.epgVideoName,
                        isDvrAvailable,
                        setEpgItemHeight
                    )
                }
            }
        }
    }
}