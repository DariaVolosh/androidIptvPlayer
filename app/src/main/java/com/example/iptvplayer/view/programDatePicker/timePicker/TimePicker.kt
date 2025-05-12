package com.example.iptvplayer.view.programDatePicker.timePicker

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.iptvplayer.data.Utils
import java.util.Calendar

@Composable
fun TimePicker(
    modifier: Modifier,
    requestCurrentTime: () -> Long,
    isFocused: Boolean,
    chosenDateSinceEpoch: Long,
    onArchiveSearch: () -> Unit,
    onTimeChanged: (Long) -> Unit,
    onDatePickerFocus: () -> Unit
) {
    var hours by remember {
        mutableIntStateOf(
            Utils.getCalendar(requestCurrentTime()).get(Calendar.HOUR_OF_DAY)
        )
    }

    var minutes by remember {
        mutableIntStateOf(
            Utils.getCalendar(requestCurrentTime()).get(Calendar.MINUTE)
        )
    }

    val hoursFocusRequester = remember { FocusRequester() }
    val minutesFocusRequester = remember { FocusRequester() }

    val getHoursInSeconds: (Int) -> Int = { hours ->
        val res = hours * 3600
        Log.i("get hours in seconds fun", "$res $hours")
        res
    }

    val getMinutesInSeconds: (Int) -> Int = { minutes ->
        val res = minutes * 60
        Log.i("get minutes in seconds fun", "$res $minutes")
        res
    }

    val getTotalTime: (Int, Int) -> Int = { hours, minutes ->
        Log.i("get total date since epoch fun", "hours: $hours minutes: $minutes")
        val res = getHoursInSeconds(hours) + getMinutesInSeconds(minutes)
        Log.i("get total date since epoch fun", res.toString())
        res
    }

    LaunchedEffect(hours, minutes) {
        val totalTimeSinceEpoch = getTotalTime(hours, minutes)
        onTimeChanged(totalTimeSinceEpoch.toLong())
    }

    LaunchedEffect(isFocused) {
        if (isFocused) hoursFocusRequester.requestFocus()
    }


    val hoursControlKeyEventHandler: (Key) -> Unit = { key ->
        when (key) {
            Key.DirectionLeft -> onDatePickerFocus()
            Key.DirectionRight -> minutesFocusRequester.requestFocus()
            Key.DirectionUp -> {
                Log.i("direction up", "fired")

                Log.i("hours", hours.toString())
                val newHour = if (hours < 23) hours + 1 else 0

                Log.i("new hour and minutes", "$newHour $hours $minutes")
                Log.i("total date", chosenDateSinceEpoch.toString())
                hours = newHour
            }
            Key.DirectionDown -> {
                val newHour = if (hours > 0) hours - 1 else 23
                hours = newHour
            }
            Key.DirectionCenter -> {
                onArchiveSearch()
            }
        }
    }

    val minutesControlKeyEventHandler: (Key) -> Unit = { key ->
        when (key) {
            Key.DirectionLeft -> hoursFocusRequester.requestFocus()
            Key.DirectionUp -> {
                val newMinutes = if (minutes < 59) minutes + 1 else 0
                minutes = newMinutes
            }
            Key.DirectionDown -> {
                val newMinutes = if (minutes > 0) minutes - 1 else 59
                minutes = newMinutes
            }
            Key.DirectionCenter -> {
                onArchiveSearch()
            }
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        TimeControl(
            Modifier.weight(1f),
            if (hours < 10) "0$hours" else "$hours",
            hoursFocusRequester,
            hoursControlKeyEventHandler
        )

        Text(
            text = ":",
            fontSize = 30.sp,
            color = MaterialTheme.colorScheme.onSecondary
        )

        TimeControl(
            Modifier.weight(1f),
            if (minutes < 10) "0$minutes" else "$minutes",
            minutesFocusRequester,
            minutesControlKeyEventHandler
        )
    }
}