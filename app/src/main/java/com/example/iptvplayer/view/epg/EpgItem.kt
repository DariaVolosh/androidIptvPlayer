package com.example.iptvplayer.view.epg

import android.util.Log
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun EpgItem(
    startTime: String,
    stopTime: String,
    title: String,
    isFocused: Boolean,
    isDvrAvailable: Boolean,
    onGloballyPositioned: (Int) -> Unit,
    onScrollRequest: () -> Unit,
    onKeyEvent: (Key) -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            onScrollRequest()
            focusRequester.requestFocus()
        } else {
            focusRequester.freeFocus()
        }
    }

    val localDensity = LocalDensity.current.density

    Row(
        modifier = Modifier
            .onGloballyPositioned { cords ->
                if (isFocused) {
                    Log.i("LAUNCHED COROUTINE", title)
                    Log.i("LAUNCHED COROUTINE", cords.positionInParent().y.toString())
                    val height = cords.size.height
                    onGloballyPositioned((height / localDensity).toInt())
                }
            }
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    Log.i("on_epg_event", event.key.toString() + " $title")
                    onKeyEvent(event.key)
                }
                true
            }
            .padding(7.dp)
            .fillMaxWidth()
    ) {
        Text(
            modifier = Modifier.alpha(if (isDvrAvailable) 1f else 0.3f),
            text = "$startTime-$stopTime $title",
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onSecondary
        )
    }
}