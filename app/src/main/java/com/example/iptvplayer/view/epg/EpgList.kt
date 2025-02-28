package com.example.iptvplayer.view.epg

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.unit.dp
import com.example.iptvplayer.data.Epg
import com.example.iptvplayer.data.Utils

@Composable
fun EpgList(
    modifier: Modifier,
    epg: List<Epg>,
    focusedEpg: Int,
    epgOnKeyEvent: (Key) -> Unit
) {

    LaunchedEffect(epg) {
        Log.i("EPGLIST", epg.toString() )
    }

    LaunchedEffect(focusedEpg) {
        Log.i("FOCUSED_EPG", focusedEpg.toString())
    }

    LazyColumn(
        modifier = modifier
            .background(MaterialTheme.colorScheme.secondary.copy(0.4f))
            .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(17.dp)
    ) {
        items(epg.size, {index -> epg[index].startTime}) { index ->
            val epgItem = epg[index]
            val currentDate = epgItem.startTime.substring(
                0,
                epgItem.startTime.indexOf(
                    " ", epgItem.startTime.indexOf(" ") + 1
                )
            )

            val prevDate = if (index != 0) {
                epg[index-1].startTime.substring(
                    0,
                    epg[index-1].startTime.indexOf(
                        " ", epg[index-1].startTime.indexOf(" ") + 1
                    )
                )
            } else {
                ""
            }

            if (index == 0 || Utils.compareDates(currentDate, prevDate) > 0) {
                EpgDate(currentDate)
            }

            EpgItem(
                epgItem.startTime.substring(epgItem.startTime.lastIndexOf(" ") + 1, epgItem.startTime.length),
                epgItem.stopTime.substring(epgItem.stopTime.lastIndexOf(" ") + 1, epgItem.stopTime.length),
                epgItem.title,
                index == focusedEpg,
                {key -> epgOnKeyEvent(key)}
            )
        }
    }
}