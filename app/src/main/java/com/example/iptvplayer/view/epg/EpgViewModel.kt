package com.example.iptvplayer.view.epg

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iptvplayer.domain.epg.AccessEpgCacheUseCase
import com.example.iptvplayer.domain.epg.GetEpgByIdUseCase
import com.example.iptvplayer.domain.sharedPrefs.SharedPreferencesUseCase
import com.example.iptvplayer.domain.time.CalendarManager
import com.example.iptvplayer.domain.time.DateManager
import com.example.iptvplayer.retrofit.data.EpgListItem
import com.example.iptvplayer.retrofit.data.EpgTimeRangeInSeconds
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.TimeZone
import javax.inject.Inject

const val CURRENT_EPG_INDEX_KEY = "current_epg_index"

@HiltViewModel
class EpgViewModel @Inject constructor(
    private val getEpgByIdUseCase: GetEpgByIdUseCase,
    private val sharedPreferencesUseCase: SharedPreferencesUseCase,
    private val accessEpgCacheUseCase: AccessEpgCacheUseCase,
    private val dateManager: DateManager,
    private val calendarManager: CalendarManager
): ViewModel() {

    // FOCUSED CHANNEL EPG INFO (used for displaying focused channel epg list)
    private val _dayToEpgList: MutableStateFlow<MutableMap<EpgListItem.Header, List<EpgListItem.Epg>>> =
        MutableStateFlow(mutableMapOf())
    val dayToEpgList: StateFlow<Map<EpgListItem.Header, List<EpgListItem.Epg>>> = _dayToEpgList

    private val _focusedChannelEpgItems: MutableStateFlow<List<EpgListItem>> =
        MutableStateFlow(listOf())
    val focusedChannelEpgItems: StateFlow<List<EpgListItem>> = _focusedChannelEpgItems

    private val _focusedEpgIndex: MutableStateFlow<Int> = MutableStateFlow(-1)
    val focusedEpgIndex: StateFlow<Int> = _focusedEpgIndex

    private var _focusedEpgLiveProgram: MutableStateFlow<Int> = MutableStateFlow(-1)
    val focusedEpgLiveProgram: StateFlow<Int> = _focusedEpgLiveProgram

    // CURRENT CHANNEL EPG INFO (for the channel that is currently playing)
    private val _currentEpg: MutableStateFlow<EpgListItem.Epg> = MutableStateFlow(EpgListItem.Epg())
    val currentEpg: StateFlow<EpgListItem.Epg> = _currentEpg

    private val _currentEpgIndex: MutableStateFlow<Int> = MutableStateFlow(-1)
    val currentEpgIndex: StateFlow<Int> = _currentEpgIndex

    private val _currentChannelEpgItems: MutableStateFlow<List<EpgListItem.Epg>> =
        MutableStateFlow(listOf())
    val currentChannelEpgItems: StateFlow<List<EpgListItem.Epg>> = _currentChannelEpgItems

    private var _currentEpgLiveProgram: MutableStateFlow<Int> = MutableStateFlow(-1)
    val currentEpgLiveProgram: StateFlow<Int> = _currentEpgLiveProgram

    // defining live programme as a live data because in main composable inside launched effect
    // the new value will not be captured if it is not specified as a key in launched effect


    private var _isEpgListFocused: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isEpgListFocused: StateFlow<Boolean> = _isEpgListFocused

    val datePattern = "yyyyMMddHHmmss"
    val timezone = TimeZone.getTimeZone("GMT+04:00")

    private var isCachedEpgDisplayed = false

    private val _allChannelsEpg: MutableStateFlow<MutableList<List<EpgListItem.Epg>?>> =
        MutableStateFlow(mutableListOf())

    var allChannelsEpg: StateFlow<MutableList<List<EpgListItem.Epg>?>> = _allChannelsEpg

    var epgFetchJob: Job? = null

    var currentEpgListUpdateJob: Job? = null
    var focusedEpgListUpdateJob: Job? = null

    init {
        val cachedEpgIndex = sharedPreferencesUseCase.getIntValue(CURRENT_EPG_INDEX_KEY)
        Log.i("PREFS", "cached epg index: $cachedEpgIndex")

        if (cachedEpgIndex != -1) {
            updateEpgIndex(cachedEpgIndex, true)
            updateEpgIndex(cachedEpgIndex, false)
            isCachedEpgDisplayed = true
        }
    }

    fun updateCurrentEpg() {
        val epg = _focusedChannelEpgItems.value.getOrNull(currentEpgIndex.value)
        Log.i("current epgIndex", "${_currentEpgIndex.value}")
        epg?.let { e ->
            _currentEpg.value = e as EpgListItem.Epg
        }
    }

    fun updateEpgIndex(index: Int, isCurrent: Boolean) {
        val currentEpgItems = if (isCurrent) _currentChannelEpgItems else _focusedChannelEpgItems
        Log.i("update epg index list size", currentEpgItems.value.size.toString())
        if (index in currentEpgItems.value.indices) {
            if (isCurrent) {
                Log.i("set current epg index", "$index")
                _currentEpgIndex.value = index
                updateCurrentEpg()
                sharedPreferencesUseCase.saveIntValue(CURRENT_EPG_INDEX_KEY, index)
            } else {
                Log.i("set focused epg index", "$index")
                _focusedEpgIndex.value = index
            }
        }
    }

    fun updateLiveEpgIndex(index: Int, isCurrent: Boolean) {
        if (isCurrent) {
            _currentEpgLiveProgram.value = index
        } else {
            _focusedEpgLiveProgram.value = index
        }
    }

    fun resetCurrentEpg() {
        _currentEpg.value = EpgListItem.Epg()
    }

    fun resetEpgList(isCurrent: Boolean) {
        if (isCurrent) {
            _currentChannelEpgItems.value = emptyList()
        } else {
            _focusedChannelEpgItems.value = emptyList()
            _dayToEpgList.value = mutableMapOf()
        }
    }

    fun resetLiveEpgIndex(isCurrent: Boolean) {
        if (isCurrent) {
            _currentEpgLiveProgram.value = -1
        } else {
            _focusedEpgLiveProgram.value = -1
        }
    }

    fun resetEpgIndex(isCurrent: Boolean) {
        if (isCurrent) {
            _currentEpgIndex.value = -1
            resetCurrentEpg()
            sharedPreferencesUseCase.saveIntValue(CURRENT_EPG_INDEX_KEY, -1)
        } else {
            _focusedEpgIndex.value = -1
        }
    }


    fun findFirstEpgIndexBackward(startIndex: Int): Int {
        if (startIndex < 0 || startIndex >= _focusedChannelEpgItems.value.size) return -1

        for (i in startIndex downTo 0) {
            if (_focusedChannelEpgItems.value[i] is EpgListItem.Epg) return i
        }

        return -1
    }

    fun findFirstEpgIndexForward(startIndex: Int): Int {
        if (startIndex < 0 || startIndex >= _focusedChannelEpgItems.value.size) return -1

        for (i in startIndex..<_focusedChannelEpgItems.value.size) {
            if (_focusedChannelEpgItems.value[i] is EpgListItem.Epg) return i
        }

        return -1
    }

    fun getEpgItemByIndex(index: Int): EpgListItem? {
        return _focusedChannelEpgItems.value.getOrNull(index)
    }

    fun setIsEpgListFocused(isFocused: Boolean) {
        _isEpgListFocused.value = isFocused
    }

    fun isTimeWithinProgramRange(
        programRange: EpgTimeRangeInSeconds,
        timeComparedAgainst: Long
    ): Boolean {
        return timeComparedAgainst in programRange.start..programRange.stop
    }

    fun getEpgTimeRangeInSeconds(
        start: String,
        stop: String
    ): EpgTimeRangeInSeconds {
        val startTimeSeconds = dateManager.parseDate(start, datePattern, timezone)
        val stopTimeSeconds = dateManager.parseDate(stop, datePattern, timezone)

        return EpgTimeRangeInSeconds(startTimeSeconds, stopTimeSeconds)
    }

    fun updateCurrentEpgList(
        currentEpgList: List<EpgListItem.Epg>,
        liveTime: Long,
        currentTime: Long
    ) {
        currentEpgListUpdateJob = viewModelScope.launch(Dispatchers.IO) {
            resetLiveEpgIndex(true)
            resetEpgIndex(true)
            resetEpgList(true)
            resetCurrentEpg()

            val datePattern = "EEEE d MMMM HH:mm:ss"

            Log.i("live time in update current epg", dateManager.formatDate(liveTime, datePattern))
            Log.i("current time in update current epg", dateManager.formatDate(currentTime, datePattern))

            val newEpgItemsList = mutableListOf<EpgListItem>()
            val newEpgList = mutableListOf<EpgListItem.Epg>()
            var currentEpgIndex = -1
            var liveEpgIndex = -1

            Log.i("items size", "initial size " + currentEpgList.size.toString())

            for (i in currentEpgList.indices) {
                val epg = currentEpgList[i]
                val epgTimeRangeInSeconds = getEpgTimeRangeInSeconds(epg.start, epg.stop)
                epg.epgVideoTimeRangeSeconds = epgTimeRangeInSeconds
                epg.epgVideoName = epg.epgVideoName.trim()

                Log.i("current epg day","${epg.epgVideoTimeRangeSeconds.start}")

                val isProgramCurrent = isTimeWithinProgramRange(epgTimeRangeInSeconds, currentTime)
                val isProgramLive = isTimeWithinProgramRange(epgTimeRangeInSeconds, liveTime)

                if (isProgramCurrent) {
                    Log.i("is program current current epg list", "$i $isProgramCurrent $epg ${dateManager.formatDate(epgTimeRangeInSeconds.start, datePattern)}")
                    currentEpgIndex = i
                }

                if (isProgramLive) {
                    Log.i("is program live", "$i $isProgramLive $epg ${dateManager.formatDate(epgTimeRangeInSeconds.start, datePattern)}")
                    liveEpgIndex = i
                }

                newEpgList.add(epg)
            }

            Log.i("items size", newEpgItemsList.size.toString())
            Log.i("focused epg", _focusedEpgIndex.value.toString())

            _currentChannelEpgItems.value = newEpgList
            updateEpgIndex(currentEpgIndex, true)
            updateLiveEpgIndex(liveEpgIndex, true)

            Log.i("epg view model", "update epg list, focused: ${_currentEpgIndex.value}, live: ${_currentEpgLiveProgram.value}")
        }
    }

    fun updateFocusedEpgList(
        focusedEpgList: List<EpgListItem.Epg>,
        liveTime: Long,
        currentTime: Long
    ) {
        focusedEpgListUpdateJob = viewModelScope.launch(Dispatchers.IO) {
            resetLiveEpgIndex(false)
            resetEpgIndex(false)
            resetEpgList(false)

            val datePattern = "EEEE d MMMM HH:mm:ss"

            Log.i("live time in update current epg", dateManager.formatDate(liveTime, datePattern))
            Log.i("current time in update current epg", dateManager.formatDate(currentTime, datePattern))

            val dayAndMonthPattern = "dd MMMM"
            var prevDay = -1
            var headersSeen = 0
            val newEpgItemsList = mutableListOf<EpgListItem>()
            var newEpgList = mutableListOf<EpgListItem.Epg>()
            val newDayToEpgList = mutableMapOf<EpgListItem.Header, List<EpgListItem.Epg>>()

            Log.i("items size", "initial size " + focusedEpgList.size.toString())

            for (i in focusedEpgList.indices) {
                val epg = focusedEpgList[i]
                val epgTimeRangeInSeconds = getEpgTimeRangeInSeconds(epg.start, epg.stop)
                epg.epgVideoTimeRangeSeconds = epgTimeRangeInSeconds
                epg.epgVideoName = epg.epgVideoName.trim()

                val currentDay = calendarManager.getCalendarDay(calendarManager.getCalendar(epg.epgVideoTimeRangeSeconds.start))
                Log.i("current epg day","${epg.epgVideoTimeRangeSeconds.start}")

                if (i == 0 || currentDay > prevDay) {
                    headersSeen++
                    newEpgList = mutableListOf()
                    val formattedDate = dateManager.formatDate(epg.epgVideoTimeRangeSeconds.start, dayAndMonthPattern)
                    prevDay = currentDay
                    val header = EpgListItem.Header(formattedDate)
                    newDayToEpgList[header] = newEpgList
                    newEpgItemsList.add(header)
                }

                val isProgramCurrent = isTimeWithinProgramRange(epgTimeRangeInSeconds, currentTime)
                val isProgramLive = isTimeWithinProgramRange(epgTimeRangeInSeconds, liveTime)

                if (isProgramCurrent) {
                    Log.i("is program current focused epg list", "$i $isProgramCurrent $epg ${dateManager.formatDate(epgTimeRangeInSeconds.start, datePattern)}")
                }

                if (isProgramLive) {
                    Log.i("is program live", "$i $isProgramLive $epg ${dateManager.formatDate(epgTimeRangeInSeconds.start, datePattern)}")
                }

                if (isProgramCurrent)  _focusedEpgIndex.value = i + headersSeen
                if (isProgramLive) _focusedEpgLiveProgram.value = i + headersSeen

                newEpgList.add(epg)
                newEpgItemsList.add(epg)
            }

            Log.i("items size", newEpgItemsList.size.toString())
            Log.i("focused epg", _focusedEpgIndex.value.toString())

            _dayToEpgList.value = newDayToEpgList
            _focusedChannelEpgItems.value = newEpgItemsList

            if (isCachedEpgDisplayed) {
                isCachedEpgDisplayed = false
            } else {
                if (_focusedEpgIndex.value == -1) {
                    Log.i("is first", "${dateManager.formatDate(focusedEpgList[0].epgVideoTimeRangeSeconds.start, datePattern)}")
                    Log.i("is first", "${dateManager.formatDate(currentTime, datePattern)}")
                    if (focusedEpgList[0].epgVideoTimeRangeSeconds.start > currentTime) {
                        updateEpgIndex(0, false)
                    } else {
                        updateEpgIndex(newEpgItemsList.size - 1, false)
                    }
                }
            }

            Log.i("epg view model", "update epg list, focused: ${_focusedEpgIndex.value}, live: ${_focusedEpgLiveProgram.value}")
            Log.i("day to epg", newDayToEpgList.keys.toString())
        }
    }

    // we include pairs of epg id and channel index in the list that indicates our channels
    // of interest (determined by amount of currently visible channels plus focus location)
    fun fetchEpg(
        requestedEpgData: List<EpgToBeFetched>,
        token: String,
    ) {
        Log.i("fetch epg debug", "entered fetch epg")
        epgFetchJob = viewModelScope.launch(Dispatchers.IO) {
            for (requestedEpg in requestedEpgData) {
                val isEpgCached = isEpgCached(requestedEpg.epgId)
                Log.i("is epg cached", "${requestedEpg.epgId} ${isEpgCached}")
                if (!isEpgCached) {
                    getEpgById(
                        requestedEpg,
                        token
                    )
                }
            }
        }
    }

    // FOR TESTING BACKEND ENDPOINT
    suspend fun getEpgById(
        requestedEpg: EpgToBeFetched,
        token: String
    ) {
        Log.i("fetch epg debug", "entered fetch epg by id")
        val epgData = getEpgByIdUseCase.getEpgById(
            requestedEpg.epgId
        )

        saveEpgToCache(requestedEpg.epgId, epgData)
    }

    suspend fun isEpgCached(epgId: Int) = accessEpgCacheUseCase.isEpgCached(epgId)

    suspend fun saveEpgToCache(epgId: Int, epgList: List<EpgListItem.Epg>) {
        accessEpgCacheUseCase.saveEpgToCache(epgId, epgList)
    }

    suspend fun getCachedEpg(epgId: Int) = accessEpgCacheUseCase.getCachedEpg(epgId)

    fun searchEpgByTime(time: Long) {
        val foundEpgIndex = _focusedChannelEpgItems.value.indexOfFirst { epgItem ->
            if (epgItem is EpgListItem.Epg) {
                time in epgItem.epgVideoTimeRangeSeconds.start..epgItem.epgVideoTimeRangeSeconds.stop
            } else {
                false
            }
        }

        if (foundEpgIndex == -1) {
            resetEpgIndex(true)

            if ((_focusedChannelEpgItems.value[1] as EpgListItem.Epg).epgVideoTimeRangeSeconds.start > time) {
                updateEpgIndex(0, false)
            } else {
                updateEpgIndex(_focusedChannelEpgItems.value.size-1, false)
            }
        } else {
            updateEpgIndex(foundEpgIndex, true)
            updateEpgIndex(foundEpgIndex, false)
        }
    }
}