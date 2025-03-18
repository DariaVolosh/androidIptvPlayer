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
import com.example.iptvplayer.data.Utils
import com.example.iptvplayer.ui.theme.IptvPlayerTheme
import com.example.iptvplayer.view.channelInfo.ChannelInfo
import com.example.iptvplayer.view.channels.ArchiveViewModel
import com.example.iptvplayer.view.channels.ChannelList
import com.example.iptvplayer.view.channels.ChannelsViewModel
import com.example.iptvplayer.view.channels.MediaViewModel
import com.example.iptvplayer.view.epg.EpgList
import com.example.iptvplayer.view.epg.EpgViewModel
import com.example.iptvplayer.view.programDatePicker.ProgramDatePickerModal
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

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
    val focusedChannelIndex by channelsViewModel.focusedChannelIndex.observeAsState()

    val epg by epgViewModel.epgList.observeAsState()
    val focusedEpgIndex by epgViewModel.focusedEpgIndex.observeAsState()
    val focusedEpg by epgViewModel.focusedEpg.observeAsState()
    val liveProgramme by epgViewModel.liveProgramme.observeAsState()

    val archiveSegmentUrl by archiveViewModel.archiveSegmentUrl.observeAsState()

    var isChannelClicked by remember { mutableStateOf(false) }
    var isChannelInfoShown by remember { mutableStateOf(false) }
    var isChannelsListFocused by remember { mutableStateOf(true) }
    var isProgramDatePickerShown by remember { mutableStateOf(false) }


    LaunchedEffect(Unit) {
        channelsViewModel.parsePlaylist()
    }

    val handleChannelOnKeyEvent: (Key) -> Unit = { key ->
        when (key) {
            Key.DirectionDown -> {
                focusedChannelIndex?.let { focused ->
                    channelsViewModel.updateFocusedChannel(focused + 1)
                }
            }
            Key.DirectionUp -> {
                focusedChannelIndex?.let { focused ->
                    channelsViewModel.updateFocusedChannel(focused - 1)
                }
            }
            Key.DirectionRight -> {
                epgViewModel.updateFocusedEpg()
                isChannelsListFocused = false
            }
            Key.DirectionCenter -> {
                isChannelInfoShown = true
                isChannelClicked = true
            }
        }
    }

    val handleEpgOnKeyEvent: (Key) -> Unit =  { key ->
        when (key) {
            Key.DirectionDown -> {
                focusedEpgIndex?.let { focused ->
                    epgViewModel.updateFocusedEpgIndex(focused + 1)
                }
            }
            Key.DirectionUp -> {
                focusedEpgIndex?.let { focused ->
                    epgViewModel.updateFocusedEpgIndex(focused - 1)
                }
            }
            Key.DirectionLeft -> {
                isChannelsListFocused = true
            }
            Key.DirectionCenter -> {
                focusedEpgIndex?.let { focused ->
                    val currentEpg = epgViewModel.getEpgByIndex(focused)
                    archiveViewModel.setCurrentTime(currentEpg?.startTime ?: 0)
                }

                focusedChannelIndex?.let { focused ->
                    val currentChannel = channelsViewModel.getChannelByIndex(focused)
                    archiveViewModel.getArchiveUrl(currentChannel?.url ?: "")
                }

                isChannelInfoShown = true
                isChannelClicked = true
            }
        }
    }

    LaunchedEffect(focusedChannelIndex) {
        focusedChannelIndex?.let { focused ->
            val currentChannel = channelsViewModel.getChannelByIndex(focused)
            epgViewModel.getEpgById(currentChannel?.id ?: "-1")
            archiveViewModel.getDvrRange(currentChannel?.id ?: "-1")
        }
    }

    LaunchedEffect(focusedEpg) {
        Log.i("FOCUSED EPG", focusedEpg.toString())
    }

    LaunchedEffect(liveProgramme) {
        // updating live programme index
        epg?.let { e ->
            liveProgramme?.let { l ->
                while (true) {
                    val liveProgrammeIndex = epgViewModel.liveProgramme
                    Log.i("LIVE", "MAIN ACTIVITY $liveProgrammeIndex")
                    if (l != -1 && e[l].stopTime < Utils.getGmtTime()) {
                        epgViewModel.updateLiveProgramme(l+1)
                    }

                    // period 3 minutes
                    delay(10800)
                }
            }
        }
    }

    LaunchedEffect(archiveSegmentUrl) {
        archiveSegmentUrl?.let { url ->
            Log.i("ARCHIVE SEGMENT CHANGED", "ARCHIVE $url")
            mediaViewModel.setMediaUrl(url)
        }
    }

    LaunchedEffect(isChannelClicked) {
        if (!isChannelClicked) {
            focusedChannelIndex?.let { focused ->
                val channel = channelsViewModel.getChannelByIndex(focused)
                mediaViewModel.setMediaUrl(channel?.url ?: "")
            }
        }
    }

    LaunchedEffect(focusedEpgIndex) { }

    Log.i("SHIT2", "$focusedEpgIndex")

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
                            } else if (event.key == Key.DirectionDown) {
                                isChannelInfoShown = true
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

        if (isProgramDatePickerShown) {
            archiveViewModel.currentTime.value?.let { currentTime ->
                ProgramDatePickerModal(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .align(Alignment.Center),
                    currentTime
                ) { secondsSinceEpoch ->
                    archiveViewModel.setCurrentTime(secondsSinceEpoch)
                    focusedChannelIndex?.let { focused ->
                        archiveViewModel.getArchiveUrl(channelsViewModel.getChannelByIndex(focused)?.url ?: "")
                    }
                    isProgramDatePickerShown = false
                    isChannelInfoShown = true
                }
            }
        }

        if (!isChannelClicked) {
            Row(
                Modifier.fillMaxSize()
            ) {
                channels?.let { channels ->
                    Log.i("RECCCOMPOSED", "channels list recomposed")
                    ChannelList(
                        Modifier.fillMaxWidth(0.5f),
                        channels,
                        if (isChannelsListFocused) focusedChannelIndex ?: -1 else -1,
                        {key -> handleChannelOnKeyEvent(key)},
                    ) { url ->
                        Log.i("LAMBDA CALLED?", "CALLED")
                        mediaViewModel.setMediaUrl(url)
                    }
                }

                epg?.let { currentChannelEpg ->
                    Log.i("RECCCOMPOSED", "epg list recomposed")
                    EpgList(
                        Modifier.fillMaxWidth(),
                        currentChannelEpg,
                        focusedEpgIndex ?: -1,
                        !isChannelsListFocused,
                    ) {
                        key -> handleEpgOnKeyEvent(key)
                    }
                }
            }
        } else {
            ChannelInfo(
                Modifier.align(Alignment.BottomCenter),
                isChannelInfoShown,
                { showDatePicker -> isProgramDatePickerShown = showDatePicker },
            ) { showChannelInfo -> isChannelInfoShown = showChannelInfo}
        }
    }
}