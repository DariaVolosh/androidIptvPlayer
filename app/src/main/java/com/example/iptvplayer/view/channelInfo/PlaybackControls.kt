package com.example.iptvplayer.view.channelInfo

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.iptvplayer.R
import com.example.iptvplayer.view.channels.ArchiveViewModel
import com.example.iptvplayer.view.channels.MediaViewModel
import com.example.iptvplayer.view.epg.EpgViewModel
import kotlinx.coroutines.launch

@Composable
fun PlaybackControls(
    channelUrl: String,
    onBack: () -> Unit,
    isDvrAvailable: Boolean,
    resetSecondsNotInteracted: () -> Unit,
    showProgrammeDatePicker: (Boolean) -> Unit
) {
    var focusedControl by remember { mutableIntStateOf(R.string.pause) }
    val coroutineScope = rememberCoroutineScope()

    val archiveViewModel: ArchiveViewModel = hiltViewModel()
    val mediaViewModel: MediaViewModel = hiltViewModel()
    val epgViewModel: EpgViewModel = hiltViewModel()

    val focusedEpgIndex by epgViewModel.focusedEpgIndex.observeAsState()

    val isPaused by mediaViewModel.isPaused.observeAsState()

    val isSeeking by archiveViewModel.isSeeking.observeAsState()

    LaunchedEffect(isSeeking) {
        isSeeking?.let { isSeeking ->
            if (isSeeking) {
                mediaViewModel.pause()
            }
        }
    }

    val handleOnControlFocusChanged: (Int) -> Unit = { control ->
        focusedControl = control
    }

    val handleCalendarOnClick = {
        if (isDvrAvailable) {
            showProgrammeDatePicker(true)
        } else {
            archiveViewModel.setRewindError("Archive is not available")
        }
    }

    val handlePreviousProgramClick: () -> Unit = {
        focusedEpgIndex?.let { focusedEpgIndex ->
            resetSecondsNotInteracted()
            val prevProgram = epgViewModel.getEpgByIndex(focusedEpgIndex - 1)
            Log.i("prev program", prevProgram.toString())

            if (prevProgram != null && channelUrl.isNotEmpty() && prevProgram.isDvrAvailable) {
                archiveViewModel.updateIsLive(false)
                archiveViewModel.setCurrentTime(prevProgram.startTime)
                archiveViewModel.getArchiveUrl(channelUrl)
                archiveViewModel.updateIsSeeking(true)
                epgViewModel.updateCurrentEpgIndex(focusedEpgIndex - 1)
                epgViewModel.updateFocusedEpgIndex(focusedEpgIndex - 1)
                archiveViewModel.updateIsSeeking(false)
            } else {
                archiveViewModel.setRewindError("Previous program is not available")
            }
        }
    }

    val handleNextProgramClick: () -> Unit = {
        focusedEpgIndex?.let { focusedEpgIndex ->
            Log.i("FOCUSED", focusedEpgIndex.toString())
            resetSecondsNotInteracted()
            val nextProgram = epgViewModel.getEpgByIndex(focusedEpgIndex + 1)

            if (nextProgram != null && channelUrl.isNotEmpty() && nextProgram.isDvrAvailable) {
                archiveViewModel.updateIsLive(false)
                archiveViewModel.setCurrentTime(nextProgram.startTime)
                archiveViewModel.getArchiveUrl(channelUrl)
                archiveViewModel.updateIsSeeking(true)
                epgViewModel.updateCurrentEpgIndex(focusedEpgIndex + 1)
                epgViewModel.updateFocusedEpgIndex(focusedEpgIndex + 1)
                archiveViewModel.updateIsSeeking(false)
            } else {
                archiveViewModel.setRewindError("Next program is not available")
            }
        }
    }

    val handleBackOnLongPressed = {
        resetSecondsNotInteracted()
        archiveViewModel.seekBack()
    }

    val handleBackClicked = {
        resetSecondsNotInteracted()
        archiveViewModel.seekBack(20)
    }

    val handleNextClicked = {
        resetSecondsNotInteracted()
        archiveViewModel.seekForward(20)
    }

    val handleOnFingerLiftedUp = {
        archiveViewModel.onSeekFinish()
    }

    val handlePauseOnClick = {
        if (isDvrAvailable) {
            focusedControl = R.string.play
            resetSecondsNotInteracted()
            mediaViewModel.pause()
        } else {
            archiveViewModel.setRewindError("Archive is not available")
        }
    }

    val handlePlayOnClick = {
        focusedControl = R.string.pause
        resetSecondsNotInteracted()
        mediaViewModel.play()
    }

    val handleNextOnLongPressed = {
        resetSecondsNotInteracted()
        archiveViewModel.seekForward()
    }

    val handleGoLiveOnClick: () -> Unit = {
        if (channelUrl.isNotEmpty()) {
            resetSecondsNotInteracted()

            if (archiveViewModel.isLive.value == false) {
                archiveViewModel.updateIsLive(true)
                archiveViewModel.updateIsSeeking(true)
                coroutineScope.launch {
                    epgViewModel.liveProgrammeIndex.value?.let { l ->
                        epgViewModel.updateCurrentEpgIndex(l)
                        epgViewModel.updateFocusedEpgIndex(l)
                    }
                    mediaViewModel.setMediaUrl(channelUrl)
                }
                archiveViewModel.updateIsSeeking(false)
            } else {
                archiveViewModel.setRewindError("Already in live")
            }
        }
    }

    val playbackControls = listOf(
        PlaybackControl(
            R.drawable.calendar, R.string.calendar,
            focusedControl == R.string.calendar,
            {control -> handleOnControlFocusChanged(control)},
            handleCalendarOnClick, {}, {}
        ),

        PlaybackControl(
            R.drawable.previous_program, R.string.previous_program,
            focusedControl == R.string.previous_program,
            {control -> handleOnControlFocusChanged(control)},
            handlePreviousProgramClick, {}, {}
        ),

        PlaybackControl(
            R.drawable.back, R.string.back,
            focusedControl == R.string.back,
            {control -> handleOnControlFocusChanged(control)},
            handleBackClicked, handleBackOnLongPressed, handleOnFingerLiftedUp
        ),

        if (isPaused == true) {
            PlaybackControl(
                R.drawable.play, R.string.play,
                focusedControl == R.string.play,
                {control -> handleOnControlFocusChanged(control)},
                handlePlayOnClick, {}, {}
            )
        } else {
            PlaybackControl(
                R.drawable.pause, R.string.pause,
                focusedControl == R.string.pause,
                {control -> handleOnControlFocusChanged(control)},
                handlePauseOnClick, {}, {}
            )
        },

        PlaybackControl(
            R.drawable.forward, R.string.forward,
            focusedControl == R.string.forward,
            {control -> handleOnControlFocusChanged(control)},
            handleNextClicked, handleNextOnLongPressed, handleOnFingerLiftedUp
        ),

        PlaybackControl(
            R.drawable.next_program, R.string.next_program,
            focusedControl == R.string.next_program,
            {control -> handleOnControlFocusChanged(control)},
            handleNextProgramClick, {}, {}
        ),

        PlaybackControl(
            R.drawable.go_live, R.string.go_live,
            focusedControl == R.string.go_live,
            {control -> handleOnControlFocusChanged(control)},
            handleGoLiveOnClick, {}, {}
        )
    )

    LaunchedEffect(focusedControl) {
        resetSecondsNotInteracted()
    }

    Row(
        modifier = Modifier
            .padding(top = 15.dp, bottom = 17.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        playbackControls.map { control ->
            PlaybackControl(control, isPaused ?: false, onBack)
        }
    }
}