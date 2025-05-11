package com.example.iptvplayer.view.channels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.example.iptvplayer.R

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun Channel(
    name: String,
    logo: String,
    index: Int,
    onGloballyPositioned: (Int) -> Unit,
) {

    Row(
        modifier = Modifier
            .onGloballyPositioned { coordinates ->
                if (index == 0) {
                    val height = coordinates.size.height
                    onGloballyPositioned(height)
                }
            }
            .padding(7.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = (index + 1).toString(),
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 22.sp
        )

        GlideImage(
            model = logo,
            modifier = Modifier.size(45.dp),
            contentScale = ContentScale.Fit,
            contentDescription = stringResource(R.string.channel_logo),
        )

        Text(
            text = name,
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 19.sp
        )
    }
}