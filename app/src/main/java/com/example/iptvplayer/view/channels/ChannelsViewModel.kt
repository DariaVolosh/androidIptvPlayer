package com.example.iptvplayer.view.channels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iptvplayer.R
import com.example.iptvplayer.domain.channels.GetChannelsDataUseCase
import com.example.iptvplayer.domain.sharedPrefs.SharedPreferencesUseCase
import com.example.iptvplayer.retrofit.data.ChannelData
import com.example.iptvplayer.view.errors.ErrorData
import com.example.iptvplayer.view.errors.ErrorManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

const val CURRENT_CHANNEL_INDEX_KEY = "current_channel_index_key"

@HiltViewModel
class ChannelsViewModel @Inject constructor(
    private val getChannelsDataUseCase: GetChannelsDataUseCase,
    private val sharedPreferencesUseCase: SharedPreferencesUseCase,
    private val errorManager: ErrorManager
): ViewModel() {
    private val _channelsData: MutableStateFlow<List<ChannelData>> = MutableStateFlow(emptyList())
    val channelsData: StateFlow<List<ChannelData>> = _channelsData

    private val _focusedChannelIndex: MutableStateFlow<Int> = MutableStateFlow(-1)
    val focusedChannelIndex: StateFlow<Int> = _focusedChannelIndex

    private val _currentChannel: MutableStateFlow<ChannelData> = MutableStateFlow(ChannelData())
    val currentChannel: StateFlow<ChannelData> = _currentChannel

    private val _currentChannelIndex: MutableStateFlow<Int> = MutableStateFlow(-1)
    val currentChannelIndex: StateFlow<Int> = _currentChannelIndex

    private val _channelError: MutableLiveData<String> = MutableLiveData()
    val channelError: LiveData<String> = _channelError

    private val _isChannelsListFocused: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val isChannelsListFocused: StateFlow<Boolean> = _isChannelsListFocused

    private val _isChannelClicked: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val isChannelClicked: StateFlow<Boolean> = _isChannelClicked

    fun getCachedChannelIndex(): Int {
        val cachedChannelIndex = sharedPreferencesUseCase.getIntValue((CURRENT_CHANNEL_INDEX_KEY))
        return if (cachedChannelIndex == -1) 0 else cachedChannelIndex
    }

    fun setChannelError(error: String) {
        _channelError.value = error
    }

    fun updateCurrentChannel() {
        val channel = channelsData.value.getOrNull(_currentChannelIndex.value)
        channel?.let { _ ->
            _currentChannel.value = channel
            Log.i("update focused channel", channel.toString())
        }
    }

    // update current channel index or focused channel index
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

    fun fetchChannelsData(token: String) {
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val data = getChannelsDataUseCase.getChannelsData(token) {
                title, description ->
                    errorManager.publishError(
                        ErrorData(title, description, R.drawable.error_icon)
                    )
            }
            Log.i("channels repository", "parsed data ${data.toString()}")

            _channelsData.value = data
            val stopTime = System.currentTimeMillis()
            Log.i("parsing time","${(stopTime-startTime)} channels view model fetch channels data")
        }
    }

    /* fun parsePlaylist() {
        viewModelScope.launch {
            // "http://91.222.154.251/tv/playlists/Lait?token=Lait" - ukrainian flussonic
            // "http://193.176.212.58:8080/tv/playlists/oktv?token=oktv" - georgian flussonic
            // "https://185.15.115.246/tv/playlists/test?token=IKT0neFHTB1Ki0" lasha flussonic
            val playlistContent =
                readFileUseCase.readFile("http://185.15.115.246/tv/playlists/test?token=IKT0neFHTB1Ki0")

            if (playlistContent.isNotEmpty()) {
                val channelsData = getPlaylistDataUseCase.getPlaylistData(playlistContent)

                _playlistChannels.value = channelsData
                _focusedChannelIndex.emit(0)
            } else {
                setChannelError("Failure to fetch playlist")
            }
        }
    } */
}