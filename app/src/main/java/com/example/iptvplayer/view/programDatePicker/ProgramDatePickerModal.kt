package com.example.iptvplayer.view.programDatePicker

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.iptvplayer.data.Utils
import com.example.iptvplayer.view.channels.ArchiveViewModel

// passing live time from the parent composable only one time
// to avoid unnecessary recompositions of this composable
@Composable
fun ProgramDatePickerModal(
    modifier: Modifier,
    currentTime: Long,
    onArchiveSearch: (Long) -> Unit
) {
    val archiveViewModel: ArchiveViewModel = hiltViewModel()

    val dvrRange by archiveViewModel.dvrRange.observeAsState()
    val dvrFirstAndLastDays by archiveViewModel.dvrFirstAndLastDay.observeAsState()
    val dvrFirstAndLastMonths by archiveViewModel.dvrFirstAndLastMonth.observeAsState()

    var isDatePickerFocused by remember { mutableStateOf(true) }

    var chosenDateSinceEpoch by remember { mutableLongStateOf(0) }
    var chosenTimeSinceEpoch by remember { mutableLongStateOf(0) }

    LaunchedEffect(chosenDateSinceEpoch, chosenTimeSinceEpoch) {
        val totalDateSinceEpoch = chosenDateSinceEpoch + chosenTimeSinceEpoch
        Log.i("chosen time since epoch", "total time: $totalDateSinceEpoch date: $chosenDateSinceEpoch time $chosenTimeSinceEpoch" )
        Log.i("chosen time since epoch", Utils.formatDate(totalDateSinceEpoch, "EEEE d MMMM HH:mm:ss"))
    }


    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.secondary.copy(0.8f))
            .padding(25.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DayPicker(
                Modifier.fillMaxWidth(0.5f),
                isDatePickerFocused,
                dvrFirstAndLastDays ?: Pair(0,0),
                dvrFirstAndLastMonths ?: Pair(0,0),
                { onArchiveSearch(chosenDateSinceEpoch + chosenTimeSinceEpoch) },
                { date -> chosenDateSinceEpoch = date}
            ) {
                isDatePickerFocused = false
            }

            TimePicker(
                Modifier.fillMaxWidth(0.7f),
                currentTime,
                !isDatePickerFocused,
                dvrRange ?: Pair(0,0),
                chosenDateSinceEpoch,
                { onArchiveSearch(chosenDateSinceEpoch + chosenTimeSinceEpoch) },
                { time -> chosenTimeSinceEpoch = time }
            ) {
                isDatePickerFocused = true
            }
        }
    }
}