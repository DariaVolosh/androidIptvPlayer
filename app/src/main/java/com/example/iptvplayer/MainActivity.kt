package com.example.iptvplayer

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.foundation.focusable
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import com.example.iptvplayer.ui.theme.IptvPlayerTheme
import com.example.iptvplayer.view.channelInfo.ChannelInfo
import com.example.iptvplayer.view.channels.ArchiveViewModel
import com.example.iptvplayer.view.channels.ChannelList
import com.example.iptvplayer.view.channels.ChannelsViewModel
import com.example.iptvplayer.view.channels.MediaViewModel
import com.example.iptvplayer.view.epg.EpgList
import com.example.iptvplayer.view.epg.EpgViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
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

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(UnstableApi::class)
@Composable
fun MainScreen() {
    val mediaViewModel: MediaViewModel = hiltViewModel()
    val channelsViewModel: ChannelsViewModel = hiltViewModel()
    val epgViewModel: EpgViewModel = hiltViewModel()
    val archiveViewModel: ArchiveViewModel = hiltViewModel()

    val isDataSourceSet by mediaViewModel.isDataSourceSet.observeAsState()

    val channels by channelsViewModel.channels.observeAsState()
    val epg by epgViewModel.epgList.observeAsState()

    var isChannelClicked by remember { mutableStateOf(false) }
    var isChannelInfoShown by remember { mutableStateOf(false) }
    var isChannelsListFocused by remember { mutableStateOf(true) }

    var focusedChannel by remember { mutableIntStateOf(0) }
    var focusedProgramme by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        channelsViewModel.parsePlaylist()
    }

    LaunchedEffect(epg) {
        if (epg?.size != 0) {
            focusedProgramme = epgViewModel.currentProgramme
        }
    }

    val handleChannelOnKeyEvent: (Key) -> Unit = { key ->
        when (key) {
            Key.DirectionDown -> {
                channels?.size?.let { size ->
                    if (focusedChannel + 1 < size) focusedChannel += 1
                }
            }
            Key.DirectionUp -> {
                if (focusedChannel - 1 >= 0) focusedChannel -= 1
            }
            Key.DirectionRight -> isChannelsListFocused = false
            Key.DirectionCenter -> {
                isChannelInfoShown = true
                isChannelClicked = true
            }
        }
    }

    val handleEpgOnKeyEvent: (Key) -> Unit =  { key ->
        when (key) {
            Key.DirectionDown -> {
                epg?.size?.let { size ->
                    if (focusedProgramme + 1 < size) focusedProgramme += 1
                }
            }
            Key.DirectionUp -> {
                if (focusedProgramme - 1 >= 0) focusedProgramme -= 1
            }
            Key.DirectionLeft -> {
                isChannelsListFocused = true
            }
            Key.DirectionCenter -> {
                epg?.get(focusedProgramme)?.let { epg ->
                    archiveViewModel.setCurrentTime(epg.startTime)

                    channels?.get(focusedChannel)?.let { channel ->
                        archiveViewModel.getArchiveUrl(channel.url)
                    }
                }
                isChannelInfoShown = true
                isChannelClicked = true
            }
        }
    }

    LaunchedEffect(focusedChannel) {
        channels?.let { channels ->
            epgViewModel.getEpgById(channels[focusedChannel].id)
        }
    }

    Log.i("SHIT2", "$focusedProgramme")

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (isDataSourceSet == true) {
            val focusRequester = FocusRequester()
            val modifier = if (!isChannelInfoShown && isChannelClicked)
                Modifier
                    .focusRequester(focusRequester)
                    .focusable()
                    .onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) {
                            if (event.key == Key.DirectionCenter) {
                                isChannelClicked = false
                            }
                        }

                        true
                    }
                else Modifier

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
            }, modifier = modifier
                .fillMaxSize()
            )
        }

        if (!isChannelClicked) {
            Row(
                Modifier.fillMaxSize()
            ) {
                channels?.let { channels ->
                    ChannelList(
                        Modifier.fillMaxWidth(0.5f),
                        channels,
                        if (isChannelsListFocused) focusedChannel else -1,
                        {key -> handleChannelOnKeyEvent(key)},
                    ) { url ->
                        mediaViewModel.setMediaUrl(url)
                    }
                }

                epg?.let { currentChannelEpg ->
                    EpgList(
                        Modifier.fillMaxSize(),
                        currentChannelEpg,
                        if (!isChannelsListFocused) focusedProgramme else -1
                    ) {
                        key -> handleEpgOnKeyEvent(key)
                    }
                }
            }
        } else {
            if (isChannelInfoShown) {
                channels?.get(focusedChannel)?.let { channel ->
                    epg?.get(focusedProgramme)?.let { e ->
                        ChannelInfo(
                            focusedChannel,
                            channel,
                            e,
                            Modifier.align(Alignment.BottomCenter),
                            { previous -> epg?.let { epg ->
                                epg[if (previous) focusedProgramme - 1 else focusedProgramme + 1]
                            } ?: e },
                            { backward -> if (backward) focusedProgramme -= 1 else focusedProgramme += 1 }
                        ) { show -> isChannelInfoShown = show }
                    }
                }
            }
        }
    }
}