package com.example.iptvplayer

import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import com.example.iptvplayer.ui.theme.IptvPlayerTheme
import com.example.iptvplayer.view.channels.ChannelList
import com.example.iptvplayer.view.channels.ChannelsViewModel
import com.example.iptvplayer.view.channels.MediaViewModel
import com.example.iptvplayer.view.epg.EpgList
import javax.inject.Inject

class MainActivity : ComponentActivity() {
    @Inject lateinit var channelsViewModel: ChannelsViewModel
    @Inject lateinit var mediaViewModel: MediaViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (applicationContext as MyApp).appComponent.inject(this)

        setContent {
            IptvPlayerTheme {
                MainScreen(channelsViewModel, mediaViewModel)
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun MainScreen(
    channelsViewModel: ChannelsViewModel,
    mediaViewModel: MediaViewModel
) {
    val isDataSourceSet by mediaViewModel.isDataSourceSet.observeAsState()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (isDataSourceSet == true) {
            AndroidView(factory = { context ->
                SurfaceView(context).apply {
                    holder.addCallback(object: SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: SurfaceHolder) {
                            mediaViewModel.ijkPlayer?.setDisplay(holder)
                        }

                        override fun surfaceChanged(
                            holder: SurfaceHolder,
                            format: Int,
                            width: Int,
                            height: Int
                        ) {

                        }

                        override fun surfaceDestroyed(holder: SurfaceHolder) {
                        }

                    })
                }
            }, modifier = Modifier.fillMaxSize())
        }

        Row(
            Modifier.fillMaxSize()
        ) {
            ChannelList(
                channelsViewModel,
                Modifier.fillMaxWidth(0.5f)
            ) { url ->
                mediaViewModel.setMediaUrl(url)
            }

            EpgList(
                Modifier.fillMaxSize()
            )
        }
    }
}