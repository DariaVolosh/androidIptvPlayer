package com.example.iptvplayer.view.channels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iptvplayer.domain.channels.ChannelsManager
import com.example.iptvplayer.domain.channels.ChannelsOrchestrator
import com.example.iptvplayer.domain.media.MediaPlaybackOrchestrator
import com.example.iptvplayer.domain.time.TimeOrchestrator
import com.example.iptvplayer.retrofit.data.ChannelData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

const val CURRENT_CHANNEL_INDEX_KEY = "current_channel_index_key"

@HiltViewModel
class ChannelsViewModel @Inject constructor(
    private val timeOrchestrator: TimeOrchestrator,
    private val channelsOrchestrator: ChannelsOrchestrator,
    private val channelsManager: ChannelsManager,
    private val mediaPlaybackOrchestrator: MediaPlaybackOrchestrator
): ViewModel() {
    val channelsData: StateFlow<List<ChannelData>> = channelsManager.channelsData.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val focusedChannelIndex: StateFlow<Int> = channelsManager.focusedChannelIndex.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), -1
    )

    val currentChannel: StateFlow<ChannelData> = channelsManager.currentChannel.stateIn(
        viewModelScope, SharingStarted.Eagerly, ChannelData()
    )

    val currentChannelIndex: StateFlow<Int> = channelsManager.currentChannelIndex.stateIn(
        viewModelScope, SharingStarted.Eagerly, -1
    )

    private val _channelError: MutableLiveData<String> = MutableLiveData()
    val channelError: LiveData<String> = _channelError

    private val _isChannelsListFocused: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val isChannelsListFocused: StateFlow<Boolean> = _isChannelsListFocused

    private val _isChannelClicked: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val isChannelClicked: StateFlow<Boolean> = _isChannelClicked

    private val _isChannelInfoShown: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val isChannelInfoShown: StateFlow<Boolean> = _isChannelInfoShown

    fun updateIsChannelInfoShown(isShown: Boolean) {
        _isChannelInfoShown.value = isShown
    }

    fun setChannelError(error: String) {
        _channelError.value = error
    }

    // update current channel index or focused channel index
    fun updateChannelIndex(index: Int, isCurrent: Boolean) {
        channelsOrchestrator.updateChannelIndex(index, isCurrent)
    }

    fun getChannelByIndex(index: Int): ChannelData? {
        return channelsData.value.getOrNull(index)
    }

    fun setIsChannelsListFocused(isFocused: Boolean) {
        _isChannelsListFocused.value = isFocused
    }

    fun setIsChannelClicked(isClicked: Boolean) {
        Log.i("set is channel clicked", "$isClicked")
        _isChannelClicked.value = isClicked
    }

    fun switchChannel(isPrevious: Boolean) {
        viewModelScope.launch {
            timeOrchestrator.updateCurrentTime(timeOrchestrator.liveTime.value)
            mediaPlaybackOrchestrator.updateIsLive(true)

            val focusedChannelIndex =
                currentChannelIndex.value + if (isPrevious) -1 else 1
            channelsManager.updateChannelIndex(focusedChannelIndex, true)
            channelsManager.updateChannelIndex(focusedChannelIndex, false)
            val updatedCurrentChannel = channelsManager.getChannelByIndex(focusedChannelIndex)

            updatedCurrentChannel?.let { channel ->
                delay(500)
                mediaPlaybackOrchestrator.resetPlayer()
                //mediaPlaybackOrchestrator.startLivePlayback(channel.channelUrl)
                updateIsChannelInfoShown(true)
            }
        }
    }
}