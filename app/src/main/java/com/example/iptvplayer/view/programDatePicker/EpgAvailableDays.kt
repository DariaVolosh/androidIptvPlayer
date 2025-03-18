package com.example.iptvplayer.view.programDatePicker

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.iptvplayer.data.Utils

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EpgAvailableDays(
    dvrMonth: Int,
    dvrFirstAndLastDay: Pair<Int,Int>,
    daysOfWeek: List<String>,
    isFocused: Boolean,
    liveTime: Long,
    onDateChanged: (Long) -> Unit,
    onTimePickerFocus: () -> Unit
) {
    var focusedDay by remember { mutableIntStateOf(-1) }
    val daysAndFocusRequesters = remember { mutableStateOf<Map<Int, FocusRequester>>(mapOf()) }

    var daysQuantity by remember { mutableIntStateOf(0) }

    var monthFirstDay by remember { mutableStateOf("") }
    var initialDayIndex by remember { mutableIntStateOf(-1) }

    var isComposed by remember { mutableStateOf(false) }

    LaunchedEffect(dvrMonth, dvrFirstAndLastDay, daysOfWeek) {
        if (dvrMonth != -1 && dvrFirstAndLastDay.first != 0 && daysOfWeek.isNotEmpty()) {
            Log.i("LAUNCHED EFFECT CALLED", "CALLED")
            daysQuantity = Utils.getDaysOfMonth(dvrMonth)

            monthFirstDay = Utils.getDayOfWeek(dvrMonth, 1)
            initialDayIndex = daysOfWeek.indexOf(monthFirstDay)

            val focusRequesters = mutableMapOf<Int, FocusRequester>()

            for (day in dvrFirstAndLastDay.first..dvrFirstAndLastDay.second) {
                focusRequesters[day] = FocusRequester()
            }

            daysAndFocusRequesters.value = focusRequesters

            focusedDay = Utils.getCalendarDay(Utils.getCalendar(liveTime))
        }
    }

    LaunchedEffect(isComposed, focusedDay, isFocused, dvrMonth) {
        if (focusedDay != -1 && isComposed && isFocused && dvrMonth != -1) {
            Log.i("days and focus", daysAndFocusRequesters.value.size.toString())
            Log.i("FOCUS REQUESTER", daysAndFocusRequesters.value[focusedDay].toString())
            daysAndFocusRequesters.value[focusedDay]?.requestFocus()

            val dateSinceEpoch = Utils.dateToEpochSeconds(focusedDay, dvrMonth, 2025, 0, 0)
            onDateChanged(dateSinceEpoch)
        }
    }

    if (
        focusedDay != -1 &&
        daysQuantity != 0 &&
        monthFirstDay != "" &&
        initialDayIndex != -1 &&
        dvrFirstAndLastDay != Pair(0,0)
        ) {

        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            var day = 1
            var dayIndex = initialDayIndex
            Log.i("epg available days", "day: $day dayIndex: $dayIndex")
            Log.i("epg available days", "days quantity: $daysQuantity")

            for (row in 0 until 6) {
                val daysToAdd = daysQuantity - day + 1
                val daysInWeek =
                    if (row == 0) {
                        7 - initialDayIndex
                    } else {
                        if (daysToAdd >= 7) 7
                        else daysToAdd
                    }

                if (day > daysQuantity) break

                Log.i("epg available days", "days in week: $daysInWeek")

                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (daysInWeek < 7 && dayIndex != 0) {
                        Spacer(
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth(daysInWeek.toFloat() / 7)
                            .padding(vertical = 11.dp),
                    ) {
                        for (i in dayIndex until 7) {
                            val circleColor = MaterialTheme.colorScheme.onSecondary.copy(0.12f)
                            val borderColor = MaterialTheme.colorScheme.onSecondary

                            Log.i("FOCUS REQUESTER", "INSIDE $day ${daysAndFocusRequesters.value[day]}")
                            Box(
                                modifier = Modifier
                                    .focusRequester(
                                        daysAndFocusRequesters.value[day] ?: FocusRequester()
                                    )
                                    .focusable()
                                    .onKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyDown) {
                                            var nextFocusedDay = focusedDay

                                            when (event.key) {
                                                Key.DirectionLeft -> nextFocusedDay--
                                                Key.DirectionRight -> nextFocusedDay++
                                                Key.DirectionUp -> nextFocusedDay -= 7
                                                Key.DirectionDown -> nextFocusedDay += 7
                                            }

                                            if (daysAndFocusRequesters.value[nextFocusedDay] != null) {
                                                focusedDay = nextFocusedDay
                                            } else {
                                                if (event.key == Key.DirectionRight) {
                                                    onTimePickerFocus()
                                                }
                                            }
                                        }

                                        true
                                    }
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                if (day == focusedDay) {
                                    Canvas(
                                        modifier = Modifier.matchParentSize()
                                    ) {
                                        drawCircle(
                                            color = if (isFocused) circleColor else borderColor,
                                            style = if (isFocused) Fill else Stroke(width = 1.dp.toPx()),
                                            radius = 34f
                                        )
                                    }
                                }

                                Text(
                                    text = day.toString(),
                                    color = MaterialTheme.colorScheme.onSecondary.copy(
                                        if (daysAndFocusRequesters.value.containsKey(day)) 1f
                                        else 0.3f
                                    ),
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center
                                )
                            }

                            day++
                            if (day > daysQuantity) break
                        }

                        dayIndex = 0
                    }
                }
            }

            isComposed = true
        }
    }
}