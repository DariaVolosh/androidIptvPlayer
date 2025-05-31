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
import com.example.iptvplayer.retrofit.data.EpgListItem
import com.example.iptvplayer.view.channelsAndEpgRow.ArchiveViewModel
import com.example.iptvplayer.view.epg.EpgViewModel
import com.example.iptvplayer.view.player.MediaViewModel
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

    val archiveViewModel: ArchiveViewModel = hiltViewModel()
    val mediaViewModel: MediaViewModel = hiltViewModel()
    val epgViewModel: EpgViewModel = hiltViewModel()

    val focusedEpgIndex by epgViewModel.focusedEpgIndex.collectAsState()
    val currentEpgIndex by epgViewModel.currentEpgIndex.collectAsState()

    val isPaused by mediaViewModel.isPaused.collectAsState()
    val isSeeking by mediaViewModel.isSeeking.collectAsState()

    val dvrRange by archiveViewModel.currentChannelDvrRange.collectAsState()

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
        if (dvrRange.first > 0) {
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
            dvrRange.first > 0 &&
            currentEpgIndex != -1 &&
            prevItem != null &&
            prevItem is EpgListItem.Epg &&
            prevItem.epgVideoTimeRangeSeconds.start in dvrRange.first..dvrRange.second
            )
        {
            coroutineScope.launch {
                mediaViewModel.updateIsSeeking(true)
                mediaViewModel.updateIsLive(false)
                mediaViewModel.resetPlayer()
                mediaViewModel.setCurrentTime(prevItem.epgVideoTimeRangeSeconds.start)
                archiveViewModel.getArchiveUrl(channelUrl, mediaViewModel.currentTime.value)
                epgViewModel.updateEpgIndex(currentEpgIndex - 1, true)
                epgViewModel.updateEpgIndex(currentEpgIndex - 1, false)
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
        archiveViewModel.getArchiveUrl(channelUrl, mediaViewModel.currentTime.value)
        mediaViewModel.play()
    }

    // pause button
    val handlePauseOnClick = {
        if (dvrRange.first > 0) {
            Log.i("playback controls", "pause handled")
            resetSecondsNotInteracted()
            mediaViewModel.updateIsLive(false)
            mediaViewModel.cancelTsCollectingJob()
            mediaViewModel.pause()
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

    // next program button
    val handleNextProgramClick: () -> Unit = {
        Log.i("FOCUSED", focusedEpgIndex.toString())
        resetSecondsNotInteracted()
        val nextProgram = epgViewModel.getEpgItemByIndex(currentEpgIndex + 1) as EpgListItem.Epg
        Log.i("next program", nextProgram.toString())

        // CHECK IF DVR IS AVAILABLE
        if (
            dvrRange.first > 0 && currentEpgIndex != -1 && nextProgram != null &&
            nextProgram.epgVideoTimeRangeSeconds.start in dvrRange.first..dvrRange.second
            )
        {
            coroutineScope.launch {
                mediaViewModel.updateIsSeeking(true)
                mediaViewModel.updateIsLive(false)
                mediaViewModel.setCurrentTime(nextProgram.epgVideoTimeRangeSeconds.start)
                mediaViewModel.resetPlayer()
                archiveViewModel.getArchiveUrl(channelUrl, mediaViewModel.currentTime.value)
                epgViewModel.updateEpgIndex(currentEpgIndex + 1, true)
                epgViewModel.updateEpgIndex(currentEpgIndex + 1, false)
                mediaViewModel.updateIsSeeking(false)
            }
        } else {
            archiveViewModel.setRewindError(context.getString(R.string.no_next_program))
        }
    }

    // go live button
    val handleGoLiveOnClick: () -> Unit = {
        if (channelUrl.isNotEmpty()) {
            resetSecondsNotInteracted()

            if (!mediaViewModel.isLive.value) {
                coroutineScope.launch {
                    mediaViewModel.updateIsSeeking(true)
                    mediaViewModel.updateIsLive(true)
                    mediaViewModel.setCurrentTime(mediaViewModel.liveTime.value)
                    val liveProgramIndex = epgViewModel.currentEpgLiveProgram.value
                    Log.i("live program index", liveProgramIndex.toString())
                    if (liveProgramIndex == -1) {
                        epgViewModel.resetEpgIndex(true)
                    } else {
                        epgViewModel.updateEpgIndex(liveProgramIndex, true)
                        epgViewModel.updateEpgIndex(liveProgramIndex, false)
                    }
                    mediaViewModel.resetPlayer()
                    mediaViewModel.startTsCollectingJob(channelUrl, true)
                    mediaViewModel.updateIsSeeking(false)
                }
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
                handleNextProgramClick, {}, {}
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