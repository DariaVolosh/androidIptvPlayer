package com.example.iptvplayer.view.epg

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iptvplayer.domain.FetchChannelEpgUseCase
import com.example.iptvplayer.domain.SaveEpgDataUseCase
import com.example.iptvplayer.room.Epg
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EpgViewModel @Inject constructor(
    private val saveEpgDataUseCase: SaveEpgDataUseCase,
    private val fetchChannelEpgUseCase: FetchChannelEpgUseCase
): ViewModel() {
    private val _currentChannelEpg: MutableLiveData<List<Epg>> = MutableLiveData()
    val currentChannelEpg: LiveData<List<Epg>> = _currentChannelEpg

    fun saveEpgData(channelNames: Set<String>) {
        viewModelScope.launch {
            saveEpgDataUseCase.saveEpgData(channelNames)
        }
    }

    fun fetchChannelEpg(channelName: String) {
        viewModelScope.launch {
            val epg = fetchChannelEpgUseCase.fetchChannelEpg(channelName)
            _currentChannelEpg.value = epg
        }
    }
}