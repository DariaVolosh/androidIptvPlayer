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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
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
    dvrRange: Pair<Long, Long>,
    isChannelClicked: Boolean,
) {
    val localDensity = LocalDensity.current.density
    val epgViewModel: EpgViewModel = hiltViewModel()

    //val epgList by epgViewModel.epgList.observeAsState()
    //val focusedEpgIndex by epgViewModel.focusedEpgIndex.observeAsState()
    val isFocused by epgViewModel.isEpgListFocused.observeAsState()

    val epgList by epgViewModel.epgListFlow.collectAsState(emptyList())
    val focusedEpgIndex by epgViewModel.focusedEpgIndexFlow.collectAsState(-1)

    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    val listState = rememberLazyListState()
    val visibleItems by remember {
        derivedStateOf { listState.layoutInfo.visibleItemsInfo.size }
    }

    var isVisibleItemsMiddle by remember { mutableStateOf(false) }
    var isListEnd by remember { mutableStateOf(false) }
    var isListDisplayed by remember { mutableStateOf(false) }

    var epgItemHeight by remember { mutableIntStateOf(0) }
    var epgDateHeight by remember { mutableIntStateOf(0) }

    var borderY by remember { mutableIntStateOf(0) }
    var borderOffset by remember { mutableStateOf(IntOffset(0,0)) }
    var epgIndexWithDateIndex by remember { mutableIntStateOf(0) }

    var itemsAlpha by remember { mutableFloatStateOf(0f) }

    val scrollToItem: (onBorderDisplay: () -> Unit) -> Unit = { onBorderDisplay ->
        if (isVisibleItemsMiddle) {
            coroutineScope.launch {
                Log.i("scrolled", "$focusedEpgIndex")
                Log.i("scroll info", "$focusedEpgIndex ${-borderY}")
                listState.scrollToItem(focusedEpgIndex, -borderY + (15 * localDensity).toInt())
                onBorderDisplay()
            }
        } else if (isListEnd) {
            coroutineScope.launch {
                listState.scrollToItem(epgList.size - 1)
                onBorderDisplay()
            }
        } else {
            coroutineScope.launch {
                listState.scrollToItem(0)
                onBorderDisplay()
            }
        }
    }

   LaunchedEffect(isFocused, isChannelClicked, isListDisplayed) {
        if (!isChannelClicked && isListDisplayed) {
            if (isFocused == true) focusRequester.requestFocus()
            else focusRequester.freeFocus()
        }
    }

    LaunchedEffect(focusedEpgIndex, epgList, visibleItems) {
        if (focusedEpgIndex != -1 && epgList.isNotEmpty()) {
            isListDisplayed = true

            if (visibleItems != 0) {
                Log.i("first visible item", visibleItems.toString())
                Log.i("focused epg index and list", "$focusedEpgIndex ${epgList.size}")
                Log.i("focus data", "$focusedEpgIndex >= ${epgList.size} - ${visibleItems / 2}")

                // end of the list is reached, focus has to be moved down
                if (focusedEpgIndex >= epgList.size - visibleItems / 2) {
                    isListEnd = true
                    isVisibleItemsMiddle = false

                    val setListEndBorder: () -> Unit = {
                        val itemsFromBottom = epgList.size - focusedEpgIndex - 1
                        borderOffset = IntOffset(
                            0, ((-15 - itemsFromBottom * epgItemHeight - itemsFromBottom * 7) * localDensity).toInt()
                        )
                    }

                    Log.i("is focused ", "$isFocused")

                    if (isFocused == false) scrollToItem(setListEndBorder)
                    else setListEndBorder()

                } else if (focusedEpgIndex >= visibleItems / 2) {
                    isListEnd = false
                    isVisibleItemsMiddle = true

                    val setListCenterBorder: () -> Unit = {
                        borderOffset = IntOffset(0,0)
                    }

                    if (isFocused == false) scrollToItem(setListCenterBorder)
                    else setListCenterBorder()
                } else {
                    isVisibleItemsMiddle = false

                    val setListEndBorder: () -> Unit = {
                        borderOffset = IntOffset(
                            0, ((15 + focusedEpgIndex * epgItemHeight + focusedEpgIndex * 7 + epgItemHeight) * localDensity).toInt()
                        )
                    }

                    if (isFocused == false) scrollToItem(setListEndBorder)
                    else setListEndBorder()
                }

                itemsAlpha = 1f
            }
        } else {
            isListDisplayed = false
            itemsAlpha = 0f
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
                           epgViewModel.handleEpgOnKeyEvent(event.key, dvrRange)
                       }

                       true
                   }
                   .padding(15.dp),
               verticalArrangement = Arrangement.spacedBy(7.dp),
               state = listState,
               userScrollEnabled = false
           ) {
               if (isListDisplayed) {
                   val dayAndMonthPattern = "dd MMMM"
                   Log.i("list state", listState.firstVisibleItemIndex.toString())

                   items(epgList.size, {index -> epgList[index].start}) { index ->
                       Log.i("epg size", epgList.size.toString())
                       epgList[index].let { item ->
                           Log.i("epg index", index.toString() + "$item")

                           val currentDate = formatDate(item.startSeconds, dayAndMonthPattern)

                           val prevDate =
                               if (index != 0) formatDate(epgList[index-1].startSeconds, dayAndMonthPattern)
                               else ""

                           // compare days, if current day is more than the previous one - add date
                           if (index == 0 || Utils.compareDates(currentDate, prevDate) > 0) {
                               EpgDate(
                                   Modifier.alpha(itemsAlpha),
                                   currentDate
                               ) { height ->
                                   epgDateHeight = height
                               }

                               epgIndexWithDateIndex = index
                           }

                           val isDvrAvailable = item.startSeconds in dvrRange.first..dvrRange.second

                           EpgItem(
                               Modifier.alpha(itemsAlpha),
                               item.epgVideoTimeRange,
                               item.epgVideoName,
                               index == focusedEpgIndex && isFocused == true,
                               isDvrAvailable,//item.isDvrAvailable
                           ) { height -> epgItemHeight = height }
                       }
                   }
               }
           }

           Box(
               modifier = Modifier
                   .offset { borderOffset }
                   .alpha(if (isFocused == false) 0f else 1f)
                   .align(
                       if (isVisibleItemsMiddle) Alignment.Center
                       else {
                           if (isListEnd) Alignment.BottomCenter
                           else Alignment.TopCenter
                       }
                   )
                   .fillMaxWidth()
                   .padding(horizontal = 15.dp)
                   .height(epgItemHeight.dp)
                   .border(1.dp, Color.White)
                   .onGloballyPositioned { cords ->
                       borderY = cords.positionInParent().y.toInt()
                       if (focusedEpgIndex != -1 && epgList.isNotEmpty()) scrollToItem {}
                   }
           )
       }
   }
}