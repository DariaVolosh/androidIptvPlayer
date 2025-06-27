package com.example.iptvplayer.view.channelsAndEpgRow

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.iptvplayer.view.auth.AuthViewModel
import com.example.iptvplayer.view.channels.ChannelsListAndBorder
import com.example.iptvplayer.view.channels.ChannelsViewModel
import com.example.iptvplayer.view.epg.EpgListAndBorder
import com.example.iptvplayer.view.epg.EpgViewModel
import com.example.iptvplayer.view.media.MediaViewModel
import kotlinx.coroutines.launch

@Composable
fun ChannelsAndEpgRow(

) {
    val coroutineScope = rememberCoroutineScope()

    val authViewModel: AuthViewModel = hiltViewModel()
    val archiveViewModel: ArchiveViewModel = hiltViewModel()
    val channelsViewModel: ChannelsViewModel = hiltViewModel()
    val epgViewModel: EpgViewModel = hiltViewModel()
    val mediaViewModel: MediaViewModel = hiltViewModel()

    val focusedChannelIndex by channelsViewModel.focusedChannelIndex.collectAsState()
    val channelsData by channelsViewModel.channelsData.collectAsState()
    val isChannelClicked by channelsViewModel.isChannelClicked.collectAsState()
    val isChannelsListFocused by channelsViewModel.isChannelsListFocused.collectAsState()

    val isEpgListFocused by epgViewModel.isEpgListFocused.collectAsState()

    var isChannelsListScrollPerformed by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(if (isChannelClicked) 0f else 1f)

    // here probably we wil inject channels and epg view models to aggregate data
    Row(
        Modifier.fillMaxSize()
            .alpha(alpha)
            .background(
                if (!isChannelClicked) {
                    Brush.horizontalGradient(
                        Pair(0f, MaterialTheme.colorScheme.primary.copy(0.8f)),
                        Pair(0.4f, MaterialTheme.colorScheme.secondary.copy(0.8f)),
                        Pair(0.4f, MaterialTheme.colorScheme.secondary.copy(0.8f))
                    )
                } else {
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, Color.Transparent)
                    )
                }
            )
    ) {
        ChannelsListAndBorder(Modifier.fillMaxWidth(0.4f))

        EpgListAndBorder(
            Modifier.fillMaxWidth(),
            //token,
        ) { time ->
            coroutineScope.launch {
                mediaViewModel.updateCurrentTime(time)
            }
        }
    }
}