package com.example.iptvplayer.view.programDatePicker.timePicker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TimeControl(
    modifier: Modifier,
    text: String,
    focusRequester: FocusRequester,
    handleKeyEvent: (Key) -> Unit
) {

    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .background(
                if (isFocused)
                    MaterialTheme.colorScheme.onSecondary.copy(0.12f)
                else
                    Color.Transparent
            )
            .focusRequester(focusRequester)
            .onFocusEvent { event ->
                isFocused = event.isFocused
            }
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    handleKeyEvent(event.key)
                }
                true
            }
            .height(80.dp)
            .border(1.dp, MaterialTheme.colorScheme.onSecondary),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 21.sp,
            color = MaterialTheme.colorScheme.onSecondary
        )
    }
}