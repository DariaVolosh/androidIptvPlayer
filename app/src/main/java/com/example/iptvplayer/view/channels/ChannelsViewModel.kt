package com.example.iptvplayer.view.channels

import android.util.Log
import androidx.compose.ui.input.key.Key
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.example.iptvplayer.data.PlaylistChannel
import com.example.iptvplayer.domain.GetChannelsDataUseCase
import com.example.iptvplayer.domain.GetPlaylistDataUseCase
import com.example.iptvplayer.domain.GetStreamsUrlTemplatesUseCase
import com.example.iptvplayer.domain.ReadFileUseCase
import com.example.iptvplayer.domain.SharedPreferencesUseCase
import com.example.iptvplayer.retrofit.data.ChannelBackendInfo
import com.example.iptvplayer.retrofit.data.ChannelData
import com.example.iptvplayer.retrofit.data.StreamUrlTemplate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import javax.inject.Inject

const val CURRENT_CHANNEL_INDEX_KEY = "current_channel_index_key"

@HiltViewModel
class ChannelsViewModel @Inject constructor(
    private val readFileUseCase: ReadFileUseCase,
    private val getPlaylistDataUseCase: GetPlaylistDataUseCase,
    private val getChannelsDataUseCase: GetChannelsDataUseCase,
    private val getStreamsUrlTemplatesUseCase: GetStreamsUrlTemplatesUseCase,
    private val sharedPreferencesUseCase: SharedPreferencesUseCase
): ViewModel() {
    private val _playlistChannels: MutableLiveData<List<PlaylistChannel>> = MutableLiveData()
    val playlistChannels: LiveData<List<PlaylistChannel>> = _playlistChannels

    private val _channelsData: MutableLiveData<List<ChannelBackendInfo>> = MutableLiveData()
    val channelsData: LiveData<List<ChannelBackendInfo>> = _channelsData

    private val _focusedChannelIndex: MutableLiveData<Int> = MutableLiveData()
    val focusedChannelIndex: LiveData<Int> = _focusedChannelIndex

    private val _currentChannel: MutableLiveData<ChannelData> = MutableLiveData()
    val currentChannel: LiveData<ChannelData> = _currentChannel

    private val _currentChannelIndex: MutableLiveData<Int> = MutableLiveData()
    val currentChannelIndex: LiveData<Int> = _currentChannelIndex

    private val _channelError: MutableLiveData<String> = MutableLiveData()
    val channelError: LiveData<String> = _channelError

    private val _isChannelsListFocused: MutableLiveData<Boolean> = MutableLiveData(true)
    val isChannelsListFocused: LiveData<Boolean> = _isChannelsListFocused

    private val _isChannelClicked: MutableLiveData<Boolean> = MutableLiveData(true)
    val isChannelClicked: LiveData<Boolean> = _isChannelClicked

    private val _streamsUrlTemplates: MutableLiveData<List<StreamUrlTemplate>> = MutableLiveData()
    val streamsUrlTemplates: LiveData<List<StreamUrlTemplate>> = _streamsUrlTemplates

    private var archiveViewModel: ArchiveViewModel? = null

    init {
        val cachedChannelIndex = sharedPreferencesUseCase.getIntValue(CURRENT_CHANNEL_INDEX_KEY)
        Log.i("PREFS", "current channel index key $cachedChannelIndex")
        if (cachedChannelIndex == -1) {
            updateChannelIndex(0, true)
            updateChannelIndex(0, false)
        } else {
            updateChannelIndex(cachedChannelIndex, true)
            updateChannelIndex(cachedChannelIndex, false)
        }
    }

    fun setArchiveViewModel(viewModel: ArchiveViewModel) {
        archiveViewModel = viewModel
    }

    fun setChannelError(error: String) {
        _channelError.value = error
    }

    fun updateCurrentChannel() {
        val channel = channelsData.value?.getOrNull(_currentChannelIndex.value ?: 0)?.channel?.get(0)
        channel?.let { channel ->
            _currentChannel.value = channel
            Log.i("update focused channel", channel.toString())
            archiveViewModel?.startDvrCollectionJob(channel.name)
        }
    }

    // update current channel index or focused channel index
    fun updateChannelIndex(index: Int, isCurrent: Boolean) {
        viewModelScope.launch {
            channelsData.asFlow().take(1).collectLatest { channels ->
                if (index in channels.indices) {
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
        }
    }

    fun getChannelByIndex(index: Int): ChannelData? {
        return channelsData.value?.getOrNull(index)?.channel?.get(0)
    }

    fun setIsChannelsListFocused(isFocused: Boolean) {
        _isChannelsListFocused.value = isFocused
    }

    fun setIsChannelClicked(isClicked: Boolean) {
        _isChannelClicked.value = isClicked
    }

    fun fetchChannelsData(token: String) {
        viewModelScope.launch {
            val data = getChannelsDataUseCase.getChannelsData(token)
            Log.i("channels data", data.toString())
            _channelsData.value = data
        }
    }

    fun fetchStreamsUrlTemplates(token: String) {
        viewModelScope.launch {
            val templates = getStreamsUrlTemplatesUseCase.getStreamsUrlTemplates(token)
            _streamsUrlTemplates.value = templates
        }
    }

    fun parsePlaylist() {
        viewModelScope.launch {
            // "http://91.222.154.251/tv/playlists/Lait?token=Lait" - ukrainian flussonic
            // "http://193.176.212.58:8080/tv/playlists/oktv?token=oktv" - georgian flussonic
            // "https://185.15.115.246/tv/playlists/test?token=IKT0neFHTB1Ki0" lasha flussonic
            val playlistContent =
                readFileUseCase.readFile("http://185.15.115.246/tv/playlists/test?token=IKT0neFHTB1Ki0")

            if (playlistContent.isNotEmpty()) {
                val channelsData = getPlaylistDataUseCase.getPlaylistData(playlistContent)

                _playlistChannels.value = channelsData
                _focusedChannelIndex.value = 0
            } else {
                setChannelError("Failure to fetch playlist")
            }
        }
    }

    fun handleChannelOnKeyEvent(
        key: Key,
        setMediaUrl: (String) -> Unit,
        setIsEpgListFocused: (Boolean) -> Unit
    ) {

        when (key) {
            Key.DirectionDown -> {
                focusedChannelIndex.value?.let { focusedIndex ->
                    Log.i("FIRED", "focused channel down")
                    updateChannelIndex(focusedIndex + 1, false)
                }
            }
            Key.DirectionUp -> {
                focusedChannelIndex.value?.let { focusedIndex ->
                    updateChannelIndex(focusedIndex - 1, false)
                }
            }
            Key.DirectionRight -> {
                _isChannelsListFocused.value = false
                setIsEpgListFocused(true)
            }

            Key.DirectionCenter -> {
                _focusedChannelIndex.value?.let { focusedIndex ->
                    Log.i("focused index in key handler", focusedIndex.toString())
                    updateChannelIndex(focusedIndex, true)
                }

                archiveViewModel?.updateIsLive(true)
                archiveViewModel?.liveTime?.value?.let { liveTime ->
                    archiveViewModel?.setCurrentTime(liveTime)
                }

                currentChannel.value?.let { channel ->
                    Log.i("channel url", channel.channelUrl)
                    setMediaUrl(channel.channelUrl)
                }
            }

            Key.Back -> {
                Log.i("show channel info", "channel back")
                _isChannelClicked.value = true
            }
        }
    }
}