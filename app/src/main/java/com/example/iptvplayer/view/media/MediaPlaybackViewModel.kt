package com.example.iptvplayer.view.media

import androidx.lifecycle.ViewModel
import com.example.iptvplayer.di.IoDispatcher
import com.example.iptvplayer.domain.archive.ArchiveManager
import com.example.iptvplayer.domain.channels.ChannelsOrchestrator
import com.example.iptvplayer.domain.channels.ChannelsState
import com.example.iptvplayer.domain.media.MediaPlaybackOrchestrator
import com.example.iptvplayer.domain.media.StreamTypeState
import com.example.iptvplayer.view.channelsAndEpgRow.CurrentDvrInfoState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class MediaPlaybackState {
    INITIALIZING,
    PLAYBACK_STARTED,
    ERROR
}

@HiltViewModel
class MediaPlaybackViewModel @Inject constructor(
    private val channelsOrchestrator: ChannelsOrchestrator,
    private val mediaPlaybackOrchestrator: MediaPlaybackOrchestrator,
    private val archiveManager: ArchiveManager,
    @IoDispatcher private val viewModelScope: CoroutineScope
): ViewModel() {
    private val _mediaPlaybackState: MutableStateFlow<MediaPlaybackState> =
        MutableStateFlow(MediaPlaybackState.INITIALIZING)
    val mediaPlaybackState: StateFlow<MediaPlaybackState> = _mediaPlaybackState

    val currentChannelDvrInfoState: StateFlow<CurrentDvrInfoState> = archiveManager.currentChannelDvrInfoState.stateIn(
        viewModelScope, SharingStarted.Eagerly, CurrentDvrInfoState.LOADING
    )

    val streamTypeState: StateFlow<StreamTypeState> = mediaPlaybackOrchestrator.streamTypeState.stateIn(
        viewModelScope, SharingStarted.Eagerly, StreamTypeState.INITIALIZING
    )

    val channelsState: StateFlow<ChannelsState> = channelsOrchestrator.channelsState.stateIn(
        viewModelScope, SharingStarted.Eagerly, ChannelsState.FETCHING
    )

    fun updateMediaPlaybackState(updatedState: MediaPlaybackState) {
        _mediaPlaybackState.value = updatedState
    }

    fun startLivePlayback() {
        mediaPlaybackOrchestrator.startLivePlayback()
    }
}