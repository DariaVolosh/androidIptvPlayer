package com.example.iptvplayer.view.epg

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EpgDate(
    date: String,
    alpha: Float,
    onGloballyPositioned: (Int) -> Unit
) {

    Row(
        modifier = Modifier
            .alpha(alpha)
            .onGloballyPositioned { cords ->
                val height = cords.size.height
                Log.i("epg date height", height.toString())
                onGloballyPositioned(height)
            }
            .background(
                Brush.verticalGradient(
                    Pair(0.6f, MaterialTheme.colorScheme.secondary),
                    Pair(1f, Color.Transparent)
                )
            )
            .padding(horizontal = 15.dp)
            .padding(top = 12.dp, bottom = 10.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = date,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onSecondary
        )

        HorizontalDivider(
            modifier = Modifier.padding(start = 10.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.onSecondary
        )
    }
}