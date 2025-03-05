package com.example.iptvplayer.view.channelInfo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned

@Composable
fun LinearProgressWithDot(
    modifier: Modifier,
    progress: Float
) {
    var progressBarWidthPx by remember {
        mutableIntStateOf(0)
    }

    Box(
        modifier = modifier,
    ) {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .onGloballyPositioned { coordinates ->
                    progressBarWidthPx = coordinates.size.width
                },
            progress = {(progress / 100)},
            color = MaterialTheme.colorScheme.onSecondary
        )

        Canvas(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
        ) {
            val progressPosition = progressBarWidthPx * (progress / 100)
            drawCircle(
                color = Color.Gray,
                radius = 10f,
                center = Offset(progressPosition, size.height / 2)
            )
        }
    }
}