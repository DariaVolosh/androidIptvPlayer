package com.example.iptvplayer.view.epg

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
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
    epgOnKeyEvent: (Key) -> Unit
) {

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(epg) {
        Log.i("EPGLIST", epg.toString() )
        Log.i("EPGLIST", epg.size.toString())
    }

    LaunchedEffect(focusedEpg) {
        Log.i("FOCUSED_EPG", focusedEpg.toString())
    }

    LaunchedEffect(focusedEpg) {
        if (focusedEpg >= 0) {
            coroutineScope.launch {
                listState.animateScrollToItem(focusedEpg)
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .background(MaterialTheme.colorScheme.secondary.copy(0.4f))
            .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(17.dp),
        state = listState
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

            Log.i("EPGITEM", "${epgItem.startTime} ${epgItem.title}")

            EpgItem(
                formatDate(epgItem.startTime, timePattern),
                formatDate(epgItem.stopTime, timePattern),
                epgItem.title,
                index == focusedEpg,
                {key -> epgOnKeyEvent(key)}
            )
        }
    }
}