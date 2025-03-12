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
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
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
    isMiddleItem: Boolean,
    onKeyEvent: (Key) -> Unit,
    onScrollRequest: () -> Unit,
    onGloballyPositioned: (Int) -> Unit,
    playMedia: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isFocused) {
        Log.i("CHANNEL_IS_FOCUSED", isFocused.toString())
        if (isFocused) {
            focusRequester.requestFocus()
            playMedia()
        } else focusRequester.freeFocus()
    }

    val localDensity = LocalDensity.current.density

    Row(
        modifier = Modifier
            .border(1.dp, if (!isMiddleItem && isFocused) MaterialTheme.colorScheme.onPrimary else Color.Transparent)
            .onGloballyPositioned { coordinates ->
                val height = coordinates.size.height
                // 17 is vertical padding between column items
                val heightWithoutPadding = height
                onGloballyPositioned((height / localDensity).toInt())
            }
            .padding(7.dp)
            .onFocusEvent { state ->
                if (state.isFocused) {
                    onScrollRequest()
                }
            }
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                Log.i("channel_key_event", event.toString())
                if (event.type == KeyEventType.KeyDown) {
                    onKeyEvent(event.key)
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