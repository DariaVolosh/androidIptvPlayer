package com.example.iptvplayer.view.player

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.zIndex
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.iptvplayer.R
import com.example.iptvplayer.data.FLUSSONIC_BASE_URL

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun StreamRewindFrame(
    streamName: String,
    imageTimestamp: Long
) {

    val localContext = LocalContext.current
    var imageBitmap: ImageBitmap? by remember {
        mutableStateOf(null)
    }
    var painter: Painter? by remember {
        mutableStateOf(null)
    }

    Log.i("stream rewind image timestamp", imageTimestamp.toString())

    Glide.with(localContext)
        .asBitmap()
        .load("$FLUSSONIC_BASE_URL${streamName}/${imageTimestamp}.jpg")
        .into(object: CustomTarget<Bitmap>() {
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                imageBitmap = resource.asImageBitmap()
                painter = BitmapPainter(imageBitmap!!)
            }

            override fun onLoadCleared(placeholder: Drawable?) {
                painter = null
            }

        })

    if (painter != null) {
        Image(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(98f),
            painter = painter!!,
            contentDescription = stringResource(R.string.stream_thumbnail),
            contentScale = ContentScale.Crop
        )
    }
}