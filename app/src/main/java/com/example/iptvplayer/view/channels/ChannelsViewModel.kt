package com.example.iptvplayer.view.channels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iptvplayer.data.PlaylistChannel
import com.example.iptvplayer.domain.GetChannelsDataUseCase
import com.example.iptvplayer.domain.ReadFileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChannelsViewModel @Inject constructor(
    private val readFileUseCase: ReadFileUseCase,
    private val getChannelsDataUseCase: GetChannelsDataUseCase
): ViewModel() {
    private val _channels: MutableLiveData<List<PlaylistChannel>> = MutableLiveData()
    val channels: LiveData<List<PlaylistChannel>> = _channels

    private val _channelNames: MutableLiveData<Set<String>> = MutableLiveData()
    val channelNames: LiveData<Set<String>> = _channelNames

    private val _focusedChannelIndex: MutableLiveData<Int> = MutableLiveData()
    val focusedChannelIndex: LiveData<Int> = _focusedChannelIndex

    private val _focusedChannel: MutableLiveData<PlaylistChannel> = MutableLiveData()
    val focusedChannel: LiveData<PlaylistChannel> = _focusedChannel

    private fun updateFocusedChannel() {
        _focusedChannel.value = channels.value?.getOrNull(focusedChannelIndex.value ?: 0)
    }

    fun updateFocusedChannel(focused: Int) {
        _channels.value?.size?.let { channelsSize ->
            if (focused < channelsSize || focused >= 0) {
                _focusedChannelIndex.value = focused
                updateFocusedChannel()
            }
        }
    }

    fun getChannelByIndex(index: Int): PlaylistChannel? {
        return _channels.value?.getOrNull(index)
    }

    fun parsePlaylist() {
        viewModelScope.launch {
            val playlistContent =
                readFileUseCase.readFile("http://193.176.212.58:8080/tv/playlists/oktv2?token=oktv2")
            _channels.value = getChannelsDataUseCase.getChannelsData(playlistContent)

            val channelNames = _channels.value?.map { ch ->
                ch.name
            }?.toSet() ?: setOf()

            _channelNames.value = channelNames
            _focusedChannelIndex.value = 0
        }
    }
}