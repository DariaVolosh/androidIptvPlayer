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
import com.example.iptvplayer.view.player.MediaManager
import com.example.iptvplayer.view.player.PlaybackOrchestrator
import com.example.iptvplayer.view.time.TimeOrchestrator
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
    private val getChannelsDataUseCase: GetChannelsDataUseCase,
    private val sharedPreferencesUseCase: SharedPreferencesUseCase,
    private val playbackOrchestrator: PlaybackOrchestrator,
    private val timeOrchestrator: TimeOrchestrator,
    private val channelsManager: ChannelsManager,
    private val mediaManager: MediaManager,
    private val errorManager: ErrorManager
): ViewModel() {
    val channelsData: StateFlow<List<ChannelData>> = channelsManager.channelsData.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val focusedChannelIndex: StateFlow<Int> = channelsManager.focusedChannelIndex.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), -1
    )

    val currentChannel: StateFlow<ChannelData> = channelsManager.currentChannel.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), ChannelData()
    )

    val currentChannelIndex: StateFlow<Int> = channelsManager.currentChannelIndex.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), -1
    )

    private val _channelError: MutableLiveData<String> = MutableLiveData()
    val channelError: LiveData<String> = _channelError

    private val _isChannelsListFocused: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val isChannelsListFocused: StateFlow<Boolean> = _isChannelsListFocused

    private val _isChannelClicked: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val isChannelClicked: StateFlow<Boolean> = _isChannelClicked

    val currentTime: StateFlow<Long> = timeOrchestrator.currentTime.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0L
    )

    private val _isChannelInfoShown: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val isChannelInfoShown: StateFlow<Boolean> = _isChannelInfoShown

    fun updateIsChannelInfoShown(isShown: Boolean) {
        _isChannelInfoShown.value = isShown
    }

    fun getCachedChannelIndex(): Int {
        val cachedChannelIndex = sharedPreferencesUseCase.getIntValue((CURRENT_CHANNEL_INDEX_KEY))
        return if (cachedChannelIndex == -1) 0 else cachedChannelIndex
    }

    fun setChannelError(error: String) {
        _channelError.value = error
    }

    fun updateCurrentChannel() {
        channelsManager.updateCurrentChannel()
    }

    // update current channel index or focused channel index
    fun updateChannelIndex(index: Int, isCurrent: Boolean) {
        channelsManager.updateChannelIndex(index, isCurrent)
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

            channelsManager.updateChannelsData(data)
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

    fun switchChannel(isPrevious: Boolean) {
        viewModelScope.launch {
            timeOrchestrator.updateCurrentTime(currentTime.value)
            mediaManager.updateIsLive(true)

            val focusedChannelIndex =
                currentChannelIndex.value + if (isPrevious) -1 else 1
            channelsManager.updateChannelIndex(focusedChannelIndex, true)
            channelsManager.updateChannelIndex(focusedChannelIndex, false)

            val updatedCurrentChannel =
                channelsManager.getChannelByIndex(focusedChannelIndex)
            updatedCurrentChannel?.let { channel ->
                delay(500)
                mediaManager.resetPlayer()
                playbackOrchestrator.startTsCollectingJob(channel.channelUrl)
                updateIsChannelInfoShown(true)
            }
        }
    }
}