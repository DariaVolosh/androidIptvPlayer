package com.example.iptvplayer.view.epg

import android.util.Log
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
    onGloballyPositioned: (Int) -> Unit
) {
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
            .padding(7.dp)
            .fillMaxWidth()
    ) {
        Text(
            modifier = Modifier.alpha(if (isDvrAvailable) 1f else 0.45f),
            text = "$startTime-$stopTime $title",
            fontSize = 17.sp,
            color = MaterialTheme.colorScheme.onSecondary
        )
    }
}