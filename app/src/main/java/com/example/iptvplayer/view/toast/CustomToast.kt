package com.example.iptvplayer.view.toast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.iptvplayer.view.channels.ChannelsViewModel
import com.example.iptvplayer.view.archive.ArchiveViewModel
import kotlinx.coroutines.delay

@Composable
fun CustomToast(
    modifier: Modifier
) {
    val channelsViewModel: ChannelsViewModel = viewModel()
    val archiveViewModel: ArchiveViewModel = hiltViewModel()

    val channelError by channelsViewModel.channelError.observeAsState()
    val rewindError by archiveViewModel.rewindError.collectAsState()

    var isToastDisplayed by remember { mutableStateOf(false) }
    var displayedError by remember { mutableStateOf("") }

    LaunchedEffect(channelError) {
        if (channelError?.isNotEmpty() == true) {
            isToastDisplayed = true
            channelError?.let { error ->
                displayedError = error
            }

            delay(3000)
            isToastDisplayed = false
            channelsViewModel.setChannelError("")
        }
    }

    LaunchedEffect(rewindError) {
        if (rewindError?.isNotEmpty() == true) {
            isToastDisplayed = true
            rewindError?.let { error ->
                displayedError = error
            }

            delay(3000)
            isToastDisplayed = false
            archiveViewModel.setRewindError("")
        }
    }

    AnimatedVisibility(
        visible = isToastDisplayed,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Row(
            modifier = modifier
                .zIndex(99f)
                .background(MaterialTheme.colorScheme.primary.copy(0.7f))
                .padding(horizontal = 6.dp)
        ) {
            Text(
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onPrimary,
                text = displayedError
            )
        }
    }
}