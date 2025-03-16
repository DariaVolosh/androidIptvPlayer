package com.example.iptvplayer.view.channelInfo

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.iptvplayer.R
import com.example.iptvplayer.view.channels.ArchiveViewModel
import com.example.iptvplayer.view.channels.MediaViewModel
import com.example.iptvplayer.view.epg.EpgViewModel
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PlaybackControls(
    channelUrl: String,
    onSeekingStarted: (Boolean) -> Unit,
    resetSecondsNotInteracted: () -> Unit,
    setIsLiveProgramme: (Boolean) -> Unit,
    showProgrammeDatePicker: (Boolean) -> Unit
) {
    var focusedControl by remember { mutableIntStateOf(R.string.pause) }
    val coroutineScope = rememberCoroutineScope()

    val archiveViewModel: ArchiveViewModel = hiltViewModel()
    val mediaViewModel: MediaViewModel = hiltViewModel()
    val epgViewModel: EpgViewModel = hiltViewModel()

    val focusedEpgIndex by epgViewModel.focusedEpgIndex.observeAsState()

    val isPaused by mediaViewModel.isPaused.observeAsState()

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

            if (prevProgram != null) {
                archiveViewModel.setCurrentTime(prevProgram.startTime)
                archiveViewModel.getArchiveUrl(channelUrl)
                onSeekingStarted(true)
                epgViewModel.updateFocusedEpgIndex(focusedEpgIndex - 1)
                onSeekingStarted(false)
            }
        }
    }

    val handleNextProgramClick: () -> Unit = {
        focusedEpgIndex?.let { focusedEpgIndex ->
            resetSecondsNotInteracted()
            val nextProgram = epgViewModel.getEpgByIndex(focusedEpgIndex + 1)

            if (nextProgram != null) {
                archiveViewModel.setCurrentTime(nextProgram.startTime)
                archiveViewModel.getArchiveUrl(channelUrl)
                onSeekingStarted(true)
                epgViewModel.updateFocusedEpgIndex(focusedEpgIndex + 1)
                onSeekingStarted(false)
            }
        }
    }

    val handleBackOnClick = {
        resetSecondsNotInteracted()
        onSeekingStarted(true)
        archiveViewModel.seekBack()
    }

    val handleOnFingerLiftedUp = {
        onSeekingStarted(false)
        archiveViewModel.getArchiveUrl(channelUrl)
    }

    val handlePauseOnClick = {
        resetSecondsNotInteracted()
        mediaViewModel.pause()
    }

    val handlePlayOnClick = {
        resetSecondsNotInteracted()
        mediaViewModel.play()
    }

    val handleNextOnClick = {
        resetSecondsNotInteracted()
        onSeekingStarted(true)
        archiveViewModel.seekForward()
    }

    val handleGoLiveOnClick: () -> Unit = {
        resetSecondsNotInteracted()
        onSeekingStarted(true)
        coroutineScope.launch {
            epgViewModel.liveProgramme.value?.let { l ->
                epgViewModel.updateFocusedEpgIndex(l)
            }
            mediaViewModel.setMediaUrl(channelUrl)
            archiveViewModel.liveTime.value?.let { t ->
                archiveViewModel.setCurrentTime(t)
            }
            setIsLiveProgramme(true)
            onSeekingStarted(false)
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
            {}, handleBackOnClick, handleOnFingerLiftedUp
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
            {}, handleNextOnClick, handleOnFingerLiftedUp
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

    Log.i("SHIT3", "COMPOSED")

    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        playbackControls.map { control ->
            PlaybackControl(control)
        }
    }
}