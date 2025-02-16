package com.example.iptvplayer.view.channels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iptvplayer.data.PlaylistChannel
import com.example.iptvplayer.domain.GetChannelsDataUseCase
import com.example.iptvplayer.domain.ReadFileUseCase
import kotlinx.coroutines.launch
import javax.inject.Inject

class ChannelsViewModel @Inject constructor(
    private val readFileUseCase: ReadFileUseCase,
    private val getChannelsDataUseCase: GetChannelsDataUseCase
): ViewModel() {
    private val _channels: MutableLiveData<List<PlaylistChannel>> = MutableLiveData()
    val channels: LiveData<List<PlaylistChannel>> = _channels

    fun parsePlaylist() {
        viewModelScope.launch {
            val playlistContent =
                readFileUseCase.readFile("http://193.176.212.58:8080/tv/playlists/oktv2?token=oktv2")
            _channels.value = getChannelsDataUseCase.getChannelsData(playlistContent)
        }
    }
}