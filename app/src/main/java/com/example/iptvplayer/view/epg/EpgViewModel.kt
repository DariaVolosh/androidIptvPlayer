package com.example.iptvplayer.view.epg

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iptvplayer.data.Utils
import com.example.iptvplayer.domain.AccessEpgCacheUseCase
import com.example.iptvplayer.domain.GetEpgByIdUseCase
import com.example.iptvplayer.domain.SharedPreferencesUseCase
import com.example.iptvplayer.retrofit.data.Epg
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
    private val accessEpgCacheUseCase: AccessEpgCacheUseCase
): ViewModel() {
    // specific epg list of the channel that the user is focused on
    private val _epgList: MutableStateFlow<List<Epg>> = MutableStateFlow(emptyList())
    val epgList: StateFlow<List<Epg>> = _epgList

    // mapping from specific epg index (when the new day begins) to the day-month string
    // to track where to insert day-month row in a lazy column
    private val _dateMap: MutableStateFlow<MutableMap<Int, String>> = MutableStateFlow(mutableMapOf())
    val dateMap: StateFlow<Map<Int, String>> = _dateMap

    // focused epg index, that changes when the user scrolls epg list
    private val _focusedEpgIndex: MutableStateFlow<Int> = MutableStateFlow(-1)
    val focusedEpgIndex: StateFlow<Int> = _focusedEpgIndex

    // currently chosen epg, the program, that should be played
    private val _currentEpg: MutableStateFlow<Epg> = MutableStateFlow(Epg())
    val currentEpg: StateFlow<Epg> = _currentEpg

    private val _currentEpgIndex: MutableStateFlow<Int> = MutableStateFlow(-1)
    val currentEpgIndex: StateFlow<Int> = _currentEpgIndex

    // defining live programme as a live data because in main composable inside launched effect
    // the new value will not be captured if it is not specified as a key in launched effect
    private var _liveProgrammeIndex: MutableStateFlow<Int> = MutableStateFlow(-1)
    val liveProgrammeIndex: StateFlow<Int> = _liveProgrammeIndex

    private var _isEpgListFocused: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isEpgListFocused: StateFlow<Boolean> = _isEpgListFocused

    private var _isCurrentEpgLoaded: MutableLiveData<Boolean> = MutableLiveData(false)
    var isCurrentEpgLoaded: LiveData<Boolean> = _isCurrentEpgLoaded

    val datePattern = "yyyyMMddHHmmss"
    val timezone = TimeZone.getTimeZone("GMT+04:00")

    private var isCachedEpgDisplayed = false

    private val _allChannelsEpg: MutableStateFlow<MutableList<List<Epg>?>> =
        MutableStateFlow(mutableListOf())

    var allChannelsEpg: StateFlow<MutableList<List<Epg>?>> = _allChannelsEpg

    var epgFetchJob: Job? = null
    var currentEpgUpdateJob: Job? = null

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
        val epg = _epgList.value?.getOrNull(currentEpgIndex.value)
        Log.i("current epgIndex", "${_currentEpgIndex.value}")
        epg?.let { e ->
            _currentEpg.value = e
        }
    }

    fun updateEpgIndex(index: Int, isCurrent: Boolean) {
        Log.i("update epg index list size", epgList.value.size.toString())
        if (index in epgList.value.indices) {
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

    fun resetCurrentEpg() {
        _currentEpg.value = Epg()
    }

    fun resetCurrentEpgList() {
        _epgList.value = emptyList()
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

    fun getEpgByIndex(index: Int): Epg? {
        return epgList.value?.getOrNull(index)
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
        val startTimeSeconds = Utils.parseDate(start, datePattern, timezone)
        val stopTimeSeconds = Utils.parseDate(stop, datePattern, timezone)

        return EpgTimeRangeInSeconds(startTimeSeconds, stopTimeSeconds)
    }

    fun updateCurrentEpgList(
        currentEpgList: List<Epg>,
        liveTime: Long,
        currentTime: Long
    ) {
        currentEpgUpdateJob = viewModelScope.launch(Dispatchers.IO) {
            resetEpgIndex(true)
            resetEpgIndex(false)
            resetCurrentEpgList()

            val datePattern = "EEEE d MMMM HH:mm:ss"

            Log.i("live time in update current epg", Utils.formatDate(liveTime, datePattern))
            Log.i("current time in update current epg", Utils.formatDate(currentTime, datePattern))

            val dayAndMonthPattern = "dd MMMM"
            var prevDay = -1
            val newDateMap = mutableMapOf<Int, String>()

            for (i in currentEpgList.indices) {
                val epg = currentEpgList[i]
                val epgTimeRangeInSeconds = getEpgTimeRangeInSeconds(epg.start, epg.stop)
                epg.epgVideoTimeRangeSeconds = epgTimeRangeInSeconds
                epg.epgVideoName = epg.epgVideoName.trim()

                val isProgramCurrent = isTimeWithinProgramRange(epgTimeRangeInSeconds, currentTime)
                val isProgramLive = isTimeWithinProgramRange(epgTimeRangeInSeconds, liveTime)

                if (isProgramCurrent) {
                    Log.i("is program current", "$i $isProgramCurrent $epg ${Utils.formatDate(epgTimeRangeInSeconds.start, datePattern)}")
                }

                if (isProgramLive) {
                    Log.i("is program live", "$i $isProgramLive $epg ${Utils.formatDate(epgTimeRangeInSeconds.start, datePattern)}")
                }

                if (isProgramCurrent) {
                    _focusedEpgIndex.value = i
                    _currentEpgIndex.value = i
                }

                if (isProgramLive) {
                    _liveProgrammeIndex.value = i
                }

                val currentDay = Utils.getCalendarDay(Utils.getCalendar(epg.epgVideoTimeRangeSeconds.start))

                if (i == 0 || currentDay > prevDay) {
                    prevDay = currentDay
                    newDateMap[i] = Utils.formatDate(epg.epgVideoTimeRangeSeconds.start, dayAndMonthPattern)
                }
            }

            _epgList.value = currentEpgList

            if (isCachedEpgDisplayed) {
                isCachedEpgDisplayed = false
            } else {
                if (_focusedEpgIndex.value == -1) {
                    Log.i("is first", "${Utils.formatDate(currentEpgList[0].epgVideoTimeRangeSeconds.start, datePattern)}")
                    Log.i("is first", "${Utils.formatDate(currentTime, datePattern)}")
                    if (currentEpgList[0].epgVideoTimeRangeSeconds.start > currentTime) {
                        updateEpgIndex(0, false)
                    } else {
                        updateEpgIndex(currentEpgList.size-1, false)
                    }
                }
            }

            Log.i("epg view model", "update epg list, focused: ${_focusedEpgIndex.value}, live: ${_liveProgrammeIndex.value}")

            _dateMap.value = newDateMap
            Log.i("epg view model update current epg list", "date map ${_dateMap.value.entries}")
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
        val channelIndex = requestedEpg.channelIndex
        val epgData = getEpgByIdUseCase.getEpgById(
            requestedEpg.epgId,
            token
        )

        saveEpgToCache(requestedEpg.epgId, epgData)
    }

    suspend fun isEpgCached(epgId: Int) = accessEpgCacheUseCase.isEpgCached(epgId)
    suspend fun saveEpgToCache(epgId: Int, epgList: List<Epg>) {
        accessEpgCacheUseCase.saveEpgToCache(epgId, epgList)
    }
    suspend fun getCachedEpg(epgId: Int) = accessEpgCacheUseCase.getCachedEpg(epgId)

    fun searchEpgByTime(time: Long) {
        val foundEpgIndex = epgList.value.indexOfFirst { epg ->
            time in epg.epgVideoTimeRangeSeconds.start..epg.epgVideoTimeRangeSeconds.stop
        }

        if (foundEpgIndex == -1) {
            resetEpgIndex(true)

            if (epgList.value[0].epgVideoTimeRangeSeconds.start > time) {
                updateEpgIndex(0, false)
            } else {
                updateEpgIndex(epgList.value.size-1, false)
            }
        } else {
            updateEpgIndex(foundEpgIndex, true)
            updateEpgIndex(foundEpgIndex, false)
        }
    }
}