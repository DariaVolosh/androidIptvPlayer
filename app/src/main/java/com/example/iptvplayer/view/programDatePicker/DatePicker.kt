package com.example.iptvplayer.view.programDatePicker

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.iptvplayer.data.Utils
import com.example.iptvplayer.view.channels.ArchiveViewModel
import com.example.iptvplayer.view.epg.EpgViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DatePicker(
    modifier: Modifier
) {
    val epgViewModel: EpgViewModel = hiltViewModel()
    val archiveViewModel: ArchiveViewModel = hiltViewModel()

    val firstAndLastEpgDay by epgViewModel.firstAndLastEpgDay.observeAsState()
    val epgMonth by epgViewModel.epgMonth.observeAsState()

    var daysOfWeek by remember { mutableStateOf<List<String>>(mutableListOf())}
    var focusedDay by remember { mutableIntStateOf(0) }
    var daysFocusRequesters = remember { mutableStateOf<List<FocusRequester>>(mutableListOf()) }

    LaunchedEffect(Unit) {
        daysOfWeek = Utils.getAllWeekdays()
        val liveTime = archiveViewModel.liveTime.value
        liveTime?.let { liveTime ->
            focusedDay = Utils.getCalendarDay(Utils.getCalendar(liveTime))
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        epgMonth?.let { epgMonth ->
            Text(
                modifier = Modifier
                    .padding(bottom = 11.dp),
                text = "${Utils.getFullMonthName(epgMonth)} 2025",
                color = MaterialTheme.colorScheme.onSecondary,
                fontSize = 16.sp
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.onSecondary)
                .padding(6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            daysOfWeek.map { dayOfWeek ->
                Text(
                    modifier = Modifier.weight(1f),
                    text = dayOfWeek,
                    color = MaterialTheme.colorScheme.onSecondary,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        EpgAvailableDays(firstAndLastEpgDay, epgMonth, daysOfWeek)
    }
}