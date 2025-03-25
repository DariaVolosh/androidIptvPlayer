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
import kotlinx.coroutines.delay

// passing live time from the parent composable only one time
// to avoid unnecessary recompositions of this composable
@Composable
fun ProgramDatePickerModal(
    modifier: Modifier,
    currentTime: Long,
    onDateChanged: (Long) -> Unit
) {
    val archiveViewModel: ArchiveViewModel = hiltViewModel()
    val dvrRange by archiveViewModel.dvrRange.observeAsState()

    var isDatePickerFocused by remember { mutableStateOf(true) }
    var chosenDateSinceEpoch by remember { mutableLongStateOf(0) }
    var chosenTimeSinceEpoch by remember { mutableLongStateOf(0) }
    var isCurrentDateSet by remember { mutableStateOf(false) }

    LaunchedEffect(chosenDateSinceEpoch, chosenTimeSinceEpoch) {
        val totalDateSinceEpoch = chosenDateSinceEpoch + chosenTimeSinceEpoch
        Log.i("chosen time since epoch", "total time: $totalDateSinceEpoch date: $chosenDateSinceEpoch time $chosenTimeSinceEpoch" )
        Log.i("chosen time since epoch", Utils.formatDate(totalDateSinceEpoch, "EEEE d MMMM HH:mm:ss"))


        if (isCurrentDateSet) {
            // delay for 2 seconds if no changes in time, and then set the time and call callback
            // to reduce amount of operations if the change in time is fast
            delay(3000)
            onDateChanged(totalDateSinceEpoch)
        } else {
            // if this condition is true - it means that the initial time setup was
            // completed (current time is chosen) and we can call onDateChange callback
            if (chosenDateSinceEpoch != 0L && chosenTimeSinceEpoch != 0L) {
                isCurrentDateSet = true
            }
        }
    }

    LaunchedEffect(chosenDateSinceEpoch) {
        delay(1000)
        isDatePickerFocused = false
    }


    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.secondary.copy(0.8f))
            .padding(25.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DatePicker(
                Modifier.fillMaxWidth(0.6f),
                currentTime,
                isDatePickerFocused,
                { date -> chosenDateSinceEpoch = date }
            ) {
                isDatePickerFocused = false
            }

            TimePicker(
                Modifier.fillMaxWidth(0.8f),
                currentTime,
                !isDatePickerFocused,
                dvrRange ?: Pair(0,0),
                chosenDateSinceEpoch,
                { time -> chosenTimeSinceEpoch = time }
            ) {
                isDatePickerFocused = true
            }
        }
    }
}