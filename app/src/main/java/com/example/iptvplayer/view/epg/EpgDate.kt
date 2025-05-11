package com.example.iptvplayer.view.epg

import android.util.Log
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EpgDate(
    index: Int,
    date: String,
    onGloballyPositioned: (Int) -> Unit
) {

    val localDensity = LocalDensity.current.density

    Row(
        modifier = Modifier
            .onGloballyPositioned { cords ->
                if (index == 0) {
                    val height = cords.size.height
                    Log.i("epg date height", height.toString())
                    onGloballyPositioned(height)
                }
            }
            //.border(1.dp, Color.Green)
            .padding(bottom = 15.dp)
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