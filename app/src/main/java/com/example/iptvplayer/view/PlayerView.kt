package com.example.iptvplayer.view

import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.iptvplayer.retrofit.data.ChannelData
import com.example.iptvplayer.view.channels.ArchiveViewModel
import com.example.iptvplayer.view.channels.ChannelsViewModel
import com.example.iptvplayer.view.channels.MediaViewModel

@Composable
fun PlayerView() {
    val mediaViewModel: MediaViewModel = hiltViewModel()
    val channelsViewModel: ChannelsViewModel = hiltViewModel()
    val archiveViewModel: ArchiveViewModel = hiltViewModel()

    val currentChannel by channelsViewModel.currentChannel.collectAsState()

    val isDataSourceSet by mediaViewModel.isDataSourceSet.observeAsState()
    val isPlaybackStarted by mediaViewModel.isPlaybackStarted.observeAsState()

    val archiveSegmentUrl by archiveViewModel.archiveSegmentUrl.collectAsState()

    var surfaceView by remember { mutableStateOf<SurfaceView?>(null) }
    var isInitialStreamStarted by remember { mutableStateOf(false) }

    LaunchedEffect(currentChannel, isInitialStreamStarted) {
        if (currentChannel != ChannelData() && !isInitialStreamStarted) {
            if (mediaViewModel.isLive.value) {
                mediaViewModel.setMediaUrl(currentChannel.channelUrl)
            } else {
                archiveViewModel.getArchiveUrl(currentChannel.channelUrl, mediaViewModel.currentTime.value)
            }

            isInitialStreamStarted = true
        }
    }

    LaunchedEffect(archiveSegmentUrl) {
        mediaViewModel.setMediaUrl(archiveSegmentUrl)
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        if (isPlaybackStarted == false) {
            CircularProgressIndicator(
                modifier = Modifier
                    .zIndex(99f)
                    .align(Alignment.Center)
            )
        }

        AndroidView(
            factory = { context ->
                SurfaceView(context).apply {
                    surfaceView = this

                    holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: SurfaceHolder) {

                        }

                        override fun surfaceChanged(
                            holder: SurfaceHolder, format: Int,
                            width: Int, height: Int
                        ) {
                            Log.i("SURFACE SIZE", "$format $width $height")
                        }

                        override fun surfaceDestroyed(holder: SurfaceHolder) {}
                    })
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = {
                if (isDataSourceSet == true && surfaceView?.holder != null) {
                    mediaViewModel.ijkPlayer?.setDisplay(surfaceView?.holder)
                }
            }
        )
    }
}