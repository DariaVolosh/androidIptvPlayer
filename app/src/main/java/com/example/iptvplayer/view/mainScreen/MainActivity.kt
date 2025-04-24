package com.example.iptvplayer.view.mainScreen

import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import com.example.iptvplayer.data.Utils
import com.example.iptvplayer.ui.theme.IptvPlayerTheme
import com.example.iptvplayer.view.AuthViewModel
import com.example.iptvplayer.view.DummyViewModel
import com.example.iptvplayer.view.StreamRewindFrame
import com.example.iptvplayer.view.channelInfo.ChannelInfo
import com.example.iptvplayer.view.channels.ArchiveViewModel
import com.example.iptvplayer.view.channels.ChannelsViewModel
import com.example.iptvplayer.view.channels.MediaViewModel
import com.example.iptvplayer.view.epg.EpgViewModel
import com.example.iptvplayer.view.programDatePicker.ProgramDatePickerModal
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private lateinit var mediaViewModel: MediaViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            mediaViewModel = hiltViewModel()
            IptvPlayerTheme {
                MainScreen()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        mediaViewModel.release()
    }
}

@OptIn(UnstableApi::class)
@Composable
fun MainScreen() {
    // think during refactoring about what view models will be left in main screen
    // and maybe some of them will not be necessary to be injected here
    // same for observable live data
    val mediaViewModel: MediaViewModel = hiltViewModel()
    val archiveViewModel: ArchiveViewModel = hiltViewModel()
    val channelsViewModel: ChannelsViewModel = hiltViewModel()
    val epgViewModel: EpgViewModel = hiltViewModel()
    val dummyViewModel: DummyViewModel = hiltViewModel()
    val authViewModel: AuthViewModel = hiltViewModel()

    val context = LocalContext.current

    val token by authViewModel.token.observeAsState()

    val isDataSourceSet by mediaViewModel.isDataSourceSet.observeAsState()
    val isPlaybackStarted by mediaViewModel.isPlaybackStarted.observeAsState()

    val channels by channelsViewModel.channelsData.observeAsState()
    val focusedChannelIndex by channelsViewModel.focusedChannelIndex.observeAsState()
    val focusedChannel by channelsViewModel.focusedChannel.observeAsState()
    val channelError by channelsViewModel.channelError.observeAsState()
    val isChannelClicked by channelsViewModel.isChannelClicked.observeAsState()

    val epg by epgViewModel.epgListFlow.collectAsState(emptyList())
    val focusedEpgIndex by epgViewModel.focusedEpgIndexFlow.collectAsState(0)
    val currentEpgIndex by epgViewModel.currentEpgIndex.observeAsState()

    val archiveSegmentUrl by archiveViewModel.archiveSegmentUrl.observeAsState()
    val liveTime by archiveViewModel.liveTime.observeAsState()
    val currentTime by archiveViewModel.currentTime.observeAsState()
    val isSeeking by archiveViewModel.isSeeking.observeAsState()
    val dvrRange by archiveViewModel.dvrRange.observeAsState()
    val rewindError by archiveViewModel.rewindError.observeAsState()

    val mainFocusRequester = remember { FocusRequester() }
    var isChannelInfoShown by remember { mutableStateOf(false) }
    var isProgramDatePickerShown by remember { mutableStateOf(false) }

    val isTrial by dummyViewModel.isTrial.observeAsState()

    val datePattern = "EEEE d MMMM HH:mm:ss"

    // lambda to update epg after calendar rewind (this should belong to epg view model)
    val updateEpg: (Long) -> Unit = { currentTime ->
        Log.i("called update epg", "true $focusedEpgIndex $epg")

        var currentEpg = epg[focusedEpgIndex]
        var currentIndex = focusedEpgIndex
        var isPreviousEpg = false

        Log.i("TIMEEE", "${Utils.formatDate(currentEpg.startSeconds, datePattern)}")
        Log.i("TIMEEE", "${Utils.formatDate(currentTime, datePattern)}")

        if (currentEpg.startSeconds <= currentTime && currentEpg.stopSeconds >= currentTime) {

        } else {
            if (currentEpg.startSeconds >= currentTime) {
                isPreviousEpg = true
            }
        }

        Log.i("is previous epg", isPreviousEpg.toString())

        if (isPreviousEpg) {
            while (--currentIndex >= 0) {
                currentEpg = epg[currentIndex]
                Log.i("EPG LIST START DATE", Utils.formatDate(currentEpg.startSeconds, datePattern))
                Log.i("EPG LIST START DATE", currentEpg.epgVideoName)

                if (currentEpg.startSeconds <= currentTime && currentEpg.stopSeconds >= currentTime) {
                    epgViewModel.updateCurrentEpgIndex(currentIndex)
                    epgViewModel.updateFocusedEpgIndex(currentIndex)
                    break
                }
            }
        } else {
            while (++currentIndex < epg.size) {
                currentEpg = epg[currentIndex]

                if (currentEpg.startSeconds <= currentTime && currentEpg.stopSeconds >= currentTime) {
                    epgViewModel.updateCurrentEpgIndex(currentIndex)
                    epgViewModel.updateFocusedEpgIndex(currentIndex)
                    break
                }
            }
        }

        Log.i("update current epg LAMBDA", "-1")

        //epgViewModel.updateFocusedEpgIndex(0)
        //epgViewModel.updateCurrentEpgIndex(-1)
    }

    // think about this during refactoring
    LaunchedEffect(Unit) {
        channelsViewModel.setArchiveViewModel(archiveViewModel)
        epgViewModel.setArchiveViewModel(archiveViewModel)
        epgViewModel.setChannelsViewModel(channelsViewModel)

        Log.i("initialized", "init")
        dummyViewModel.checkIfTrial()
        authViewModel.getBackendToken()
        archiveViewModel.updateIsLive(true)
        val currentGmtTime = Utils.getGmtTime()
        Log.i("CURRENT GMT TIME", currentGmtTime.toString())

        if (currentGmtTime.toInt() != 0) {
            archiveViewModel.setLiveTime(currentGmtTime)

            while (true) {
                liveTime?.let { time ->
                    archiveViewModel.setLiveTime(time + 1)
                }

                delay(1000)
            }
        }
    }

    // probably epg list composable
    LaunchedEffect(currentEpgIndex) {
        epgViewModel.updateCurrentEpg()
    }

    // launch first channel after application boot
    // probably channels list composable
   LaunchedEffect(channels) {
        channelsViewModel.updateFocusedChannelIndex(0)
        channels?.let { channels ->
            Log.i("channel url 0", channels[0].channel[0].channelUrl.toString())
            val firstChannel = channels[0].channel[0]
            mediaViewModel.setMediaUrl(firstChannel.channelUrl)
            archiveViewModel.getDvrRange(firstChannel.name)
        }
    }

    // would be nice to create stream window composable and manage this stuff there
    // put android view with ijk player initialization there also
    LaunchedEffect(archiveSegmentUrl) {
        archiveSegmentUrl?.let { url ->
            mediaViewModel.setMediaUrl(url)
        }
    }

    // release all the resources, associated with player
    DisposableEffect(Unit) {
        onDispose {
            Log.i("on dispose called", "called")
            mediaViewModel.release()
        }
    }

    // idk? lets see
    LaunchedEffect(channelError) {
        channelError?.let { error ->
            if (error.isNotEmpty()) {
                Toast.makeText(context, channelError, Toast.LENGTH_LONG).show()
                archiveViewModel.setRewindError("")
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(mainFocusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionLeft -> {
                            channelsViewModel.setIsChannelClicked(false)
                        }

                        Key.DirectionCenter -> isChannelInfoShown = true
                    }
                }

                true
            }
    ) {
        Log.i("circular", "true")

        if (isPlaybackStarted == false) {
            Log.i("is playback started", "false")
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (isTrial == true) {
            if (isDataSourceSet == true) {
                AndroidView(
                    factory = { context ->
                        SurfaceView(context).apply {
                            holder.addCallback(object : SurfaceHolder.Callback {
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
                    }, modifier = Modifier
                        .fillMaxSize()
                )
            }

            if (isSeeking == true) {
                StreamRewindFrame(
                    focusedChannel?.name ?: "",
                    currentTime ?: 0
                )
            }

            if (isProgramDatePickerShown) {
                Log.i("is called program date picker?", "true")
                isChannelInfoShown = false
                archiveViewModel.currentTime.value?.let { currentTime ->
                    ProgramDatePickerModal(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .align(Alignment.Center),
                        currentTime
                    ) { secondsSinceEpoch ->
                        archiveViewModel.setCurrentTime(secondsSinceEpoch)
                        Log.i("called calendar", "$focusedEpgIndex")
                        if (focusedEpgIndex != -1) updateEpg(secondsSinceEpoch)
                        archiveViewModel.updateIsLive(false)
                        focusedChannelIndex?.let { focused ->
                            archiveViewModel.getArchiveUrl(
                                channelsViewModel.getChannelByIndex(
                                    focused
                                )?.channelUrl ?: ""
                            )
                        }
                        isProgramDatePickerShown = false
                        isChannelInfoShown = true
                    }
                }
            }

            ChannelsAndEpgRow(
                isChannelClicked ?: true,
                token ?: "",
                { url -> mediaViewModel.setMediaUrl(url) },
                dvrRange ?: Pair(0,0),
                { isEpgListFocused -> epgViewModel.setIsEpgListFocused(isEpgListFocused) },
                {epgViewModel.updateCurrentEpgList(focusedChannelIndex ?: -1)},
            ) { channelsData ->  epgViewModel.fetchEpg(channelsData, token ?: "")}

            if (isChannelClicked == true) {
                ChannelInfo(
                    Modifier.align(Alignment.BottomCenter),
                    isChannelInfoShown,
                    mediaViewModel.isPaused,
                    { showDatePicker -> isProgramDatePickerShown = showDatePicker }
                ) { showChannelInfo ->
                    isChannelInfoShown = showChannelInfo
                    if (!showChannelInfo) mainFocusRequester.requestFocus()
                }
            }
        }
    }
}