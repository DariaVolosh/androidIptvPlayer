package com.example.iptvplayer.view.channelInfo

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.example.iptvplayer.R

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun ChannelLogo(
    channelLogo: String
) {
    GlideImage(
        model = channelLogo,
        modifier = Modifier.size(50.dp),
        contentScale = ContentScale.Fit,
        contentDescription = stringResource(R.string.channel_logo)
    )
}