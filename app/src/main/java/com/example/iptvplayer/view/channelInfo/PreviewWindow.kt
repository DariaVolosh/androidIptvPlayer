package com.example.iptvplayer.view.channelInfo

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.example.iptvplayer.R
import com.example.iptvplayer.data.Utils.formatDate

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun PreviewWindow(
    modifier: Modifier,
    streamName: String,
    imageTimestamp: Long,
    progressBarXOffset: Int,
    dotXOffset: Int
) {
    val aspectRatio = 16f / 9
    val width = 140
    val height = width / aspectRatio

    val datePattern = "EEEE d MMMM HH:mm:ss"


    Log.i("CURRENT TIME IN FUNCTIONS", formatDate(imageTimestamp, datePattern))


    if (streamName != "-1" && imageTimestamp != 0L) {
        Box (
            modifier = modifier
                .size(width.dp, height.dp)
                .offset(x = (progressBarXOffset + dotXOffset - width / 2).dp, y = (-height + 10).dp)
                .border(1.dp, Color.White),
            contentAlignment = Alignment.BottomCenter
        ) {
            GlideImage(
                model = "http://193.176.212.58:8080/${streamName}/${imageTimestamp}.jpg",
                contentDescription = stringResource(R.string.stream_thumbnail),
                contentScale = ContentScale.Fit
            )

            Image(
                modifier = Modifier
                    .size(12.dp, 12.dp)
                    .offset(x = 1.dp, y = 12.dp),
                painter = painterResource(R.drawable.preview_window_pointer),
                contentDescription = stringResource(R.string.preview_window_pointer)
            )
        }
    }
}