package com.example.iptvplayer.view.epg

import android.util.Log
import com.example.iptvplayer.domain.sharedPrefs.SharedPreferencesUseCase
import com.example.iptvplayer.retrofit.data.EpgListItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class EpgManager @Inject constructor(
    private val sharedPreferencesUseCase: SharedPreferencesUseCase
) {
    private val _currentChannelEpgItems: MutableStateFlow<List<EpgListItem.Epg>> =
        MutableStateFlow(listOf())
    val currentChannelEpgItems: StateFlow<List<EpgListItem.Epg>> = _currentChannelEpgItems

    private val _focusedChannelEpgItems: MutableStateFlow<List<EpgListItem>> =
        MutableStateFlow(listOf())
    val focusedChannelEpgItems: StateFlow<List<EpgListItem>> = _focusedChannelEpgItems

    private val _currentEpgIndex: MutableStateFlow<Int> = MutableStateFlow(-1)
    val currentEpgIndex: StateFlow<Int> = _currentEpgIndex

    private val _focusedEpgIndex: MutableStateFlow<Int> = MutableStateFlow(-1)
    val focusedEpgIndex: StateFlow<Int> = _focusedEpgIndex

    // CURRENT CHANNEL EPG INFO (for the channel that is currently playing)
    private val _currentEpg: MutableStateFlow<EpgListItem.Epg> = MutableStateFlow(EpgListItem.Epg())
    val currentEpg: StateFlow<EpgListItem.Epg> = _currentEpg


    fun findFirstEpgIndexForward(startIndex: Int): Int {
        if (startIndex < 0 || startIndex >= _currentChannelEpgItems.value.size) return -1

        for (i in startIndex..<_currentChannelEpgItems.value.size) {
            if (_focusedChannelEpgItems.value[i] is EpgListItem.Epg) return i
        }

        return -1
    }

    fun getNextEpgItem(): EpgListItem.Epg? {
        return _currentChannelEpgItems.value.getOrNull(_currentEpgIndex.value + 1)
    }

    fun getEpgItemByIndex(index: Int): EpgListItem? {
        return _focusedChannelEpgItems.value.getOrNull(index)
    }

    fun updateCurrentEpg() {
        val epg = _focusedChannelEpgItems.value.getOrNull(currentEpgIndex.value)
        Log.i("current epgIndex", "${_currentEpgIndex.value}")
        epg?.let { e ->
            _currentEpg.value = e as EpgListItem.Epg
        }
    }

    fun updateCurrentEpgIndex(index: Int) {
        Log.i("update epg index list size", currentChannelEpgItems.value.size.toString())
        if (index in currentChannelEpgItems.value.indices) {
            updateCurrentEpg()
            sharedPreferencesUseCase.saveIntValue(CURRENT_EPG_INDEX_KEY, index)
        }
    }

    fun updateFocusedEpgIndex(index: Int) {
        if (index in focusedChannelEpgItems.value.indices) {
            Log.i("set focused epg index", "$index")
            _focusedEpgIndex.value = index
        }
    }
}