package com.example.iptvplayer.view.programDatePicker.datePicker

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.iptvplayer.data.Utils

data class ArchiveDate(
    val dayOfWeek: String,
    val day: Int,
    val month: Int
)

//"$currentDay ${Utils.getFullMonthName(currentMonth)}"

@Composable
fun ArchiveDate(
    modifier: Modifier,
    archiveDateInfo: ArchiveDate,
    isFocused: Boolean,
    index: Int,
    onArchiveSearch: () -> Unit,
    onTimePickerFocus: () -> Unit,
    onDateFocusChange: (Int) -> Unit
) {

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            focusRequester.requestFocus()
        } else {
            focusRequester.freeFocus()
        }
    }

    Row(
        modifier = modifier
            .background(
                if (isFocused)
                    MaterialTheme.colorScheme.onSecondary.copy(0.12f)
                else
                    Color.Transparent
            )
            .padding(15.dp)
            .focusRequester(focusRequester)
            .focusable(true)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionDown -> {
                            onDateFocusChange(index + 1)
                        }

                        Key.DirectionUp -> {
                            onDateFocusChange(index - 1)
                        }

                        Key.DirectionRight -> {
                            onTimePickerFocus()
                        }

                        Key.DirectionCenter -> {
                            onArchiveSearch()
                        }
                    }
                }

                true
            }
    ) {
        Text(
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Start,
            text = archiveDateInfo.dayOfWeek,
            fontSize = 19.sp,
            color = MaterialTheme.colorScheme.onSecondary
        )

        Text(
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Start,
            text = "${archiveDateInfo.day} ${Utils.getFullMonthName(archiveDateInfo.month)}",
            fontSize = 19.sp,
            color = MaterialTheme.colorScheme.onSecondary
        )
    }
}