package com.example.iptvplayer.view.channels

import android.util.Log
import com.example.iptvplayer.domain.sharedPrefs.SharedPreferencesUseCase
import com.example.iptvplayer.retrofit.data.ChannelData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ChannelsManager @Inject constructor(
    private val sharedPreferencesUseCase: SharedPreferencesUseCase
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
                sharedPreferencesUseCase.saveIntValue(CURRENT_CHANNEL_INDEX_KEY, index)
            } else {
                Log.i("update channel index called", "$index")
                _focusedChannelIndex.value = index
            }
        }
    }

    fun updateCurrentChannel() {
        val channel = channelsData.value.getOrNull(_currentChannelIndex.value)
        channel?.let { _ ->
            _currentChannel.value = channel
            Log.i("update focused channel", channel.toString())
        }
    }

    fun updateChannelsData(data: List<ChannelData>) {
        _channelsData.value = data
    }

    fun getChannelByIndex(index: Int): ChannelData? {
        return channelsData.value.getOrNull(index)
    }
}