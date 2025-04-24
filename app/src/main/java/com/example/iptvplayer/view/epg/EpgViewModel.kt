package com.example.iptvplayer.view.epg

import android.util.Log
import androidx.compose.ui.input.key.Key
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iptvplayer.domain.GetEpgByIdUseCase
import com.example.iptvplayer.retrofit.data.Epg
import com.example.iptvplayer.retrofit.data.EpgDataAndCurrentIndex
import com.example.iptvplayer.view.channels.ArchiveViewModel
import com.example.iptvplayer.view.channels.ChannelsViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EpgViewModel @Inject constructor(
    private val getEpgByIdUseCase: GetEpgByIdUseCase
): ViewModel() {
    // specific epg list of the channel that the user is focused on
    private val _epgList: MutableLiveData<List<Epg>> = MutableLiveData()
    val epgList: LiveData<List<Epg>> = _epgList

    private val _epgListFlow: MutableSharedFlow<List<Epg>> = MutableSharedFlow()
    val epgListFlow: Flow<List<Epg>> = _epgListFlow

    // focused epg index, that changes when the user scrolls epg list
    private val _focusedEpgIndex: MutableLiveData<Int> = MutableLiveData(-1)
    val focusedEpgIndex: LiveData<Int> = _focusedEpgIndex

    private val _focusedEpgIndexFlow: MutableSharedFlow<Int> = MutableSharedFlow()
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

    // VERSION 2
    private val _allChannelsEpg: MutableLiveData<MutableList<EpgDataAndCurrentIndex>> = MutableLiveData(
        mutableListOf()
    )
    var allChannelsEpg: LiveData<MutableList<EpgDataAndCurrentIndex>> = _allChannelsEpg

    fun setArchiveViewModel(viewModel: ArchiveViewModel) {
        archiveViewModel = viewModel
    }

    fun setChannelsViewModel(viewModel: ChannelsViewModel) {
        channelsViewModel = viewModel
    }

    val channelsIdToEpgIdMapper = mapOf(
        "X2plus2" to "ch001",
        "Xm1-ua" to "ch002",
        "ch003" to "ch003",
        "Xtet-ua" to "ch004",
        "Xnovy-kanal-ua" to "ch005",
        "Xntn-ua" to "ch006",
        "Xsetanta-plus-ua" to "ch007",
        "Xsetanta-ua" to "ch008",
        "Xviasat-nature" to "ch009",
        "Xtlc" to "ch010",
        "Xfilmbox-ru" to "ch011",
        "Xfilmbox-arthouse" to "ch012",
        "id-xtra" to "ch013",
        "ROZPAKUY" to "ch014",
        "Super" to "ch015",
        "Xdiscovery-channel" to "ch016",
        "Xstar-family-ua " to "ch017",
        "Xstar-cinema-ua" to "ch018",
        "Xviasat-explore" to "ch019",
        "Xanimal-planet-eu" to "ch020",
        "Xsonce-ua" to "ch021",
        "Xpixel" to "ch022",
        "Xenter-film" to "ch023",
        "Xxsport-ua" to "ch024",
        "Xespreso-tv" to "ch025",
        "Xpriamyj" to "ch026",
        "Xk2-ua" to "ch027",
        "Xk1-ua" to "ch028",
        "Xsuspilne-sport-ua" to "ch029",
        "Xua-tv" to "ch030",
        "Xarmia-tv-ua" to "ch031",
        "Xbigudi" to "ch032",
        "Xunian-tv" to "ch033",
        "Xinter-ua" to "ch034",
        "Xstb-ua" to "ch035",
        "Xictv-ukraine" to "ch036",
        "Xkultura-ua" to "ch037",
        "X1plus1-ukraina" to "ch038",
        "Xua-pershy" to "ch039",
        "Xmy-ukraina-plus" to "ch040",
        "Xplusplus" to "ch041",
        "Xmega-ua" to "ch042",
        // OCE! (ua channel)
        "ch36" to "ch043",
        // TRK
        "ch25" to "ch044",
        // VITA TV
        "ch24" to "ch045",
        // TUSO
        "ch23" to "ch046",
        // TAK TV
        "ch19" to "ch047",
        // 1+1 marafon
        "ch11" to "ch048",
        "ch005" to "ch049",
        "ch172" to "ch050",
        "ch170" to "ch051",
        "ch169" to "ch052",
        "ch006" to "ch053",
        "ch179" to "ch054",
        "ch002" to "ch055",
        "ch004" to "ch056",
        "ch168" to "ch057",
        // lasha flussonic
        "tv-pirveli" to "ch050",
        "marao" to "ch052",
        "palitranews" to "ch054",
        "rustavi2tv" to "ch055",
    )


    fun updateCurrentEpg() {
        epgList.value?.let { epgList ->
            Log.i("current epgIndex", "${_currentEpgIndex.value}")
            if (currentEpgIndex.value != -1) {
                _currentEpg.value = epgList[currentEpgIndex.value ?: 0]
            } else {
                _currentEpg.value = Epg()
            }
        }
    }

    fun updateCurrentEpgIndex(current: Int) {
        epgList.value?.size?.let { epgSize ->
            _currentEpgIndex.value = current
            updateCurrentEpg()
        }
    }

    fun updateFocusedEpgIndex(index: Int) {
       viewModelScope.launch {
           epgList.value?.size?.let { epgSize ->
               if (index < epgSize && index >= 0) {
                   _focusedEpgIndex.value = index
                   _focusedEpgIndexFlow.emit(index)
               }
           }
       }
    }

    fun getEpgByIndex(index: Int): Epg? {
        return epgList.value?.getOrNull(index)
    }

    fun setIsEpgListFocused(isFocused: Boolean) {
        _isEpgListFocused.value = isFocused
    }

    fun updateCurrentEpgList(channelIndex: Int) {
        viewModelScope.launch {
            Log.i("all channels epg", _allChannelsEpg.value.toString())
            Log.i("update current epg list", channelIndex.toString())
            Log.i("channel index epg:", "${_allChannelsEpg.value?.getOrNull(channelIndex)}")
            while (true) {
                val epg = _allChannelsEpg.value?.get(channelIndex)
                Log.i("channel index epg:", "$epg")

                if (epg == EpgDataAndCurrentIndex()) {
                    delay(100)
                } else {
                    if (epg?.data?.isNotEmpty() == true) {
                        CoroutineScope(Dispatchers.Main).launch {
                            _epgListFlow.emit(emptyList())
                            _focusedEpgIndexFlow.emit(-1)
                            _epgList.value = epg.data
                            updateFocusedEpgIndex(epg.currentEpgIndex)
                            updateCurrentEpgIndex(epg.currentEpgIndex)
                            Log.i("update current epg list", "${epg.currentEpgIndex} ${epg.data}")
                            _epgListFlow.emit(epg.data)
                            _focusedEpgIndexFlow.emit(epg.currentEpgIndex)
                        }

                        break
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
                    updateFocusedEpgIndex(focusedIndex + 1)
                }
            }

            Key.DirectionUp -> {
                focusedEpgIndex.value?.let { focusedIndex ->
                    Log.i("FIRED", "focused epg up")
                    updateFocusedEpgIndex(focusedIndex - 1)
                }
            }

            Key.DirectionLeft -> {
                _isEpgListFocused.value = false
                channelsViewModel?.setIsChannelsListFocused(true)
            }

            Key.DirectionCenter -> {
                focusedEpgIndex.value?.let { focusedIndex ->
                    val focusedEpg = getEpgByIndex(focusedIndex)
                    val currentChannel = channelsViewModel?.focusedChannel?.value

                    if (focusedEpg != null && focusedEpg.startSeconds in dvrRange.first..dvrRange.second) {
                        archiveViewModel?.updateIsLive(false)
                        updateCurrentEpgIndex(focusedIndex)

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
        Log.i("channels data", channelsData.toString())
        if (_allChannelsEpg.value?.size == 0) {
            for (channel in channelsData) {
                _allChannelsEpg.value?.add(EpgDataAndCurrentIndex())
            }
        }
        for (channelData in channelsData) {
            getEpgById(channelData.first, channelData.second, token)
        }
    }

    // FOR TESTING BACKEND ENDPOINT
    fun getEpgById(epgId: Int, channelIndex: Int, token: String) {
        viewModelScope.launch {
            Log.i("GET EPG BY ID 2", "${epgId.toString()} ${channelIndex.toString()} ${token.toString()}")

            val epgDataAndCurrentIndex = getEpgByIdUseCase.getEpgById(epgId, token)
            Log.i("GET EPG BY ID 2", epgDataAndCurrentIndex.data.size.toString())
            Log.i("GET EPG BY ID 2" , epgDataAndCurrentIndex.currentEpgIndex.toString() + " CURRENT INDEX")
            Log.i("GET EPG BY ID 2", epgId.toString())
            Log.i("GET EPG BY ID 2", epgDataAndCurrentIndex.toString())
            Log.i("GET EPG BY ID 2", "CHANNEL INDEX $channelIndex")
            _allChannelsEpg.value?.set(channelIndex, epgDataAndCurrentIndex)
        }
    }
}