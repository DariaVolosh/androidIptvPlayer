package com.example.iptvplayer.view.mainScreen

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import com.example.iptvplayer.retrofit.data.ChannelData
import com.example.iptvplayer.ui.theme.IptvPlayerTheme
import com.example.iptvplayer.view.DummyViewModel
import com.example.iptvplayer.view.channelInfo.ChannelInfo
import com.example.iptvplayer.view.channels.ChannelsViewModel
import com.example.iptvplayer.view.channelsAndEpgRow.ArchiveViewModel
import com.example.iptvplayer.view.channelsAndEpgRow.ChannelsAndEpgRow
import com.example.iptvplayer.view.player.MediaViewModel
import com.example.iptvplayer.view.player.PlayerView
import com.example.iptvplayer.view.programDatePicker.ProgramDatePickerModal
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mediaViewModel: MediaViewModel by viewModels()
    private val channelsViewModel: ChannelsViewModel by viewModels()
    private val archiveViewModel: ArchiveViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val splashScreen = installSplashScreen()
        var keepSplashScreen = true
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }

        lifecycleScope.launch {
            delay(1000)
            keepSplashScreen = false
        }

        setContent {
            IptvPlayerTheme {
                MainScreen(
                    onExitApp = {finish()}
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                channelsViewModel.currentChannel.collect { currentChannel ->
                    if (currentChannel != ChannelData()) {
                        if (mediaViewModel.isLive.value) {
                            mediaViewModel.startTsCollectingJob(currentChannel.channelUrl, true)
                        } else {
                            archiveViewModel.getArchiveUrl(
                                currentChannel.channelUrl,
                                mediaViewModel.currentTime.value
                            )

                            if (archiveViewModel.archiveSegmentUrl.value.isNotEmpty()) {
                                mediaViewModel.play()
                            }
                        }

                        cancel()
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (mediaViewModel.isLive.value) {
            mediaViewModel.resetPlayer()
        } else {
            mediaViewModel.updateIsLive(false)
            mediaViewModel.cancelTsCollectingJob()
            mediaViewModel.pause()
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun MainScreen(
    onExitApp: () -> Unit
) {
    val context = LocalContext.current

    val mainFocusRequester = remember { FocusRequester() }
    var isChannelInfoShown by remember { mutableStateOf(false) }
    var isChannelInfoFullyVisible by remember { mutableStateOf(false) }
    var isProgramDatePickerShown by remember { mutableStateOf(false) }
    var isBackPressed by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    val channelsViewModel: ChannelsViewModel = hiltViewModel()
    val mediaViewModel: MediaViewModel = hiltViewModel()
    val archiveViewModel: ArchiveViewModel = hiltViewModel()
    val dummyViewModel: DummyViewModel = hiltViewModel()

    val channelError by channelsViewModel.channelError.observeAsState()
    val isTrial by dummyViewModel.isTrial.observeAsState()
    val currentChannel by channelsViewModel.currentChannel.collectAsState()

    val switchChannel: (Boolean) -> Unit = { previous ->
        coroutineScope.launch {
            mediaViewModel.setCurrentTime(mediaViewModel.liveTime.value)
            mediaViewModel.updateIsLive(true)

            val focusedChannelIndex =
                channelsViewModel.focusedChannelIndex.value + if (previous) -1 else 1
            channelsViewModel.updateChannelIndex(focusedChannelIndex, true)
            channelsViewModel.updateChannelIndex(focusedChannelIndex, false)

            val updatedCurrentChannel =
                channelsViewModel.getChannelByIndex(focusedChannelIndex)
            updatedCurrentChannel?.let { channel ->
                delay(500)
                mediaViewModel.resetPlayer()
                mediaViewModel.startTsCollectingJob(channel.channelUrl, true)
                isChannelInfoShown = true
            }
        }
    }

    LaunchedEffect(isChannelInfoShown, isProgramDatePickerShown, isBackPressed) {
        Log.i("is main screen focused", "$isChannelInfoShown $isProgramDatePickerShown")
        if (!isChannelInfoShown && !isProgramDatePickerShown && !isBackPressed) {
            mainFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(isChannelInfoShown) {
        if (isChannelInfoShown) {
            delay(300)
            isChannelInfoFullyVisible = true
        } else {
            isChannelInfoFullyVisible = false
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

    // think about this during refactoring
    LaunchedEffect(Unit) {
        dummyViewModel.checkIfTrial()
    }


    Box(
        modifier = Modifier
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

                        Key.DirectionUp -> {
                            switchChannel(true)
                        }

                        Key.DirectionDown -> {
                            switchChannel(false)
                        }

                        Key.DirectionCenter -> isChannelInfoShown = true

                        Key.Back -> {
                            isBackPressed = true
                        }
                    }
                }

                true
            }
    ) {
        if (isTrial == true) {
            PlayerView(
                isBackPressed,
                { isBackPressed = false },
                onExitApp
            )
            ChannelsAndEpgRow()

            if (isProgramDatePickerShown) {
                Log.i("is called program date picker?", "true")
                archiveViewModel.setRewindError("")
                isChannelInfoShown = false

                ProgramDatePickerModal(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .align(Alignment.Center),
                    currentChannel = currentChannel,
                    { isProgramDatePickerShown = false}
                ) { isChannelInfoShown = true }
            }

            AnimatedVisibility(
                visible = isChannelInfoShown,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300)),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                ChannelInfo(
                    isChannelInfoFullyVisible,
                    { showDatePicker -> isProgramDatePickerShown = showDatePicker },
                    switchChannel,
                ) { showChannelInfo ->
                    isChannelInfoShown = showChannelInfo
                    if (!showChannelInfo) mainFocusRequester.requestFocus()
                }
            }
        }
    }
}