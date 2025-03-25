package com.example.iptvplayer.view.epg

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.iptvplayer.data.Epg
import com.example.iptvplayer.data.Utils
import com.example.iptvplayer.data.Utils.formatDate
import com.example.iptvplayer.view.channels.ArchiveViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
fun EpgList(
    modifier: Modifier,
    epg: List<Epg>,
    focusedEpg: Int,
    isListFocused: Boolean,
    epgOnKeyEvent: (Key) -> Unit
) {
    val archiveViewModel: ArchiveViewModel = hiltViewModel()

    val liveTime by archiveViewModel.liveTime.observeAsState()

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var visibleItems by remember { mutableIntStateOf(0) }
    var itemHeight by remember { mutableIntStateOf(0) }

    var borderYOffset by remember { mutableIntStateOf(0) }

    LaunchedEffect(epg) {
        Log.i("EPGLIST", epg.toString() )
        Log.i("EPGLIST", epg.size.toString())
        visibleItems = listState.layoutInfo.visibleItemsInfo.size
        Log.i("VISIBLE EPG PROGRAMMES", visibleItems.toString())
    }


    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged() // Only emit distinct values
            .collectLatest { isScrolling ->
                if (isScrolling) {
                    println("Scrolling started")
                } else {
                    println("Scrolling stopped")
                }
            }
    }

    LaunchedEffect(focusedEpg, isListFocused) {
        Log.i("focused epg", focusedEpg.toString())
        if (focusedEpg != -1 && isListFocused) {
            listState.scrollToItem(focusedEpg, -borderYOffset + 31)
        }
    }

    Box(
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.secondary.copy(0.4f))
                .onGloballyPositioned { cords ->
                    Log.i("EPG LIST HEIGHT", cords.size.toString())
                }
                .padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(17.dp),
            state = listState,
            userScrollEnabled = false
        ) {
            val dayAndMonthPattern = "dd MMMM"
            val timePattern = "HH:mm"

            items(epg.size, {index -> epg[index].startTime}) { index ->
                val epgItem = epg[index]
                val currentDate = formatDate(epgItem.startTime, dayAndMonthPattern)

                val prevDate = if (index != 0) formatDate(epg[index-1].startTime, dayAndMonthPattern) else ""

                if (index == 0 || Utils.compareDates(currentDate, prevDate) > 0) {
                    EpgDate(currentDate)
                }

                Log.i("EPGITEM", "${index == focusedEpg} ${epgItem.title}")

                EpgItem(
                    formatDate(epgItem.startTime, timePattern),
                    formatDate(epgItem.stopTime, timePattern),
                    epgItem.title,
                    index == focusedEpg && isListFocused,
                    epgItem.isDvrAvailable,
                    { height -> itemHeight = height },
                    {
                        coroutineScope.launch {
                            // providing delay to make layout before scrolling stable (debounce focus request)
                            // i do not know why it works without delay for channelsList but i am tired of
                            // debugging and that is why i leave delay here (i am sorry)
                            delay(30)

                            listState.scrollToItem(focusedEpg, -borderYOffset + 31)
                        }
                    }
                ) {key -> epgOnKeyEvent(key)}
            }
        }

        if (focusedEpg >= visibleItems / 2 && isListFocused) {
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