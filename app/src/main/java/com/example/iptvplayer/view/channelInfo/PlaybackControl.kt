package com.example.iptvplayer.view.channelInfo

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.iptvplayer.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Immutable
data class PlaybackControl(
    val image: Int,
    val contentDescription: Int,
    val isFocused: Boolean,
    val changeFocusedControl: (Int) -> Unit,
    val onPressed: () -> Unit,
    val onLongPressed: () -> Unit,
    val onFingerLiftedUp: () -> Unit
)

@Composable
fun PlaybackControl(
    playbackControlInfo: PlaybackControl
) {

    var isLongPressed by remember {
        mutableStateOf(false)
    }

    var isKeyPressed by remember {
        mutableStateOf(false)
    }

    val coroutineScope = rememberCoroutineScope()
    val focusRequester = FocusRequester()

    val name = stringResource(playbackControlInfo.contentDescription)

    LaunchedEffect(isLongPressed) {
        Log.i("SHIT1", "$isLongPressed $name")
        while (isLongPressed) {
            playbackControlInfo.onLongPressed()
            delay(1000)
        }
    }

    LaunchedEffect(playbackControlInfo.isFocused) {
        if (playbackControlInfo.isFocused) {
            focusRequester.requestFocus()
        } else {
            focusRequester.freeFocus()
        }
    }

    Image(
        modifier = Modifier
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.key == Key.DirectionCenter) {
                    if (event.type == KeyEventType.KeyDown) {
                        Log.i("SHIT1", "Key down $name")
                        playbackControlInfo.onPressed()
                        if (!isKeyPressed) {
                            isKeyPressed = true
                            coroutineScope.launch {
                                delay(500)
                                if (isKeyPressed) isLongPressed = true
                            }
                        }
                    } else {
                        Log.i("SHIT1", "key up $name")
                        playbackControlInfo.onFingerLiftedUp()
                        isKeyPressed = false
                        isLongPressed = false
                    }
                } else if (event.key == Key.DirectionLeft || event.key == Key.DirectionRight) {
                    if (event.type == KeyEventType.KeyDown) {
                        val left = event.key == Key.DirectionLeft

                        val nextFocusedControl = when (playbackControlInfo.contentDescription) {
                            R.string.pause -> if (left) R.string.back else R.string.forward
                            R.string.back -> if (left) R.string.previous_program else R.string.pause
                            R.string.previous_program -> if (left) R.string.calendar else R.string.back
                            R.string.calendar -> if (left) R.string.go_live else R.string.previous_program
                            R.string.go_live -> if (left) R.string.next_program else R.string.calendar
                            R.string.next_program -> if (left) R.string.forward else R.string.go_live
                            R.string.forward -> if (left) R.string.pause else R.string.next_program
                            else -> 0
                        }

                        playbackControlInfo.changeFocusedControl(nextFocusedControl)
                    }
                }

                true

            }.size(40.dp)
            .alpha(if (playbackControlInfo.isFocused) 1f else 0.3f),
        painter = painterResource(playbackControlInfo.image),
        contentDescription = stringResource(playbackControlInfo.contentDescription)
    )
}