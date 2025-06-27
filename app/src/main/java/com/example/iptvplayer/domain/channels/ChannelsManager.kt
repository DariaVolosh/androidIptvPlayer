package com.example.iptvplayer.domain.channels

import com.example.iptvplayer.data.repositories.ChannelsRepository
import com.example.iptvplayer.retrofit.data.ChannelData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChannelsManager @Inject constructor(
    private val channelsRepository: ChannelsRepository
) {
    private val _channelsData: MutableStateFlow<List<ChannelData>> = MutableStateFlow(emptyList())
    val channelsData: StateFlow<List<ChannelData>> = _channelsData

    private val _currentChannel: MutableStateFlow<ChannelData> = MutableStateFlow(ChannelData())
    val currentChannel: StateFlow<ChannelData> = _currentChannel

    // channels indices
    private val _currentChannelIndex: MutableStateFlow<Int> = MutableStateFlow(-1)
    val currentChannelIndex: StateFlow<Int> = _currentChannelIndex

    private val _focusedChannelIndex: MutableStateFlow<Int> = MutableStateFlow(-1)
    val focusedChannelIndex: StateFlow<Int> = _focusedChannelIndex

    fun updateChannelIndex(index: Int, isCurrent: Boolean) {
        if (index in channelsData.value.indices) {
            if (isCurrent) {
                _currentChannelIndex.value = index
                updateCurrentChannel()
            } else {
                _focusedChannelIndex.value = index
            }
        }
    }

    fun updateCurrentChannel() {
        val channel = channelsData.value.getOrNull(_currentChannelIndex.value)
        channel?.let { _ ->
            _currentChannel.value = channel
        }
    }

    fun updateChannelsData(data: List<ChannelData>) {
        _channelsData.value = data
    }

    fun getChannelByIndex(index: Int): ChannelData? {
        return channelsData.value.getOrNull(index)
    }

    suspend fun getChannelsData(
        channelsErrorCallback: (String, String) -> Unit
    ): List<ChannelData> {
        val streamTemplates = channelsRepository.getStreamsUrlTemplates(channelsErrorCallback)

        if (streamTemplates.isEmpty()) return emptyList()
        else return channelsRepository.parseChannelsData(streamTemplates[0].template, channelsErrorCallback)
    }
}