package com.example.iptvplayer.view.channelInfo

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun PlaybackControl(
    image: Int,
    contentDescription: Int
) {
    Image(
        modifier = Modifier.size(40.dp),
        painter = painterResource(image),
        contentDescription = stringResource(contentDescription)
    )
}