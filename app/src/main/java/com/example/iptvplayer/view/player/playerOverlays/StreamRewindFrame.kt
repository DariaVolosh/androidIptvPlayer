package com.example.iptvplayer.view.player.playerOverlays

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.iptvplayer.R
import com.example.iptvplayer.data.FLUSSONIC_BASE_URL
import com.example.iptvplayer.view.errors.ErrorData
import com.example.iptvplayer.view.errors.ErrorViewModel

@Composable
fun StreamRewindFrame(
    streamName: String,
    imageTimestamp: Long
) {
    val errorViewModel: ErrorViewModel = hiltViewModel()
    val localContext = LocalContext.current

    var imageBitmap: ImageBitmap? by remember {
        mutableStateOf(null)
    }
    var painter: Painter? by remember {
        mutableStateOf(null)
    }

    var timesFailedToFetchFrame by remember { mutableIntStateOf(0) }

    Log.i("stream rewind image timestamp", imageTimestamp.toString())

    LaunchedEffect(timesFailedToFetchFrame) {
        Log.i("times tried to fetch frame", timesFailedToFetchFrame.toString())
        if (timesFailedToFetchFrame >= 2) {
            errorViewModel.publishError(
                ErrorData(
                    localContext.getString(R.string.preview_not_available),
                    localContext.getString(R.string.preview_not_available_descr),
                    R.drawable.warning_icon
                )
            )
        }
    }

    Glide.with(localContext)
        .asBitmap()
        .load("$FLUSSONIC_BASE_URL${streamName}/${imageTimestamp}-preview.mp4")
        .into(object: CustomTarget<Bitmap>() {
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                imageBitmap = resource.asImageBitmap()
                painter = BitmapPainter(imageBitmap!!)
                timesFailedToFetchFrame = 0
            }

            override fun onLoadCleared(placeholder: Drawable?) {
                painter = null
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {
                super.onLoadFailed(errorDrawable)
                timesFailedToFetchFrame++
                Log.i("preview error", "error")
            }
        })

    if (painter != null) {
        Image(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(99f),
            painter = painter!!,
            contentDescription = stringResource(R.string.stream_thumbnail),
            contentScale = ContentScale.Crop
        )
    }
}