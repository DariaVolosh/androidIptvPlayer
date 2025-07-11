package com.example.iptvplayer.view.channelInfo.playbackControls

import android.util.Log
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.iptvplayer.R
import com.example.iptvplayer.domain.media.StreamTypeState
import com.example.iptvplayer.retrofit.data.EpgListItem
import com.example.iptvplayer.view.archive.ArchiveViewModel
import com.example.iptvplayer.view.archive.CurrentDvrInfoState
import com.example.iptvplayer.view.epg.EpgViewModel
import com.example.iptvplayer.view.media.MediaPlaybackViewModel
import com.example.iptvplayer.view.media.MediaViewModel
import com.example.iptvplayer.view.time.DateAndTimeViewModel
import com.example.iptvplayer.view.time.DateType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PlaybackControls(
    channelUrl: String,
    isChannelsInfoFullyVisible: Boolean,
    onBack: () -> Unit,
    resetSecondsNotInteracted: () -> Unit,
    switchChannel: (Boolean) -> Unit,
    showProgrammeDatePicker: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var focusedControl by remember { mutableIntStateOf(3) }
    var isLongPressed by remember { mutableStateOf(false) }
    var isKeyPressed by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    val datePattern = "EEEE d MMMM HH:mm:ss"

    val archiveViewModel: ArchiveViewModel = hiltViewModel()
    val mediaViewModel: MediaViewModel = hiltViewModel()
    val mediaPlaybackViewModel: MediaPlaybackViewModel = hiltViewModel()
    val epgViewModel: EpgViewModel = hiltViewModel()
    val playbackControlsViewModel: PlaybackControlsViewModel = hiltViewModel()
    val dateAndTimeViewModel: DateAndTimeViewModel = hiltViewModel()

    val focusedEpgIndex by epgViewModel.focusedEpgIndex.collectAsState()
    val currentEpgIndex by epgViewModel.currentEpgIndex.collectAsState()

    val isPaused by mediaViewModel.isPaused.collectAsState()
    val isSeeking by mediaViewModel.isSeeking.collectAsState()

    val currentDvrInfoState by archiveViewModel.currentChannelDvrInfoState.collectAsState()

    LaunchedEffect(isChannelsInfoFullyVisible) {
        Log.i("requested focus", "yea")
        if (isChannelsInfoFullyVisible) {
            focusRequester.requestFocus()
        }
    }

    LaunchedEffect(isSeeking) {
        Log.i("is seeking", isSeeking.toString())
        if (isSeeking) {
            mediaViewModel.pause()
        }
    }

    LaunchedEffect(focusedControl) {
        Log.i("playback controls", "focused control $focusedControl")
        resetSecondsNotInteracted()
    }

    val handleOnFingerLiftedUp = {
        Log.i("back key", "handled FINGER UP")
        mediaViewModel.onSeekFinish()
    }

    val handleOnControlFocusChanged: (Int) -> Unit = { control ->
        focusedControl = control
    }

    // calendar button
    val handleCalendarOnClick = {
        if (
            currentDvrInfoState != CurrentDvrInfoState.LOADING &&
            currentDvrInfoState != CurrentDvrInfoState.NOT_AVAILABLE_GLOBAL
            ) {
            showProgrammeDatePicker(true)
        } else {
            archiveViewModel.setRewindError(context.getString(R.string.archive_is_not_available))
        }
    }

    // previous program button
    val handlePreviousProgramClick: () -> Unit = {
        resetSecondsNotInteracted()
        val prevEpgIndex = epgViewModel.findFirstEpgIndexBackward(currentEpgIndex - 1)
        val prevItem = epgViewModel.getEpgItemByIndex(prevEpgIndex)
        Log.i("prev program", prevItem.toString())

        if (
            (currentDvrInfoState != CurrentDvrInfoState.LOADING &&
                    currentDvrInfoState != CurrentDvrInfoState.NOT_AVAILABLE_GLOBAL) &&
            currentEpgIndex != -1 &&
            prevItem != null &&
            prevItem is EpgListItem.Epg
        )
        {
            coroutineScope.launch {
                mediaViewModel.updateIsSeeking(true)
                mediaViewModel.updateIsLive(false)
                mediaViewModel.updateCurrentTime(prevItem.epgVideoTimeRangeSeconds.start)
                archiveViewModel.getArchiveUrl(channelUrl, dateAndTimeViewModel.currentTime.value)
                epgViewModel.updateEpgIndex(prevEpgIndex, true)
                epgViewModel.updateEpgIndex(prevEpgIndex, false)
                mediaViewModel.updateIsSeeking(false)
            }
        } else {
            archiveViewModel.setRewindError(context.getString(R.string.no_previous_program))
        }
    }

    // seek back button long press
    val handleBackOnLongPressed = {
        resetSecondsNotInteracted()
        mediaViewModel.seekBack()
    }

    // seek back button
    val handleBackClicked = {
        Log.i("back key", "handled back PRESS")
        resetSecondsNotInteracted()
        mediaViewModel.seekBack(20)
    }

    // play button
    val handlePlayOnClick = {
        resetSecondsNotInteracted()
        mediaViewModel.play()
    }

    // pause button
    val handlePauseOnClick = {
        if (
            currentDvrInfoState != CurrentDvrInfoState.LOADING &&
            currentDvrInfoState != CurrentDvrInfoState.NOT_AVAILABLE_GLOBAL
            ) {
            Log.i("playback controls", "pause handled")
            resetSecondsNotInteracted()
            mediaPlaybackViewModel.pausePlayback()
        } else {
            archiveViewModel.setRewindError(context.getString(R.string.archive_is_not_available))
        }
    }

    // seek forward button long press
    val handleNextOnLongPressed = {
        resetSecondsNotInteracted()
        mediaViewModel.seekForward()
    }

    // seek forward button
    val handleNextClicked = {
        resetSecondsNotInteracted()
        mediaViewModel.seekForward(20)
    }

    // go live button
    val handleGoLiveOnClick: () -> Unit = {
        if (channelUrl.isNotEmpty()) {
            resetSecondsNotInteracted()

            if (mediaPlaybackViewModel.streamType.value == StreamTypeState.ARCHIVE) {
                mediaViewModel.updateIsSeeking(true)
                mediaViewModel.updateIsLive(true)
                dateAndTimeViewModel.formatDate(dateAndTimeViewModel.liveTime.value, datePattern, DateType.CURRENT_FULL_DATE)
                mediaViewModel.updateCurrentTime(dateAndTimeViewModel.liveTime.value)
                mediaPlaybackViewModel.startPlayback()
                val liveProgramIndex = epgViewModel.currentEpgLiveProgram.value
                Log.i("live program index", liveProgramIndex.toString())
                if (liveProgramIndex == -1) {
                    epgViewModel.resetEpgIndex(true)
                } else {
                    epgViewModel.updateEpgIndex(liveProgramIndex, true)
                    epgViewModel.updateEpgIndex(liveProgramIndex, false)
                }
                mediaViewModel.updateIsSeeking(false)
            } else {
                archiveViewModel.setRewindError(context.getString(R.string.already_in_live))
            }
        }
    }

    val playbackControls = remember(isPaused, focusedControl) {
        listOf(
            PlaybackControl(
                R.drawable.calendar, R.string.calendar,
                focusedControl == 0,
                { control -> handleOnControlFocusChanged(control) },
                handleCalendarOnClick, {}, {}
            ),

            PlaybackControl(
                R.drawable.previous_program, R.string.previous_program,
                focusedControl == 1,
                { control -> handleOnControlFocusChanged(control) },
                handlePreviousProgramClick, {}, {}
            ),

            PlaybackControl(
                R.drawable.back, R.string.back,
                focusedControl == 2,
                { control -> handleOnControlFocusChanged(control) },
                handleBackClicked, handleBackOnLongPressed, handleOnFingerLiftedUp
            ),

            PlaybackControl(
                if (isPaused) R.drawable.play else R.drawable.pause,
                if (isPaused) R.string.play else R.string.pause,
                focusedControl == 3,
                { control -> handleOnControlFocusChanged(control)},
                if (isPaused) handlePlayOnClick else handlePauseOnClick,
                {}, {}
            ),

            PlaybackControl(
                R.drawable.forward, R.string.forward,
                focusedControl == 4,
                { control -> handleOnControlFocusChanged(control) },
                handleNextClicked, handleNextOnLongPressed, handleOnFingerLiftedUp
            ),

            PlaybackControl(
                R.drawable.next_program, R.string.next_program,
                focusedControl == 5,
                { control -> handleOnControlFocusChanged(control) },
                playbackControlsViewModel::handleNextProgramClick, {}, {}
            ),

            PlaybackControl(
                R.drawable.go_live, R.string.go_live,
                focusedControl == 6,
                { control -> handleOnControlFocusChanged(control) },
                handleGoLiveOnClick, {}, {}
            )
        )
    }

    LaunchedEffect(isLongPressed) {
        while (isLongPressed) {
            playbackControls[focusedControl].onLongPressed()
            delay(500)
        }
    }

    Row(
        modifier = Modifier
            .focusRequester(focusRequester)
            .focusable()
            .onFocusChanged {
                Log.i("focus", it.isFocused.toString())
            }
            .onKeyEvent { event ->
                Log.i("playback controls", "${event.key}")
                when (event.key) {
                    Key.DirectionCenter -> {
                        if (event.type == KeyEventType.KeyDown) {
                            if (!isKeyPressed) {
                                isKeyPressed = true
                                coroutineScope.launch {
                                    delay(400)
                                    if (isKeyPressed) isLongPressed = true
                                }
                            }
                        } else {
                            Log.i("up long pressed", isLongPressed.toString())
                            if (!isLongPressed) {
                                Log.i("up long pressed", "on pressed clicked")
                                playbackControls[focusedControl].onPressed()
                            }

                            isLongPressed = false
                            isKeyPressed = false
                            playbackControls[focusedControl].onFingerLiftedUp()
                        }
                    }

                    Key.DirectionLeft -> {
                        if (event.type == KeyEventType.KeyDown) {
                            if (focusedControl == 0) focusedControl = playbackControls.size - 1
                            else focusedControl--
                        }
                    }

                    Key.DirectionRight -> {
                        if (event.type == KeyEventType.KeyDown) {
                            if (focusedControl == playbackControls.size - 1) focusedControl = 0
                            else focusedControl++
                        }
                    }

                    Key.Back -> {
                        if (event.type == KeyEventType.KeyDown) {
                            onBack()
                        }
                    }

                    Key.DirectionUp -> {
                        if (event.type == KeyEventType.KeyDown) {
                            switchChannel(true)
                        }
                    }

                    Key.DirectionDown -> {
                        if (event.type == KeyEventType.KeyDown) {
                            switchChannel(false)
                        }
                    }
                }
                true
            }
            .padding(top = 15.dp, bottom = 17.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        playbackControls.map { control ->
            PlaybackControl(control)
        }
    }
}