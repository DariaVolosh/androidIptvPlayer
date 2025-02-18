package com.example.iptvplayer.view.channels

import android.util.Log
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.example.iptvplayer.R

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun Channel(
    name: String,
    logo: String,
    index: Int,
    isFocused: Boolean,
    onChannelClicked: () -> Unit,
    onFocusedChannel: (Int) -> Unit,
    playMedia: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            focusRequester.requestFocus()
            playMedia()
        } else focusRequester.freeFocus()
    }

    Row(
        modifier = Modifier
            .border(1.dp, if (isFocused) MaterialTheme.colorScheme.onPrimary else Color.Transparent)
            .padding(7.dp)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                Log.i("LOL", event.toString())
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionDown -> onFocusedChannel(index + 1)
                        Key.DirectionUp -> onFocusedChannel(index - 1)
                        Key.DirectionCenter -> onChannelClicked()
                    }
                }

                true
            }
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = (index + 1).toString(),
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 22.sp
        )

        GlideImage(
            model = logo,
            modifier = Modifier.size(45.dp),
            contentScale = ContentScale.Fit,
            contentDescription = stringResource(R.string.channel_logo),
        )

        Text(
            text = name,
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 19.sp
        )
    }
}