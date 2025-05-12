package com.example.iptvplayer.view.channelsAndEpgRow

import android.util.Log
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

@Composable
fun ItemBorder(
    borderYOffset: Int,
    borderHeight: Int
) {
    val density = LocalDensity.current.density

    /*// offset stay in pixels
    val borderYOffsetAnim by animateIntOffsetAsState(IntOffset(0, borderYOffset), animationSpec = tween(200)) */

    Log.i("border y offset", borderYOffset.toString())

    Box(
        modifier = Modifier
            .offset { IntOffset(0, borderYOffset) }
            .fillMaxWidth()
            .padding(horizontal = 7.dp)
            // conversion to dp for height display
            .height((borderHeight / density).dp)
            .border(1.dp, Color.White)
    )
}