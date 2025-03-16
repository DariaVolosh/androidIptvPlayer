package com.example.iptvplayer.view.programDatePicker

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.iptvplayer.data.Utils
import com.example.iptvplayer.view.channels.ArchiveViewModel
import kotlin.math.ceil

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EpgAvailableDays(
    firstAndLastDay: Pair<Int,Int>?,
    epgMonth: Int?,
    daysOfWeek: List<String>
) {
    val archiveViewModel: ArchiveViewModel = hiltViewModel()

    var focusedDay by remember { mutableIntStateOf(-1) }
    val daysAndFocusRequesters = remember { mutableStateOf<Map<Int, FocusRequester>>(mapOf()) }

    var daysQuantity by remember { mutableIntStateOf(0) }
    var rowsQuantity by remember { mutableIntStateOf(0) }

    var monthFirstDay by remember { mutableStateOf("") }
    var initialDayIndex by remember { mutableIntStateOf(-1) }

    var isComposed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { }

    LaunchedEffect(firstAndLastDay, epgMonth, daysOfWeek) {
        if (firstAndLastDay != null && epgMonth != null && daysOfWeek.isNotEmpty()) {
            Log.i("LAUNCHED EFFECT CALLED", "CALLED")
            daysQuantity = firstAndLastDay.second - firstAndLastDay.first

            monthFirstDay = Utils.getDayOfWeek(epgMonth, firstAndLastDay.first)
            initialDayIndex = daysOfWeek.indexOf(monthFirstDay)

            val focusRequesters = mutableMapOf<Int, FocusRequester>()

            for (day in firstAndLastDay.first..firstAndLastDay.second) {
                focusRequesters[day] = FocusRequester()
            }

            daysAndFocusRequesters.value = focusRequesters

            val liveTime = archiveViewModel.liveTime.value
            liveTime?.let { liveTime ->
                focusedDay = Utils.getCalendarDay(Utils.getCalendar(liveTime))
            }
        }
    }

    LaunchedEffect(daysQuantity) {
        if (daysQuantity != 0) {
            rowsQuantity = ceil(daysQuantity.toFloat() / 7).toInt()
        }
    }

    LaunchedEffect(isComposed, focusedDay) {
        if (focusedDay != -1 && isComposed) {
            Log.i("days and focus", daysAndFocusRequesters.value.size.toString())
            Log.i("FOCUS REQUESTER", daysAndFocusRequesters.value[focusedDay].toString())
            daysAndFocusRequesters.value[focusedDay]?.requestFocus()
        }
    }

    if (
        focusedDay != -1 &&
        daysQuantity != 0 &&
        rowsQuantity != 0 &&
        monthFirstDay != "" &&
        initialDayIndex != -1 &&
        firstAndLastDay != null
        ) {

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End
        ) {
            var day = firstAndLastDay.first
            var dayIndex = initialDayIndex

            for (row in 0 until rowsQuantity) {
                val daysInWeek = 7 - dayIndex

                Row(
                    modifier = Modifier
                        .fillMaxWidth(daysInWeek.toFloat() / 7)
                        .padding(vertical = 11.dp)
                        .padding(end = 6.dp, start = if (daysInWeek == 7) 6.dp else 0.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (i in dayIndex until 7) {
                        val circleColor = MaterialTheme.colorScheme.onSecondary.copy(0.12f)

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
                                        color = circleColor,
                                        radius = 34f
                                    )
                                }
                            }

                            Text(
                                text = day.toString(),
                                color = MaterialTheme.colorScheme.onSecondary,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        day++
                    }

                    dayIndex = 0
                }
            }

            isComposed = true
        }
    }
}