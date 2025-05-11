package com.example.iptvplayer.view.channelInfo

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

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
    playbackControlInfo: PlaybackControl,
) {

    Image(
        modifier = Modifier
            .size(30.dp)
            .alpha(if (playbackControlInfo.isFocused) 1f else 0.3f),
        painter = painterResource(playbackControlInfo.image),
        contentDescription = stringResource(playbackControlInfo.contentDescription)
    )
}