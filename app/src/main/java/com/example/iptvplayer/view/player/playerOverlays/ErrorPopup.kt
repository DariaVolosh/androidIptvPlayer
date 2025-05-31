package com.example.iptvplayer.view.player.playerOverlays

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.tv.material3.Text
import com.example.iptvplayer.R
import com.example.iptvplayer.view.errors.ErrorData

@Composable
fun ErrorPopup(
    errorData: ErrorData
) {
    val gradientBorderColor = Color(0xFf7F6969).copy(0.3f)

    Box(
        modifier = Modifier
            .zIndex(99f)
            .background(Color.Black)
            .background(
                Brush.verticalGradient(
                    Pair(0f, gradientBorderColor),
                    Pair(0.5f, MaterialTheme.colorScheme.primary.copy(0.6f)),
                    Pair(1f, gradientBorderColor)
                )
            )
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Image(
                modifier = Modifier.size(45.dp),
                painter = painterResource(errorData.errorIcon),
                contentDescription = stringResource(R.string.error_icon)
            )

            Text(
                text = errorData.errorTitle,
                fontSize = 23.sp,
                fontWeight = FontWeight(500),
                color = MaterialTheme.colorScheme.onSecondary
            )

            Text(
                text = errorData.errorDescription,
                fontSize = 18.sp,
                fontWeight = FontWeight(300),
                color = MaterialTheme.colorScheme.onSecondary
            )
        }
    }
}