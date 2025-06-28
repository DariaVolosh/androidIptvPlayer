package com.example.iptvplayer.view.media

import android.view.Surface
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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

    val channelsState: StateFlow<ChannelsState> = channelsOrchestrator.channelsState.stateIn(
        viewModelScope, SharingStarted.Eagerly, ChannelsState.FETCHING
    )

    val isPlaybackStarted: StateFlow<Boolean> = mediaPlaybackOrchestrator.isPlaybackStarted.stateIn(
        viewModelScope, SharingStarted.Eagerly, false
    )

    val streamType: StateFlow<StreamTypeState> = mediaPlaybackOrchestrator.streamTypeState.stateIn(
        viewModelScope, SharingStarted.Eagerly, StreamTypeState.INITIALIZING
    )

    val isPaused: StateFlow<Boolean> = mediaPlaybackOrchestrator.isPaused.stateIn(
        viewModelScope, SharingStarted.Eagerly, false
    )

    private var startLivePlaybackJob: Job? = null
    private var startArchivePlaybackJob: Job? = null

    fun startPlayback() {
        viewModelScope.launch {
            streamType.collect { streamType ->
                when (streamType) {
                    StreamTypeState.INITIALIZING, StreamTypeState.ERROR -> {

                    }
                    StreamTypeState.LIVE -> {
                        startLivePlayback()
                        return@collect
                    }
                    StreamTypeState.ARCHIVE -> {
                        startArchivePlayback()
                        return@collect
                    }
                }
            }
        }
    }

    suspend fun startLivePlayback() {
        mediaPlaybackOrchestrator.startArchivePlayback()
    }

    suspend fun startArchivePlayback() {
        mediaPlaybackOrchestrator.startArchivePlayback()
    }

    fun pausePlayback() {
        mediaPlaybackOrchestrator.pausePlayerPlayback()
    }

    fun getLastSegmentFromQueue(): String {
        return mediaPlaybackOrchestrator.getLastTsSegmentFromQueue()
    }

    fun setPlayerSurface(surface: Surface) {
        println("surface set view model? $surface")
        mediaPlaybackOrchestrator.setPlayerSurface(surface)
    }
}