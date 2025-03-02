package com.example.iptvplayer.view.channelInfo

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun PlaybackControl(
    image: Int,
    contentDescription: Int,
    onPressed: () -> Unit,
    onLongPressed: () -> Unit,
    onFingerLiftedUp: () -> Unit
) {

    var isLongPressed by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(isLongPressed) {
        while (isLongPressed) {
            onLongPressed()
            delay(1000)
        }
    }

    Image(
        modifier = Modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        isLongPressed = true
                    },
                    onPress = {
                        onPressed()
                        awaitRelease()
                        isLongPressed = false
                        onFingerLiftedUp()
                    }
                )
            }
            .size(40.dp),
        painter = painterResource(image),
        contentDescription = stringResource(contentDescription)
    )
}