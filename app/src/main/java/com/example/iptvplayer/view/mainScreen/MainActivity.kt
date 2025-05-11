package com.example.iptvplayer.view.mainScreen

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import com.example.iptvplayer.ui.theme.IptvPlayerTheme
import com.example.iptvplayer.view.PlayerView
import com.example.iptvplayer.view.channelInfo.ChannelInfo
import com.example.iptvplayer.view.channels.ArchiveViewModel
import com.example.iptvplayer.view.channels.ChannelsViewModel
import com.example.iptvplayer.view.channels.MediaViewModel
import com.example.iptvplayer.view.epg.EpgViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    //private val mediaViewModel: MediaViewModel by viewModels()
    private val channelsViewModel: ChannelsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //val splashScreen = installSplashScreen()
        //var keepSplashScreen = true
        //splashScreen.setKeepOnScreenCondition { keepSplashScreen }

        /*lifecycleScope.launch {
            delay(1000)
            keepSplashScreen = false
        } */

        setContent {
            IptvPlayerTheme {
                MainScreen()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Log.i("activity hashcode", this.hashCode().toString())
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onStop() {
        super.onStop()
        //mediaViewModel.reset()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("on destroy", "called")
    }
}

@OptIn(UnstableApi::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current

    // think during refactoring about what view models will be left in main screen
    // and maybe some of them will not be necessary to be injected here
    // same for observable live data
    /*val archiveViewModel: ArchiveViewModel = hiltViewModel()
    val channelsViewModel: ChannelsViewModel = hiltViewModel()
    val epgViewModel: EpgViewModel = hiltViewModel()
    val dummyViewModel: DummyViewModel = hiltViewModel()
    val authViewModel: AuthViewModel = hiltViewModel()

    val token by authViewModel.token.observeAsState()
*/
    //val isPlaybackStarted by mediaViewModel.isPlaybackStarted.observeAsState()

    /*val channels by channelsViewModel.channelsData.observeAsState()
    val focusedChannelIndex by channelsViewModel.focusedChannelIndex.observeAsState()
    val focusedChannel by channelsViewModel.currentChannel.observeAsState()
    val channelError by channelsViewModel.channelError.observeAsState()
    val isChannelClicked by channelsViewModel.isChannelClicked.observeAsState()

    val epg by epgViewModel.epgList.observeAsState()
    val focusedEpgIndex by epgViewModel.focusedEpgIndex.observeAsState()
    val currentEpgIndex by epgViewModel.currentEpgIndex.observeAsState()

    val archiveSegmentUrl by archiveViewModel.archiveSegmentUrl.observeAsState()
    val liveTime by archiveViewModel.liveTime.observeAsState()
    val currentTime by archiveViewModel.currentTime.observeAsState()
    val isSeeking by archiveViewModel.isSeeking.observeAsState()
    val dvrRange by archiveViewModel.dvrRange.observeAsState()

    var isChannelInfoShown by remember { mutableStateOf(false) }
    var isProgramDatePickerShown by remember { mutableStateOf(false) }
    var isInitialStreamSet by remember { mutableStateOf(false) }

    val isTrial by dummyViewModel.isTrial.observeAsState()

    val datePattern = "EEEE d MMMM HH:mm:ss" */

    // lambda to update epg after calendar rewind (this should belong to epg view model)
    /*val updateEpg: (Long) -> Unit = { currentTime ->
        Log.i("called update epg", "true $focusedEpgIndex $epg")

        val localEpgList = epg
        val localFocusedEpgIndex = focusedEpgIndex
        if (localEpgList != null && localFocusedEpgIndex != null) {
            var currentEpg = localEpgList[localFocusedEpgIndex]
            var currentIndex = localFocusedEpgIndex
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
                    currentEpg = localEpgList[currentIndex]
                    Log.i("EPG LIST START DATE", Utils.formatDate(currentEpg.startSeconds, datePattern))
                    Log.i("EPG LIST START DATE", currentEpg.epgVideoName)

                    if (currentEpg.startSeconds <= currentTime && currentEpg.stopSeconds >= currentTime) {
                        epgViewModel.updateEpgIndex(currentIndex, true)
                        epgViewModel.updateEpgIndex(currentIndex, false)
                        break
                    }
                }
            } else {
                while (++currentIndex < localEpgList.size) {
                    currentEpg = localEpgList[currentIndex]

                    if (currentEpg.startSeconds <= currentTime && currentEpg.stopSeconds >= currentTime) {
                        epgViewModel.updateEpgIndex(currentIndex, true)
                        epgViewModel.updateEpgIndex(currentIndex, false)
                        break
                    }
                }
            }

            if (currentIndex == -1 || currentIndex == localEpgList.size) {
                Log.i("update current epg LAMBDA", "-1")

                epgViewModel.updateEpgIndex(0, false)
                epgViewModel.resetEpgIndex(true)
            }
        }
    } */

    // think about this during refactoring
    LaunchedEffect(Unit) {
        //dummyViewModel.checkIfTrial()
    }

    // probably epg list composable
    /*LaunchedEffect(currentEpgIndex) {
        epgViewModel.updateCurrentEpg()
    } */

    // probably channels list composable
   /*LaunchedEffect(channels, focusedChannel, dvrRange) {
       channels?.let { _ ->
           val focusedChannelLocal = focusedChannel
           Log.i("init focused channel", focusedChannelLocal.toString())

           // initial application boot (set focused channel to first one and play it)
           // if focused channel is set, application is resuming, just skip and play
           if (!isInitialStreamSet && focusedChannelLocal != null) {
               dvrRange?.let { range ->
                   isInitialStreamSet = true

                   if (range.first > 0 && archiveViewModel.isLive.value == false) {
                       //archiveViewModel.getArchiveUrl(focusedChannelLocal.channelUrl)
                   } else {
                       //mediaViewModel.setMediaUrl(focusedChannelLocal.channelUrl)
                   }
               }
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
    } */

    val mainFocusRequester = remember { FocusRequester() }
    var isChannelInfoShown by remember { mutableStateOf(false) }
    var isProgramDatePickerShown by remember { mutableStateOf(false) }

    val channelsViewModel: ChannelsViewModel = hiltViewModel()
    val epgViewModel: EpgViewModel = hiltViewModel()
    val mediaViewModel: MediaViewModel = hiltViewModel()
    val archiveViewModel: ArchiveViewModel = hiltViewModel()


    Box(
        modifier = Modifier
            .background(Color.Black)
            .fillMaxSize()
            .focusRequester(mainFocusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    Log.i("main activity", "${event.key} captured")

                    when (event.key) {
                        Key.DirectionLeft -> {
                            Log.i("main activity left pressed", "true")
                            channelsViewModel.setIsChannelClicked(false)
                        }

                        Key.DirectionCenter -> isChannelInfoShown = true
                    }
                }

                true
            }
    ) {
        PlayerView()
        ChannelsAndEpgRow()

        /*if (isSeeking == true) {
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
                    if (focusedEpgIndex != null) updateEpg(secondsSinceEpoch)
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
        } */

        AnimatedVisibility(
            visible = isChannelInfoShown,
            enter = fadeIn(animationSpec = tween(600)),
            exit = fadeOut(animationSpec = tween(600)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            ChannelInfo(
                { showDatePicker -> isProgramDatePickerShown = showDatePicker }
            ) { showChannelInfo ->
                isChannelInfoShown = showChannelInfo
                if (!showChannelInfo) mainFocusRequester.requestFocus()
            }
        }
    }
}