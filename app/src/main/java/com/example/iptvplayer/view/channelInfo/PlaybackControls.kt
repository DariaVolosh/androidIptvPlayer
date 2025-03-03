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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.iptvplayer.R
import com.example.iptvplayer.data.PlaylistChannel
import com.example.iptvplayer.view.channels.ArchiveViewModel
import com.example.iptvplayer.view.channels.MediaViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PlaybackControls(
    onSeekingStarted: (Boolean) -> Unit,
    resetSecondsNotInteracted: () -> Unit,
    channel: PlaylistChannel
) {
    var focusedControl by remember { mutableIntStateOf(R.string.pause) }

    val archiveViewModel: ArchiveViewModel = hiltViewModel()
    val mediaViewModel: MediaViewModel = hiltViewModel()

    val isPaused by mediaViewModel.isPaused.observeAsState()
    val archiveSegmentUrl by archiveViewModel.archiveSegmentUrl.observeAsState()

    val handleBackOnClick = {
        resetSecondsNotInteracted()
        onSeekingStarted(true)
        archiveViewModel.seekBack()
    }

    val handleOnFingerLiftedUp = {
        onSeekingStarted(false)
        archiveViewModel.getArchiveUrl(channel.url)
    }

     val handleOnControlFocusChanged: (Int) -> Unit = { control ->
        focusedControl = control
    }

    val handlePauseOnClick = {
        resetSecondsNotInteracted()
        mediaViewModel.pause()
    }

    val handlePlayOnClick = {
        resetSecondsNotInteracted()
        mediaViewModel.play()
    }

    val playbackControls = listOf(
        PlaybackControl(
            R.drawable.calendar, R.string.calendar,
            focusedControl == R.string.calendar,
            {control -> handleOnControlFocusChanged(control)},
            {}, {}, {}
        ),

        PlaybackControl(
            R.drawable.previous_program, R.string.previous_program,
            focusedControl == R.string.previous_program,
            {control -> handleOnControlFocusChanged(control)},
            {}, {}, {}
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
            {}, {}, {}
        ),

        PlaybackControl(
            R.drawable.next_program, R.string.next_program,
            focusedControl == R.string.next_program,
            {control -> handleOnControlFocusChanged(control)},
            {}, {}, {}
        ),

        PlaybackControl(
            R.drawable.go_live, R.string.go_live,
            focusedControl == R.string.go_live,
            {control -> handleOnControlFocusChanged(control)},
            {}, {}, {}
        )
    )

    LaunchedEffect(focusedControl) {
        resetSecondsNotInteracted()
    }

    LaunchedEffect(archiveSegmentUrl) {
        archiveSegmentUrl?.let { url ->
            mediaViewModel.setMediaUrl(url)
        }
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