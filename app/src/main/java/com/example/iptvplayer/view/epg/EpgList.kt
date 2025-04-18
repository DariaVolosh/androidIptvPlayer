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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.iptvplayer.data.Utils
import com.example.iptvplayer.data.Utils.formatDate
import kotlinx.coroutines.launch

@Composable
fun EpgList(
    modifier: Modifier,
    isChannelClicked: Boolean,
) {
    val localDensity = LocalDensity.current.density
    val epgViewModel: EpgViewModel = hiltViewModel()

    val epgList by epgViewModel.epgList.observeAsState()
    val focusedEpgIndex by epgViewModel.focusedEpgIndex.observeAsState()
    val isFocused by epgViewModel.isEpgListFocused.observeAsState()

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    var visibleItems by remember { mutableIntStateOf(0) }
    var isVisibleItemsMiddle by remember { mutableStateOf(false) }
    var isListEnd by remember { mutableStateOf(false) }

    var epgItemHeight by remember { mutableIntStateOf(0) }
    var epgDateHeight by remember { mutableIntStateOf(0) }

    var borderYOffset by remember { mutableIntStateOf(0) }
    var epgIndexWithDateIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(epgList) {
        Log.i("EPGLIST", epgList.toString() )
        visibleItems = listState.layoutInfo.visibleItemsInfo.size
        Log.i("VISIBLE EPG PROGRAMMES", visibleItems.toString())
    }

    LaunchedEffect(focusedEpgIndex, isChannelClicked) {
        Log.i("scroll performed", "$focusedEpgIndex")
        val localEpgList = epgList
        val localFocusedEpgIndex = focusedEpgIndex
        if (!isChannelClicked && localEpgList != null && localFocusedEpgIndex != null) {

            // end of the list is reached, focus has to be moved down
            if (localFocusedEpgIndex >= localEpgList.size - visibleItems / 2) {
                isListEnd = true
                isVisibleItemsMiddle = false
                coroutineScope.launch {
                    listState.scrollToItem(localEpgList.size - 1)
                }
            } else if (localFocusedEpgIndex >= visibleItems / 2) {
                isVisibleItemsMiddle = true
                isListEnd = false

                coroutineScope.launch {
                    if (localFocusedEpgIndex == epgIndexWithDateIndex) {
                        listState.scrollToItem(localFocusedEpgIndex, -borderYOffset + ((15 + epgDateHeight) * localDensity).toInt())
                    } else {
                        listState.scrollToItem(localFocusedEpgIndex, -borderYOffset + (15 * localDensity).toInt())
                    }
                }
            } else {
                isVisibleItemsMiddle = false

                coroutineScope.launch {
                    listState.scrollToItem(0)
                }
            }
        }
    }

    LaunchedEffect(isFocused, isChannelClicked) {
        if (!isChannelClicked) {
            if (isFocused == true) focusRequester.requestFocus()
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
                    .background(MaterialTheme.colorScheme.secondary.copy(0.8f))
                    .onGloballyPositioned { cords ->
                        Log.i("EPG LIST HEIGHT", cords.size.toString())
                    }
                    .focusRequester(focusRequester)
                    .focusable(true)
                    .onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) {
                            epgViewModel.handleEpgOnKeyEvent(event.key)
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

                items(epgList?.size ?: 0, {index -> epgList?.get(index)?.startTime ?: ""}) { index ->
                    Log.i("epg size", epgList?.size.toString())
                    val epgItem = epgList?.get(index)

                    epgList?.get(index)?.let { item ->
                        val currentDate = formatDate(item.startTime, dayAndMonthPattern)

                        val prevDate = if (index != 0) formatDate(epgList?.get(index-1)?.startTime ?: 0L, dayAndMonthPattern) else ""

                        // compare days, if current day is more than the previous one - add date
                        if (index == 0 || Utils.compareDates(currentDate, prevDate) > 0) {
                            EpgDate(currentDate) { height ->
                                epgDateHeight = height
                            }

                            epgIndexWithDateIndex = index
                        }

                        Log.i("EPGITEM", "${index == focusedEpgIndex} ${item.title}")

                        EpgItem(
                            formatDate(item.startTime, timePattern),
                            formatDate(item.stopTime, timePattern),
                            item.title,
                            index == focusedEpgIndex && isFocused == true,
                            item.isDvrAvailable
                        ) { height -> epgItemHeight = height }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .offset {
                        val localFocusedEpgIndex = focusedEpgIndex

                        if (!isVisibleItemsMiddle && localFocusedEpgIndex != null) {
                            Log.i("epg scroll performed", "$focusedEpgIndex $epgIndexWithDateIndex")

                            if (!isListEnd) {
                                IntOffset(
                                    0, ((15 + localFocusedEpgIndex * epgItemHeight + localFocusedEpgIndex * 7 + epgItemHeight) * localDensity).toInt()
                                )
                            } else {
                                val localEpgList = epgList

                                if (localEpgList != null) {
                                    val itemsFromBottom = localEpgList.size - localFocusedEpgIndex - 1
                                    IntOffset(
                                        0, ((-15 - itemsFromBottom * epgItemHeight - itemsFromBottom * 7) * localDensity).toInt()
                                    )
                                } else {
                                    IntOffset(0,0)
                                }
                            }
                        } else {
                            IntOffset(0,0)
                        }
                    }
                    .alpha(if (isFocused == false) 0f else 1f)
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
                    .padding(horizontal = 15.dp)
                    .height(epgItemHeight.dp)
                    .border(1.dp, Color.White)
                    .onGloballyPositioned { cords ->
                        borderYOffset = cords.positionInParent().y.toInt()
                        Log.i("epg list y coordinate", borderYOffset.toString())
                    }
            )
        }
    }
}