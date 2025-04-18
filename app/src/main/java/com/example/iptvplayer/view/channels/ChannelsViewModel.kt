package com.example.iptvplayer.view.channels

import android.util.Log
import androidx.compose.ui.input.key.Key
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iptvplayer.data.PlaylistChannel
import com.example.iptvplayer.domain.GetChannelsDataUseCase
import com.example.iptvplayer.domain.GetPlaylistDataUseCase
import com.example.iptvplayer.domain.ReadFileUseCase
import com.example.iptvplayer.retrofit.ChannelBackendInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChannelsViewModel @Inject constructor(
    private val readFileUseCase: ReadFileUseCase,
    private val getPlaylistDataUseCase: GetPlaylistDataUseCase,
    private val getChannelsDataUseCase: GetChannelsDataUseCase
): ViewModel() {
    private val _playlistChannels: MutableLiveData<List<PlaylistChannel>> = MutableLiveData()
    val playlistChannels: LiveData<List<PlaylistChannel>> = _playlistChannels

    private val _channelsData: MutableLiveData<List<ChannelBackendInfo>> = MutableLiveData()
    val channelsData: LiveData<List<ChannelBackendInfo>> = _channelsData

    private val _focusedChannelIndex: MutableLiveData<Int> = MutableLiveData()
    val focusedChannelIndex: LiveData<Int> = _focusedChannelIndex

    private val _focusedChannel: MutableLiveData<PlaylistChannel> = MutableLiveData()
    val focusedChannel: LiveData<PlaylistChannel> = _focusedChannel

    private val _channelError: MutableLiveData<String> = MutableLiveData()
    val channelError: LiveData<String> = _channelError

    private val _isChannelsListFocused: MutableLiveData<Boolean> = MutableLiveData(true)
    val isChannelsListFocused: LiveData<Boolean> = _isChannelsListFocused

    private val _isChannelClicked: MutableLiveData<Boolean> = MutableLiveData()
    val isChannelClicked: LiveData<Boolean> = _isChannelClicked

    private var archiveViewModel: ArchiveViewModel? = null

    fun setArchiveViewModel(viewModel: ArchiveViewModel) {
        archiveViewModel = viewModel
    }

    fun setChannelError(error: String) {
        _channelError.value = error
    }

    private fun updateFocusedChannel() {
        _focusedChannel.value = playlistChannels.value?.getOrNull(focusedChannelIndex.value ?: 0)
    }

    fun updateFocusedChannelIndex(focused: Int) {
        _playlistChannels.value?.size?.let { channelsSize ->
            if (focused in 0..<channelsSize) {
                _focusedChannelIndex.value = focused
                Log.i("focused channel", focused.toString())
                updateFocusedChannel()
            }
        }
    }

    fun getChannelByIndex(index: Int): PlaylistChannel? {
        return _playlistChannels.value?.getOrNull(index)
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
        setIsEpgListFocused: (Boolean) -> Unit,
        getEpgById: (String, Pair<Long, Long>) -> Unit
    ) {
        when (key) {
            Key.DirectionDown -> {
                focusedChannelIndex.value?.let { focusedIndex ->
                    Log.i("FIRED", "focused channel down")
                    updateFocusedChannelIndex(focusedIndex + 1)
                }
            }
            Key.DirectionUp -> {
                focusedChannelIndex.value?.let { focusedIndex ->
                    updateFocusedChannelIndex(focusedIndex - 1)
                }
            }
            Key.DirectionRight -> {
                _isChannelsListFocused.value = false
                setIsEpgListFocused(true)
            }

            Key.DirectionCenter -> {
                archiveViewModel?.updateIsLive(true)
                archiveViewModel?.liveTime?.value?.let { liveTime ->
                    archiveViewModel?.setCurrentTime(liveTime)
                }

                focusedChannel.value?.let { channel ->
                    setMediaUrl(channel.url)

                    archiveViewModel?.startDvrCollectionJob(channel.id) { dvrRange ->
                        getEpgById(channel.id, dvrRange)
                    }
                }
            }

            Key.Back -> {
                Log.i("show channel info", "channel back")
                _isChannelClicked.value = true
            }
        }
    }
}