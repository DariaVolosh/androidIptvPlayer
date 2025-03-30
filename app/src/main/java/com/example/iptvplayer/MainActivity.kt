package com.example.iptvplayer

import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.iptvplayer.view.DummyViewModel
import com.example.iptvplayer.view.SplashScreen
import com.example.iptvplayer.view.StreamRewindFrame
import com.example.iptvplayer.view.channelInfo.ChannelInfo
import com.example.iptvplayer.view.channels.ArchiveViewModel
import com.example.iptvplayer.view.channels.ChannelList
import com.example.iptvplayer.view.channels.ChannelsViewModel
import com.example.iptvplayer.view.channels.MediaViewModel
import com.example.iptvplayer.view.epg.EpgList
import com.example.iptvplayer.view.epg.EpgViewModel
import com.example.iptvplayer.view.programDatePicker.ProgramDatePickerModal
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (applicationContext as MyApp).appComponent.inject(this)


        setContent {
            var isLoading by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                launch {
                    delay(2000)
                    isLoading = false
                }
            }

            IptvPlayerTheme {
                Crossfade(
                    targetState = isLoading,
                    animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing)
                ) { targetState ->
                    if (targetState) {
                        SplashScreen()
                    } else {
                        MainScreen()
                    }
                }
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
    val archiveViewModel: ArchiveViewModel = hiltViewModel()
    val dummyViewModel: DummyViewModel = hiltViewModel()

    val isDataSourceSet by mediaViewModel.isDataSourceSet.observeAsState()
    val isPlaybackStarted by mediaViewModel.isPlaybackStarted.observeAsState()

    val channels by channelsViewModel.channels.observeAsState()
    val focusedChannelIndex by channelsViewModel.focusedChannelIndex.observeAsState()
    val focusedChannel by channelsViewModel.focusedChannel.observeAsState()

    val epg by epgViewModel.epgList.observeAsState()
    val focusedEpgIndex by epgViewModel.focusedEpgIndex.observeAsState()
    val focusedEpg by epgViewModel.focusedEpg.observeAsState()

    val archiveSegmentUrl by archiveViewModel.archiveSegmentUrl.observeAsState()
    val liveTime by archiveViewModel.liveTime.observeAsState()
    val currentTime by archiveViewModel.currentTime.observeAsState()
    val isSeeking by archiveViewModel.isSeeking.observeAsState()
    val dvrRange by archiveViewModel.dvrRange.observeAsState()

    var isChannelClicked by remember { mutableStateOf(true) }
    var isChannelInfoShown by remember { mutableStateOf(true) }
    var isChannelsListFocused by remember { mutableStateOf(true) }
    var isProgramDatePickerShown by remember { mutableStateOf(false) }
    var dvrCollectionJob = remember { mutableStateOf<Job?>(null) }

    val isTrial by dummyViewModel.isTrial.observeAsState()

    val coroutineScope = rememberCoroutineScope()

    val handleChannelOnKeyEvent: (Key) -> Unit = { key ->
        when (key) {
            Key.DirectionDown -> {
                focusedChannelIndex?.let { focused ->
                    Log.i("FIRED", "focused channel down")
                    channelsViewModel.updateFocusedChannelIndex(focused + 1)
                }
            }
            Key.DirectionUp -> {
                focusedChannelIndex?.let { focused ->
                    channelsViewModel.updateFocusedChannelIndex(focused - 1)
                }
            }
            Key.DirectionRight -> {
                epgViewModel.updateFocusedEpg()
                isChannelsListFocused = false
            }
            Key.DirectionCenter -> {
                dvrCollectionJob.value?.cancel()
                archiveViewModel.updateIsLive(true)
                liveTime?.let { liveTime ->
                    archiveViewModel.setCurrentTime(liveTime)
                }

                focusedChannel?.let { channel ->
                    mediaViewModel.setMediaUrl(channel.url)
                    Log.i("focused channel", focusedChannel.toString())
                    dvrCollectionJob.value = coroutineScope.launch {
                        archiveViewModel.getDvrRange(channel.id)

                        dvrRange?.let { range ->
                            Log.i("focused channel", "dvr $range")
                            epgViewModel.getEpgById(channel.id, range)
                        }

                        while (true) {
                            archiveViewModel.getDvrRange(channel.id)
                            Log.i("dvr range received", dvrRange.toString())
                            delay(5000)
                        }
                    }
                }
            }

            Key.Back -> {
                Log.i("show channel info", "channel back")
                isChannelInfoShown = true
                isChannelClicked = true
            }
        }
    }

    val handleEpgOnKeyEvent: (Key) -> Unit =  { key ->
        when (key) {
            Key.DirectionDown -> {
                focusedEpgIndex?.let { focused ->
                    Log.i("FIRED", "focused epg down")
                    epgViewModel.updateFocusedEpgIndex(focused + 1)
                }
            }
            Key.DirectionUp -> {
                focusedEpgIndex?.let { focused ->
                    Log.i("FIRED", "focused epg up")
                    epgViewModel.updateFocusedEpgIndex(focused - 1)
                }
            }
            Key.DirectionLeft -> {
                isChannelsListFocused = true
            }
            Key.DirectionCenter -> {
                focusedEpg?.let { focused ->
                    if (focused.isDvrAvailable) {
                        archiveViewModel.updateIsLive(false)
                        focusedChannel?.let { channel ->
                            archiveViewModel.getArchiveUrl(channel.url)
                        }

                        focusedEpg?.let { epg ->
                            archiveViewModel.setCurrentTime(epg.startTime)
                            archiveViewModel.getArchiveUrl(focusedChannel?.url ?: "")
                        }

                        isChannelInfoShown = true
                        isChannelClicked = true
                    }
                }
            }

            Key.Back -> {
                isChannelInfoShown = true
                isChannelClicked = true
            }
        }
    }

    LaunchedEffect(Unit) {
        Log.i("initialized", "init")
        channelsViewModel.parsePlaylist()
        archiveViewModel.updateIsLive(true)
        archiveViewModel.setLiveTime(Utils.getGmtTime())
        dummyViewModel.checkIfTrial()

        liveTime?.let { liveTime ->
            archiveViewModel.setCurrentTime(liveTime)
        }

        while (true) {
            liveTime?.let { time ->
                archiveViewModel.setLiveTime(time + 1)
            }

            delay(1000)
        }
    }

    LaunchedEffect(focusedEpgIndex) {
        epgViewModel.updateFocusedEpg()
    }

    // launch first channel after application boot
   LaunchedEffect(channels) {
        channelsViewModel.updateFocusedChannelIndex(0)
        channels?.let { channels ->
            mediaViewModel.setMediaUrl(channels[0].url)
        }
    }

    LaunchedEffect(archiveSegmentUrl) {
        archiveSegmentUrl?.let { url ->
            mediaViewModel.setMediaUrl(url)
        }
    }

    // release all the resources, associated with player
    DisposableEffect(Unit) {
        onDispose {
            mediaViewModel.release()
        }
    }

    if (isTrial == true) {
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
                                when (event.key) {
                                    Key.DirectionLeft -> {
                                        isChannelsListFocused = true
                                        isChannelClicked = false
                                    }

                                    Key.DirectionCenter -> isChannelInfoShown = true
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
                            ) {
                                Log.i("SURFACE SIZE", "$format $width $height")
                            }

                            override fun surfaceDestroyed(holder: SurfaceHolder) {}
                        })
                    }
                }, modifier = modifier
                    .fillMaxSize()
                )
            }

            if (isPlaybackStarted == false) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            if (isSeeking == true) {
                StreamRewindFrame(
                    focusedChannel?.id ?: "",
                    currentTime ?: 0
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

            Row(
                Modifier.fillMaxSize()
            ) {
                channels?.let { channels ->
                    Log.i("RECCCOMPOSED", "channels list recomposed")
                    ChannelList(
                        Modifier.fillMaxWidth(0.4f),
                        channels,
                        if (isChannelsListFocused) focusedChannelIndex ?: -1 else -1,
                        isChannelsListFocused,
                        isChannelClicked
                    ) { key -> handleChannelOnKeyEvent(key) }
                }

                epg?.let { currentChannelEpg ->
                    Log.i("RECCCOMPOSED", "epg list recomposed")
                    EpgList(
                        Modifier.fillMaxWidth(),
                        currentChannelEpg,
                        focusedEpgIndex ?: -1,
                        !isChannelsListFocused,
                        isChannelClicked
                    ) {
                            key -> handleEpgOnKeyEvent(key)
                    }
                }
            }

            if (isChannelClicked) {
                ChannelInfo(
                    Modifier.align(Alignment.BottomCenter),
                    isChannelInfoShown,
                    mediaViewModel.isPaused,
                    { showDatePicker -> isProgramDatePickerShown = showDatePicker },
                ) { showChannelInfo -> isChannelInfoShown = showChannelInfo}
            }
        }
    }
}