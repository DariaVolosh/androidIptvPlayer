package com.example.iptvplayer.view.programDatePicker.datePicker

import android.util.Log
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.iptvplayer.data.Utils

@Composable
fun DayPicker(
    modifier: Modifier,
    isFocused: Boolean,
    dvrFirstAndLastDays: Pair<Int, Int>,
    dvrFirstAndLastMonths: Pair<Int, Int>,
    onArchiveSearch: () -> Unit,
    onDateChanged: (Long) -> Unit,
    onTimePickerFocus: () -> Unit
) {
    var availableDays by remember { mutableStateOf<List<ArchiveDate>>(emptyList()) }
    var focusedDay by remember { mutableIntStateOf(-1) }

    LaunchedEffect(focusedDay) {
        if (focusedDay != -1) {
            val currentDay = availableDays[focusedDay]
            val currentDayAndMonthSinceEpoch = Utils.dateToEpochSeconds(currentDay.day, currentDay.month, 2025, 0, 0)
            onDateChanged(currentDayAndMonthSinceEpoch)
        }
    }

    LaunchedEffect(dvrFirstAndLastDays, dvrFirstAndLastMonths) {
        if (dvrFirstAndLastDays.first != 0 && dvrFirstAndLastMonths.first != 0) {
            val days = mutableListOf<ArchiveDate>()

            // last available day in dvr (current day)
            var currentDay = dvrFirstAndLastDays.second

            // last available month in dvr (current month)
            var currentMonth = dvrFirstAndLastMonths.second

            days.add(ArchiveDate("Today", currentDay, currentMonth))

            while (currentDay != dvrFirstAndLastDays.first || currentMonth != dvrFirstAndLastMonths.first) {
                currentDay -= 1

                if (currentDay == 0) {
                    currentMonth -= 1
                    currentDay = Utils.getDaysOfMonth(currentMonth)
                }

                days.add(
                    ArchiveDate(
                        if (days.size == 1) "Yesterday" else Utils.getDayOfWeek(currentMonth, currentDay),
                        currentDay,
                        currentMonth
                    )
                )
            }

            availableDays = days
            focusedDay = 0
        }
    }

    LazyColumn(
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.onSecondary)
    ) {
        items(availableDays.size, key = {i: Int ->
            "${availableDays[i].day} ${availableDays[i].month}"}
        ) { index: Int ->
            ArchiveDate(
                if (focusedDay == index && !isFocused) Modifier.border(1.dp, MaterialTheme.colorScheme.onSecondary)
                else Modifier,
                availableDays[index],
                focusedDay == index && isFocused,
                index,
                onArchiveSearch,
                onTimePickerFocus
            ) { newFocusedDay ->
                if (newFocusedDay >= 0 && newFocusedDay < availableDays.size) {
                    Log.i("new focused day", newFocusedDay.toString())
                    focusedDay = newFocusedDay
                }
            }
        }
    }
}