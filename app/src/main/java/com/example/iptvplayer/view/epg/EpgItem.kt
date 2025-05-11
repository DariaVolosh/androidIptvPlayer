package com.example.iptvplayer.view.epg

import android.util.Log
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun EpgItem(
    index: Int,
    videoTimeRange: String,
    title: String,
    isDvrAvailable: Boolean,
    onGloballyPositioned: (Int) -> Unit
) {
    val localDensity = LocalDensity.current.density

    Row(
        modifier = Modifier
            .onGloballyPositioned { cords ->
                if (index == 1) {
                    val height = cords.size.height
                    Log.i("epg item height", height.toString())
                    onGloballyPositioned(height)
                }
            }
            //.border(1.dp, Color.Red)
            .padding(3.dp)
    ) {
        Text(
            modifier = Modifier.alpha(if (isDvrAvailable) 1f else 0.45f),
            text = "$videoTimeRange $title",
            fontSize = 17.sp,
            color = MaterialTheme.colorScheme.onSecondary
        )
    }
}