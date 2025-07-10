package com.example.iptvplayer.view.programDatePicker

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.iptvplayer.R
import com.example.iptvplayer.retrofit.data.ChannelData
import com.example.iptvplayer.view.archive.ArchiveViewModel
import com.example.iptvplayer.view.epg.EpgViewModel
import com.example.iptvplayer.view.media.MediaPlaybackViewModel
import com.example.iptvplayer.view.media.MediaViewModel
import com.example.iptvplayer.view.programDatePicker.datePicker.DayPicker
import com.example.iptvplayer.view.programDatePicker.timePicker.TimePicker
import com.example.iptvplayer.view.time.DateAndTimeViewModel

@Composable
fun ProgramDatePickerModal(
    modifier: Modifier,
    currentChannel: ChannelData,
    hideProgramDatePicker: () -> Unit,
    showChannelInfo: () -> Unit
) {
    val archiveViewModel: ArchiveViewModel = hiltViewModel()
    val mediaViewModel: MediaViewModel = hiltViewModel()
    val epgViewModel: EpgViewModel = hiltViewModel()
    val dateAndTimeViewModel: DateAndTimeViewModel = hiltViewModel()
    val mediaPlaybackViewModel: MediaPlaybackViewModel = hiltViewModel()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val dvrRanges by archiveViewModel.currentChannelDvrRanges.collectAsState()
    val dvrFirstAndLastDays by archiveViewModel.dvrFirstAndLastDay.collectAsState()
    val dvrFirstAndLastMonths by archiveViewModel.dvrFirstAndLastMonth.collectAsState()

    var isDatePickerFocused by remember { mutableStateOf(true) }

    var chosenDateSinceEpoch by remember { mutableLongStateOf(0) }
    var chosenTimeSinceEpoch by remember { mutableLongStateOf(0) }

    LaunchedEffect(chosenDateSinceEpoch, chosenTimeSinceEpoch) {
        val totalDateSinceEpoch = chosenDateSinceEpoch + chosenTimeSinceEpoch
        Log.i("chosen time since epoch", "total time: $totalDateSinceEpoch date: $chosenDateSinceEpoch time $chosenTimeSinceEpoch" )
        //Log.i("chosen time since epoch", dateAndTimeViewModel.formatDate(totalDateSinceEpoch, "EEEE d MMMM HH:mm:ss"))
    }

    val onArchiveSearchLocal: () -> Unit = {
        val newTotalDate = chosenDateSinceEpoch + chosenTimeSinceEpoch
        val firstDvrRange = dvrRanges[0]
        val lastDvrRange = dvrRanges[dvrRanges.size-1]
        if (newTotalDate >= firstDvrRange.from && newTotalDate <= lastDvrRange.from + lastDvrRange.duration) {
            mediaViewModel.updateCurrentTime(newTotalDate)
            mediaViewModel.updateIsLive(false)
            epgViewModel.searchEpgByTime(newTotalDate)
            archiveViewModel.getArchiveUrl(currentChannel.channelUrl, newTotalDate)
            hideProgramDatePicker()
            showChannelInfo()
            mediaPlaybackViewModel.startPlayback()
        } else {
            Toast.makeText(context, context.getString(R.string.date_is_not_available), Toast.LENGTH_LONG)
                .show()
        }
    }

    val requestCurrentTime: () -> Long = {
        dateAndTimeViewModel.currentTime.value
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
                dvrFirstAndLastDays,
                dvrFirstAndLastMonths,
                onArchiveSearchLocal,
                { date -> chosenDateSinceEpoch = date}
            ) {
                isDatePickerFocused = false
            }

            TimePicker(
                Modifier.fillMaxWidth(0.7f),
                requestCurrentTime,
                !isDatePickerFocused,
                chosenDateSinceEpoch,
                onArchiveSearchLocal,
                { time -> chosenTimeSinceEpoch = time }
            ) {
                isDatePickerFocused = true
            }
        }
    }
}