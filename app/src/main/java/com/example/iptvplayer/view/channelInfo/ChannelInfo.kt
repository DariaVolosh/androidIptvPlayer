package com.example.iptvplayer.view.channelInfo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.example.iptvplayer.R


@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun ChannelInfo(
    index: Int,
    name: String,
    logo: String,
    modifier: Modifier
) {
    Column (
        modifier = modifier
            .background(MaterialTheme.colorScheme.secondary.copy(0.8f))
            .padding(bottom = 20.dp, top = 10.dp)
            .padding(horizontal = 10.dp)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.padding(end = 15.dp),
                text = "02:00",
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.onSecondary
            )

            LinearProgressIndicator(
                modifier = Modifier.weight(1f),
                progress = {0.5f},
                color = MaterialTheme.colorScheme.onSecondary
            )

            Text(
                modifier = Modifier.padding(start = 15.dp),
                text = "06:00",
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.onSecondary
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {

            Column(
                 modifier = Modifier
                     .padding(top = 17.dp)
                     .align(Alignment.TopStart)
            ) {
                Row(

                ) {
                    Text(
                        modifier = Modifier.padding(end = 7.dp),
                        text = "${index + 1}.",
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.onSecondary
                    )

                    Text(
                        text = name,
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                }

                GlideImage(
                    model = logo,
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .size(50.dp),
                    contentScale = ContentScale.Fit,
                    contentDescription = stringResource(R.string.channel_logo)
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.6f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 25.dp)
                ) {
                    Text(
                        modifier = Modifier.padding(end = 15.dp),
                        fontSize = 22.sp,
                        text = "Thursday 12 Sep 04:00:00",
                        color = MaterialTheme.colorScheme.onSecondary
                    )

                    Text(
                        fontSize = 22.sp,
                        text = "Erin Burnett OutFront",
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    PlaybackControl(R.drawable.calendar, R.string.calendar)
                    PlaybackControl(R.drawable.previous_program, R.string.previous_program)
                    PlaybackControl(R.drawable.back, R.string.back)
                    PlaybackControl(R.drawable.pause, R.string.pause)
                    PlaybackControl(R.drawable.forward, R.string.forward)
                    PlaybackControl(R.drawable.next_program, R.string.next_program)
                    PlaybackControl(R.drawable.go_live, R.string.go_live)
                }
            }

            Row(
                modifier = Modifier
                    .padding(top = 17.dp)
                    .align(Alignment.TopEnd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .padding(end = 7.dp)
                        .size(12.dp)
                        .background(Color.Red, shape = CircleShape)
                )

                Text(
                    text = "live",
                    color = Color.Red,
                    fontSize = 24.sp
                )
            }
        }
    }
}