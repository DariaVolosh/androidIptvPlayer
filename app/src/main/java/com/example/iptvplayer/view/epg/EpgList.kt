package com.example.iptvplayer.view.epg

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.iptvplayer.data.Epg
import com.example.iptvplayer.data.Utils
import com.example.iptvplayer.data.Utils.formatDate
import kotlinx.coroutines.launch

@Composable
fun EpgList(
    modifier: Modifier,
    epg: List<Epg>,
    focusedEpg: Int,
    isListFocused: Boolean,
    isChannelClicked: Boolean,
    epgOnKeyEvent: (Key) -> Unit
) {

    val localDensity = LocalDensity.current.density

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    var visibleItems by remember { mutableIntStateOf(0) }
    var isVisibleItemsMiddle by remember { mutableStateOf(false) }
    var itemHeight by remember { mutableIntStateOf(0) }
    var borderYOffset by remember { mutableIntStateOf(0) }

    LaunchedEffect(epg) {
        Log.i("EPGLIST", epg.toString() )
        Log.i("EPGLIST", epg.size.toString())
        visibleItems = listState.layoutInfo.visibleItemsInfo.size
        Log.i("VISIBLE EPG PROGRAMMES", visibleItems.toString())
    }

    LaunchedEffect(focusedEpg, isChannelClicked) {
        if (!isChannelClicked) {
            if (focusedEpg >= visibleItems / 2) {
                isVisibleItemsMiddle = true

                Log.i("epg scroll performed", "$focusedEpg $borderYOffset")
                coroutineScope.launch {
                    listState.scrollToItem(focusedEpg, -borderYOffset + (15 * localDensity).toInt())
                }
            } else {
                isVisibleItemsMiddle = false
                coroutineScope.launch {
                    listState.scrollToItem(0)
                }
            }
        }
    }

    LaunchedEffect(isListFocused, isChannelClicked) {
        if (!isChannelClicked) {
            if (isListFocused) focusRequester.requestFocus()
            else focusRequester.freeFocus()
        }
    }

    if (!isChannelClicked) {
        Box(
            modifier = modifier
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.secondary.copy(0.55f))
                    .onGloballyPositioned { cords ->
                        Log.i("EPG LIST HEIGHT", cords.size.toString())
                    }
                    .focusRequester(focusRequester)
                    .focusable(true)
                    .onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) {
                            epgOnKeyEvent(event.key)
                        }

                        true
                    }
                    .padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
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
                        epgItem.isDvrAvailable
                    ) { height -> itemHeight = height }
                }
            }

            Box(
                modifier = Modifier
                    .offset {
                        if (!isVisibleItemsMiddle) {
                            IntOffset(0, ((15 + focusedEpg * itemHeight + focusedEpg * 7) * localDensity).toInt())
                        } else {
                            IntOffset(0,0)
                        }
                    }
                    .alpha(if (!isListFocused) 0f else 1f)
                    .align(if (isVisibleItemsMiddle) Alignment.Center else Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(itemHeight.dp)
                    .border(1.dp, Color.White)
                    .onGloballyPositioned { cords ->
                        borderYOffset = cords.positionInParent().y.toInt()
                        Log.i("epg list y coordinate", borderYOffset.toString())
                    }
            )
        }
    }
}