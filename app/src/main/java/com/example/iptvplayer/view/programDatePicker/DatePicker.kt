package com.example.iptvplayer.view.programDatePicker

import androidx.compose.foundation.border
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.iptvplayer.data.Utils
import com.example.iptvplayer.view.channels.ArchiveViewModel

@Composable
fun DatePicker(
    modifier: Modifier,
    currentTime: Long,
    isFocused: Boolean,
    onDateChanged: (Long) -> Unit,
    onTimePickerFocus: () -> Unit
) {
    val archiveViewModel: ArchiveViewModel = hiltViewModel()

    var daysOfWeek by remember { mutableStateOf<List<String>>(mutableListOf())}

    val dvrMonth by archiveViewModel.dvrMonth.observeAsState()
    val dvrFirstAndLastDay by archiveViewModel.dvrFirstAndLastDay.observeAsState()

    LaunchedEffect(Unit) {
        daysOfWeek = Utils.getAllWeekdays()
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        dvrMonth?.let { dvrMonth ->
            Text(
                modifier = Modifier
                    .padding(bottom = 11.dp),
                text = "${Utils.getFullMonthName(dvrMonth)} 2025",
                color = MaterialTheme.colorScheme.onSecondary,
                fontSize = 16.sp
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.onSecondary)
                .padding(vertical = 6.dp)
        ) {
            daysOfWeek.map { dayOfWeek ->
                Text(
                    modifier = Modifier
                        .weight(1f),
                    text = dayOfWeek,
                    color = MaterialTheme.colorScheme.onSecondary,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        EpgAvailableDays(
            dvrMonth ?: -1,
            dvrFirstAndLastDay ?: Pair(0,0),
            daysOfWeek,
            isFocused,
            currentTime,
            onDateChanged,
            onTimePickerFocus
        )
    }
}