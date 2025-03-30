package com.example.iptvplayer.view.channelInfo

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

    val dvrRange by archiveViewModel.dvrRange.observeAsState()
    val currentTime by archiveViewModel.currentTime.observeAsState()

    val handleOnControlFocusChanged: (Int) -> Unit = { control ->
        focusedControl = control
    }

    val handleCalendarOnClick = {
        showProgrammeDatePicker(true)
    }

    val handlePreviousProgramClick: () -> Unit = {
        focusedEpgIndex?.let { focusedEpgIndex ->
            resetSecondsNotInteracted()
            val prevProgram = epgViewModel.getEpgByIndex(focusedEpgIndex - 1)

            if (prevProgram != null && channelUrl.isNotEmpty()) {
                archiveViewModel.setCurrentTime(prevProgram.startTime)
                archiveViewModel.getArchiveUrl(channelUrl)
                archiveViewModel.updateIsSeeking(true)
                epgViewModel.updateFocusedEpgIndex(focusedEpgIndex - 1)
                archiveViewModel.updateIsSeeking(false)
            }
        }
    }

    val handleNextProgramClick: () -> Unit = {
        focusedEpgIndex?.let { focusedEpgIndex ->
            resetSecondsNotInteracted()
            val nextProgram = epgViewModel.getEpgByIndex(focusedEpgIndex + 1)

            if (nextProgram != null && channelUrl.isNotEmpty()) {
                archiveViewModel.setCurrentTime(nextProgram.startTime)
                archiveViewModel.getArchiveUrl(channelUrl)
                archiveViewModel.updateIsSeeking(true)
                epgViewModel.updateFocusedEpgIndex(focusedEpgIndex + 1)
                archiveViewModel.updateIsSeeking(false)
            }
        }
    }

    val handleBackOnLongPressed = {
        resetSecondsNotInteracted()
        mediaViewModel.pause()
        archiveViewModel.updateIsSeeking(true)
        archiveViewModel.updateIsContinuousRewind(true)
        archiveViewModel.seekBack()
    }

    val handleBackClicked = {
        resetSecondsNotInteracted()
        mediaViewModel.pause()
        archiveViewModel.updateIsSeeking(true)
        archiveViewModel.updateIsContinuousRewind(false)
        archiveViewModel.seekBack(20)
    }

    val handleNextClicked = {
        resetSecondsNotInteracted()
        mediaViewModel.pause()
        archiveViewModel.updateIsSeeking(true)
        archiveViewModel.updateIsContinuousRewind(false)
        archiveViewModel.seekForward(20)
    }

    val handleOnFingerLiftedUp = {
        archiveViewModel.onSeekFinish()
    }

    val handlePauseOnClick = {
        resetSecondsNotInteracted()
        mediaViewModel.pause()
    }

    val handlePlayOnClick = {
        resetSecondsNotInteracted()
        mediaViewModel.play()
    }

    val handleNextOnLongPressed = {
        resetSecondsNotInteracted()
        mediaViewModel.pause()
        archiveViewModel.updateIsSeeking(true)
        archiveViewModel.updateIsContinuousRewind(true)
        archiveViewModel.seekForward()
    }


    val isProgramDvrAvailable: (Int) -> Boolean = { nextIndex ->
        val res = epgViewModel.getEpgByIndex(nextIndex)?.isDvrAvailable ?: false
        res
    }

    val handleGoLiveOnClick: () -> Unit = {
        if (channelUrl.isNotEmpty()) {
            resetSecondsNotInteracted()
            archiveViewModel.updateIsSeeking(true)
            archiveViewModel.updateIsLive(true)
            coroutineScope.launch {
                epgViewModel.liveProgramme.value?.let { l ->
                    epgViewModel.updateFocusedEpgIndex(l)
                }
                mediaViewModel.setMediaUrl(channelUrl)
                archiveViewModel.liveTime.value?.let { t ->
                    archiveViewModel.setCurrentTime(t)
                }
            }
            archiveViewModel.updateIsSeeking(false)
        }
    }

    val playbackControls = listOf(
        PlaybackControl(
            R.drawable.calendar, R.string.calendar,
            focusedControl == R.string.calendar,
            true,
            {control -> handleOnControlFocusChanged(control)},
            handleCalendarOnClick, {}, {}
        ),

        PlaybackControl(
            R.drawable.previous_program, R.string.previous_program,
            focusedControl == R.string.previous_program,
            focusedEpgIndex?.let { focusedEpgIndex ->
                isProgramDvrAvailable(focusedEpgIndex - 1)
            } ?: false,
            {control -> handleOnControlFocusChanged(control)},
            handlePreviousProgramClick, {}, {}
        ),

        PlaybackControl(
            R.drawable.back, R.string.back,
            focusedControl == R.string.back,
            true,
            {control -> handleOnControlFocusChanged(control)},
            handleBackClicked, handleBackOnLongPressed, handleOnFingerLiftedUp
        ),

        if (isPaused == true) {
            PlaybackControl(
                R.drawable.play, R.string.play,
                focusedControl == R.string.play,
                true,
                {control -> handleOnControlFocusChanged(control)},
                handlePlayOnClick, {}, {}
            )
        } else {
            PlaybackControl(
                R.drawable.pause, R.string.pause,
                focusedControl == R.string.pause,
                true,
                {control -> handleOnControlFocusChanged(control)},
                handlePauseOnClick, {}, {}
            )
        },

        PlaybackControl(
            R.drawable.forward, R.string.forward,
            focusedControl == R.string.forward,
            true,
            {control -> handleOnControlFocusChanged(control)},
            handleNextClicked, handleNextOnLongPressed, handleOnFingerLiftedUp
        ),

        PlaybackControl(
            R.drawable.next_program, R.string.next_program,
            focusedControl == R.string.next_program,
            focusedEpgIndex?.let { focusedEpgIndex ->
                isProgramDvrAvailable(focusedEpgIndex + 1)
            } ?: false,
            {control -> handleOnControlFocusChanged(control)},
            handleNextProgramClick, {}, {}
        ),

        PlaybackControl(
            R.drawable.go_live, R.string.go_live,
            focusedControl == R.string.go_live,
            true,
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
            PlaybackControl(control, onBack)
        }
    }
}