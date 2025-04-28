package com.example.iptvplayer.view.epg

import android.util.Log
import androidx.compose.ui.input.key.Key
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.example.iptvplayer.domain.GetEpgByIdUseCase
import com.example.iptvplayer.domain.SharedPreferencesUseCase
import com.example.iptvplayer.retrofit.data.Epg
import com.example.iptvplayer.retrofit.data.EpgDataAndCurrentIndex
import com.example.iptvplayer.view.channels.ArchiveViewModel
import com.example.iptvplayer.view.channels.ChannelsViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import javax.inject.Inject

const val CURRENT_EPG_INDEX_KEY = "current_epg_index"
const val CURRENT_EPG_KEY = "current_epg"

@HiltViewModel
class EpgViewModel @Inject constructor(
    private val getEpgByIdUseCase: GetEpgByIdUseCase,
    private val sharedPreferencesUseCase: SharedPreferencesUseCase
): ViewModel() {
    // specific epg list of the channel that the user is focused on
    private val _epgList: MutableLiveData<List<Epg>> = MutableLiveData()
    val epgList: LiveData<List<Epg>> = _epgList

    private val _epgListFlow: MutableSharedFlow<List<Epg>> = MutableSharedFlow()
    val epgListFlow: Flow<List<Epg>> = _epgListFlow

    // focused epg index, that changes when the user scrolls epg list
    private val _focusedEpgIndex: MutableLiveData<Int> = MutableLiveData(-1)
    val focusedEpgIndex: LiveData<Int> = _focusedEpgIndex

    private val _focusedEpgIndexFlow: MutableSharedFlow<Int> = MutableSharedFlow(replay = 1)
    val focusedEpgIndexFlow: Flow<Int> = _focusedEpgIndexFlow

    // currently chosen epg, the program, that should be played
    private val _currentEpg: MutableLiveData<Epg> = MutableLiveData()
    val currentEpg: LiveData<Epg> = _currentEpg

    private val _currentEpgIndex: MutableLiveData<Int> = MutableLiveData(-1)
    val currentEpgIndex: LiveData<Int> = _currentEpgIndex

    // defining live programme as a live data because in main composable inside launched effect
    // the new value will not be captured if it is not specified as a key in launched effect
    private var _liveProgrammeIndex: MutableLiveData<Int> = MutableLiveData(-1)
    val liveProgrammeIndex: LiveData<Int> = _liveProgrammeIndex

    private var _isEpgListFocused: MutableLiveData<Boolean> = MutableLiveData(false)
    val isEpgListFocused: LiveData<Boolean> = _isEpgListFocused

    private var archiveViewModel: ArchiveViewModel? = null
    private var channelsViewModel: ChannelsViewModel? = null
    private var isCachedEpgDisplayed = false

    private val _allChannelsEpg: MutableLiveData<MutableList<EpgDataAndCurrentIndex?>> = MutableLiveData(
        mutableListOf()
    )

    var allChannelsEpg: LiveData<MutableList<EpgDataAndCurrentIndex?>> = _allChannelsEpg

    init {
        val cachedEpgIndex = sharedPreferencesUseCase.getIntValue(CURRENT_EPG_INDEX_KEY)
        Log.i("PREFS", "cached epg index: $cachedEpgIndex")

        if (cachedEpgIndex == -1) {
            updateEpgIndex(0, true)
            updateEpgIndex(0, false)
        } else {
            updateEpgIndex(cachedEpgIndex, true)
            updateEpgIndex(cachedEpgIndex, false)
            isCachedEpgDisplayed = true
        }
    }

    fun setArchiveViewModel(viewModel: ArchiveViewModel) {
        archiveViewModel = viewModel
    }

    fun setChannelsViewModel(viewModel: ChannelsViewModel) {
        channelsViewModel = viewModel
    }

    fun updateCurrentEpg() {
        val epg = _epgList.value?.getOrNull(currentEpgIndex.value ?: 0)
        Log.i("current epgIndex", "${_currentEpgIndex.value}")
        epg?.let { e ->
            _currentEpg.value = e
        }
    }

    fun updateEpgIndex(index: Int, isCurrent: Boolean) {
        viewModelScope.launch {
            epgList.asFlow().take(1).collectLatest { epgList ->
                Log.i("update epg index list size", epgList.size.toString())
                if (index in epgList.indices) {
                    if (isCurrent) {
                        Log.i("set current epg index", "$index")
                        _currentEpgIndex.value = index
                        updateCurrentEpg()
                        sharedPreferencesUseCase.saveIntValue(CURRENT_EPG_INDEX_KEY, index)
                    } else {
                        _focusedEpgIndex.value = index
                        _focusedEpgIndexFlow.emit(index)
                    }
                }
            }
        }
    }

    fun resetCurrentEpg() {
        _currentEpg.value = Epg()
    }

    fun resetEpgIndex(isCurrent: Boolean) {
        if (isCurrent) {
            _currentEpgIndex.value = -1
            resetCurrentEpg()
            sharedPreferencesUseCase.saveIntValue(CURRENT_EPG_INDEX_KEY, -1)
        }
        else _focusedEpgIndex.value = -1
    }

    fun getEpgByIndex(index: Int): Epg? {
        return epgList.value?.getOrNull(index)
    }

    fun setIsEpgListFocused(isFocused: Boolean) {
        _isEpgListFocused.value = isFocused
    }

    fun updateCurrentEpgList(currentTime: Long, channelIndex: Int) {
        viewModelScope.launch {
            Log.i("all channels epg", _allChannelsEpg.value.toString())
            Log.i("update current epg list", channelIndex.toString())
            Log.i("channel index epg:", "${_allChannelsEpg.value?.getOrNull(channelIndex)}")

            val currentEpgListData = _allChannelsEpg.value?.get(channelIndex)

            if (currentEpgListData != null) {
                val currentEpgList = currentEpgListData.data
                val currentEpgIndex = currentEpgListData.currentEpgIndex
                val liveEpgIndex = currentEpgListData.liveEpgIndex

                if (isCachedEpgDisplayed) {
                    _epgList.value = currentEpgList
                    _epgListFlow.emit(currentEpgList)

                    if (liveEpgIndex != -1) {
                        _liveProgrammeIndex.value = liveEpgIndex
                    }

                    isCachedEpgDisplayed = false

                } else {
                    _epgListFlow.emit(emptyList())
                    _epgList.value = currentEpgList
                    _epgListFlow.emit(currentEpgList)

                    if (currentEpgIndex == -1) {
                        _currentEpgIndex.value = - 1
                        _currentEpg.value = Epg()

                        if (currentEpgList[0].startSeconds > currentTime) {
                            updateEpgIndex(0, false)
                        } else {
                            updateEpgIndex(currentEpgList.size-1, false)
                        }
                    } else {
                        updateEpgIndex(currentEpgListData.currentEpgIndex, true)
                        updateEpgIndex(currentEpgListData.currentEpgIndex, false)
                    }

                    if (liveEpgIndex != -1) {
                        _liveProgrammeIndex.value = liveEpgIndex
                    }
                }
            }
        }
    }

    fun handleEpgOnKeyEvent(
        key: Key,
        dvrRange: Pair<Long, Long>
    ) {
        when (key) {
            Key.DirectionDown -> {
                focusedEpgIndex.value?.let { focusedIndex ->
                    Log.i("FIRED", "focused epg down")
                    updateEpgIndex(focusedIndex + 1, false)
                }
            }

            Key.DirectionUp -> {
                focusedEpgIndex.value?.let { focusedIndex ->
                    Log.i("FIRED", "focused epg up")
                    updateEpgIndex(focusedIndex - 1, false)
                }
            }

            Key.DirectionLeft -> {
                _isEpgListFocused.value = false
                channelsViewModel?.setIsChannelsListFocused(true)
            }

            Key.DirectionCenter -> {
                focusedEpgIndex.value?.let { focusedIndex ->
                    val focusedEpg = getEpgByIndex(focusedIndex)
                    val focusedChannelIndex = channelsViewModel?.focusedChannelIndex?.value
                    if (focusedChannelIndex != null) {
                        channelsViewModel?.updateChannelIndex(focusedChannelIndex, true)
                    }

                    if (focusedEpg != null && focusedEpg.startSeconds in dvrRange.first..dvrRange.second) {
                        archiveViewModel?.updateIsLive(false)
                        updateEpgIndex(focusedIndex, false)
                        updateEpgIndex(focusedIndex, true)

                        val currentChannel = channelsViewModel?.currentChannel?.value

                        currentChannel?.let { channel ->
                            archiveViewModel?.setCurrentTime(focusedEpg.startSeconds)
                            archiveViewModel?.getArchiveUrl(channel.channelUrl)
                        }

                        channelsViewModel?.setIsChannelClicked(true)
                    }
                }
            }

            Key.Back -> {
                channelsViewModel?.setIsChannelClicked(true)
            }
        }
    }

    // we include pairs of epg id and channel index in the list that indicates our channels
    // of interest (determined by amount of currently visible channels plus focus location)
    fun fetchEpg(channelsData: List<Pair<Int, Int>>, token: String) {
        val currentTime = archiveViewModel?.currentTime?.value
        val liveTime = archiveViewModel?.liveTime?.value

        Log.i("channels data", channelsData.toString())


        if (currentTime != null && liveTime != null) {
            for (channelData in channelsData) {
                getEpgById(
                    currentTime,
                    liveTime,
                    channelData.first,
                    channelData.second,
                    token
                )
            }
        }
    }

    // FOR TESTING BACKEND ENDPOINT
    fun getEpgById(
        currentTime: Long,
        liveTime: Long,
        epgId: Int,
        channelIndex: Int,
        token: String
    ) {
        viewModelScope.launch {
            Log.i("GET EPG BY ID 2", "${epgId.toString()} ${channelIndex.toString()} ${token.toString()}")

            val epgDataAndCurrentIndex = getEpgByIdUseCase.getEpgById(
                currentTime,
                liveTime,
                epgId,
                token
            )
            Log.i("GET EPG BY ID 2", epgDataAndCurrentIndex.data.size.toString())
            Log.i("GET EPG BY ID 2" , epgDataAndCurrentIndex.currentEpgIndex.toString() + " CURRENT INDEX")
            Log.i("GET EPG BY ID 2", epgId.toString())
            Log.i("GET EPG BY ID 2", epgDataAndCurrentIndex.toString())
            Log.i("GET EPG BY ID 2", "CHANNEL INDEX $channelIndex")
            _allChannelsEpg.value?.set(channelIndex, epgDataAndCurrentIndex)
            if (channelIndex == channelsViewModel?.focusedChannelIndex?.value) {
                updateCurrentEpgList(currentTime, channelIndex)
            }
        }
    }

    fun createAllChannelsEpgList(channelsAmount: Int) {
        for (channelIndex in 0..<channelsAmount) {
            _allChannelsEpg.value?.add(null)
        }
    }
}