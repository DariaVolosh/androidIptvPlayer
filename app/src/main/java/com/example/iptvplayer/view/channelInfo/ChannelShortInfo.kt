package com.example.iptvplayer.view.channelInfo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.example.iptvplayer.R

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun ChannelShortInfo(
    focusedChannelIndex: Int,
    channelName: String,
    channelLogo: String
) {
    Column(
        //modifier = Modifier.border(1.dp, Color.Magenta),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = channelName,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onSecondary
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "${focusedChannelIndex + 1}.",
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSecondary
            )

            GlideImage(
                model = channelLogo,
                modifier = Modifier.size(40.dp),
                contentScale = ContentScale.Fit,
                contentDescription = stringResource(R.string.channel_logo)
            )
        }
    }
}