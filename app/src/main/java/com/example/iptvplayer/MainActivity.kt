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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import com.example.iptvplayer.ui.theme.IptvPlayerTheme
import com.example.iptvplayer.view.channelInfo.ChannelInfo
import com.example.iptvplayer.view.channels.ChannelList
import com.example.iptvplayer.view.channels.ChannelsViewModel
import com.example.iptvplayer.view.channels.MediaViewModel
import com.example.iptvplayer.view.epg.EpgList
import com.example.iptvplayer.view.epg.EpgViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (applicationContext as MyApp).appComponent.inject(this)

        setContent {
            IptvPlayerTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun MainScreen() {
    val mediaViewModel: MediaViewModel = hiltViewModel()
    val channelsViewModel: ChannelsViewModel = hiltViewModel()
    val epgViewModel: EpgViewModel = hiltViewModel()

    val isDataSourceSet by mediaViewModel.isDataSourceSet.observeAsState()

    val channels by channelsViewModel.channels.observeAsState()
    val channelNames by channelsViewModel.channelNames.observeAsState()

    val currentChannelEpg by epgViewModel.currentChannelEpg.observeAsState()

    var isChannelClicked by remember { mutableStateOf(false) }
    var focusedChannel by remember { mutableIntStateOf(0) }
    var isChannelInfoShown by remember { mutableStateOf(false) }

    /*LaunchedEffect(isChannelInfoShown) {
        if (isChannelInfoShown) {
            delay(4000)
            isChannelInfoShown = false
        }
    } */

    LaunchedEffect(Unit) {
        channelsViewModel.parsePlaylist()
    }

    LaunchedEffect(channelNames) {
        channelNames?.let { channelNames ->
            epgViewModel.saveEpgData(channelNames)
        }
    }

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
                            holder: SurfaceHolder, format: Int,
                            width: Int, height: Int
                        ) {}

                        override fun surfaceDestroyed(holder: SurfaceHolder) {}
                    })
                }
            }, modifier = Modifier.fillMaxSize())
        }

        if (!isChannelClicked) {
            Row(
                Modifier.fillMaxSize()
            ) {
                channels?.let { channels ->
                    ChannelList(
                        Modifier.fillMaxWidth(0.5f),
                        channels,
                        focusedChannel,
                        {
                            isChannelInfoShown = true
                            isChannelClicked = true
                        },
                        { ch ->
                            epgViewModel.fetchChannelEpg(channels[ch].name)
                            focusedChannel = ch
                        }
                    ) { url ->
                        mediaViewModel.setMediaUrl(url)
                    }
                }

                currentChannelEpg?.let { currentChannelEpg ->
                    EpgList(
                        Modifier.fillMaxSize(),
                        currentChannelEpg
                    )
                }
            }
        } else {
            if (isChannelInfoShown) {
                channels?.let { channels ->
                    ChannelInfo(
                        focusedChannel,
                        channels[focusedChannel].name,
                        channels[focusedChannel].logo,
                        channels[focusedChannel].url,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}