package com.example.iptvplayer.view.player

import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.iptvplayer.view.channels.ChannelsViewModel
import com.example.iptvplayer.view.channelsAndEpgRow.ArchiveViewModel

@Composable
fun PlayerView(
    isBackPressed: Boolean,
    stayInsideApp: () -> Unit,
    exitApp: () -> Unit
) {
    val mediaViewModel: MediaViewModel = hiltViewModel()
    val archiveViewModel: ArchiveViewModel = hiltViewModel()
    val channelsViewModel: ChannelsViewModel = hiltViewModel()

    val isDataSourceSet by mediaViewModel.isDataSourceSet.collectAsState()
    val isPlaybackStarted by mediaViewModel.isPlaybackStarted.collectAsState()
    val isSeeking by mediaViewModel.isSeeking.collectAsState()

    val archiveSegmentUrl by archiveViewModel.archiveSegmentUrl.collectAsState()

    val currentChannel by channelsViewModel.currentChannel.collectAsState()
    val currentTime by mediaViewModel.currentTime.collectAsState()

    var surfaceHolder by remember { mutableStateOf<SurfaceHolder?>(null) }

    LaunchedEffect(archiveSegmentUrl) {
        mediaViewModel.startTsCollectingJob(archiveSegmentUrl)
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        if (!isPlaybackStarted) {
            CircularProgressIndicator(
                modifier = Modifier
                    .zIndex(99f)
                    .align(Alignment.Center)
            )

            Box(
                modifier =  Modifier.fillMaxSize()
                    .background(Color.Black)
                    .zIndex(98f)
            )
        }

        if (isSeeking) {
            StreamRewindFrame(
                currentChannel.name,
                currentTime
            )
        }

        if (isBackPressed) {
            ExitConfirmationDialog(
                stayInsideApp,
                exitApp,
            )
        }

        Log.i("player recomposed", "is data source set ${isDataSourceSet} surface holder ${surfaceHolder}")

        AndroidView(
            factory = { context ->
                SurfaceView(context).apply {
                    holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: SurfaceHolder) {
                            surfaceHolder = holder
                        }

                        override fun surfaceChanged(
                            holder: SurfaceHolder, format: Int,
                            width: Int, height: Int
                        ) {
                            Log.i("SURFACE SIZE", "$format $width $height")
                        }

                        override fun surfaceDestroyed(holder: SurfaceHolder) {
                            surfaceHolder = null
                            Log.i("is surface destroyed", "yes")
                        }
                    })
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = {
                if (isDataSourceSet && surfaceHolder != null) {
                    Log.i("set display?", "set")
                    mediaViewModel.ijkPlayer?.setDisplay(surfaceHolder)
                }
            }
        )
    }
}