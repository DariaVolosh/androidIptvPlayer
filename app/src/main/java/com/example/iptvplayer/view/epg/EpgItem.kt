package com.example.iptvplayer.view.epg

import android.util.Log
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun EpgItem(
    videoTimeRange: String,
    title: String,
    isDvrAvailable: Boolean,
    isFocused: Boolean,
    isEpgListFocused: Boolean,
    epgListBorderMiddle: Int,
    isListStart: Boolean,
    isListEnd: Boolean,
    onGloballyPositioned: (Int) -> Unit
) {
    var yOffset by remember { mutableIntStateOf(0) }
    LaunchedEffect(yOffset) {
        Log.i("y offset epg", "$yOffset $videoTimeRange $title")
    }

    Log.i("epg item data", "$videoTimeRange $title $isListStart $isListEnd")

    Row(
        modifier = Modifier
            .fillMaxWidth(0.97f)
            .onGloballyPositioned { cords ->
                if (isFocused) {
                    val height = cords.size.height
                    Log.i("epg item height", height.toString())
                    onGloballyPositioned(height)
                }

                val yCord = cords.positionInParent().y
                yOffset = yCord.toInt()
            }
            .border(
                1.dp,
                if (
                    isFocused && isEpgListFocused && ((isListStart || isListEnd))
                    ) MaterialTheme.colorScheme.onSecondary
                else Color.Transparent
            )
            .padding(vertical = 5.dp)
            .padding(horizontal = 10.dp)
    ) {
        Text(
            modifier = Modifier.alpha(if (isDvrAvailable) 1f else 0.45f),
            text = "$videoTimeRange $title",
            fontSize = 17.sp,
            color = MaterialTheme.colorScheme.onSecondary
        )
    }
}