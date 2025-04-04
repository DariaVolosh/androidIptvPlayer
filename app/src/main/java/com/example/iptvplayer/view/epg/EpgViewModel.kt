package com.example.iptvplayer.view.epg

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iptvplayer.data.Epg
import com.example.iptvplayer.data.Utils
import com.example.iptvplayer.domain.GetEpgByIdUseCase
import com.example.iptvplayer.domain.GetFirstAndLastEpgMonthUseCase
import com.example.iptvplayer.domain.GetFirstAndLastEpgTimestampsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EpgViewModel @Inject constructor(
    private val getEpgByIdUseCase: GetEpgByIdUseCase,
    private val getFirstAndLastEpgMonthUseCase: GetFirstAndLastEpgMonthUseCase,
    private val getFirstAndLastEpgTimestampsUseCase: GetFirstAndLastEpgTimestampsUseCase,
    //private val getCountryCodeByIdUseCase: GetCountryCodeByIdUseCase
): ViewModel() {
    private val _epgList: MutableLiveData<List<Epg>> = MutableLiveData()
    val epgList: LiveData<List<Epg>> = _epgList

    // focused epg index, that changes when the user scrolls epg list
    private val _focusedEpgIndex: MutableLiveData<Int> = MutableLiveData(-1)
    val focusedEpgIndex: LiveData<Int> = _focusedEpgIndex

    // currently chosen epg, the program, that should be played
    private val _currentEpg: MutableLiveData<Epg> = MutableLiveData()
    val currentEpg: LiveData<Epg> = _currentEpg

    private val _currentEpgIndex: MutableLiveData<Int> = MutableLiveData(-1)
    val currentEpgIndex: LiveData<Int> = _currentEpgIndex

    // defining live programme as a live data because in main composable inside launched effect
    // the new value will not be captured if it is not specified as a key in launched effect
    private var _liveProgrammeIndex: MutableLiveData<Int> = MutableLiveData(-1)
    val liveProgrammeIndex: LiveData<Int> = _liveProgrammeIndex

    private val _firstAndLastEpgDay: MutableLiveData<Pair<Int,Int>> = MutableLiveData()
    val firstAndLastEpgDay: LiveData<Pair<Int, Int>> = _firstAndLastEpgDay

    private val _epgMonth: MutableLiveData<Int> = MutableLiveData()
    val epgMonth: LiveData<Int> = _epgMonth

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
        "ch168" to "ch057"
    )


    fun updateCurrentEpg() {
        epgList.value?.let { epgList ->
            if (currentEpgIndex.value != -1) {
                _currentEpg.value = epgList[currentEpgIndex.value ?: 0]
            }
        }
    }

    fun updateCurrentEpgIndex(current: Int) {
        epgList.value?.size?.let { epgSize ->
            if (current < epgSize && current >= 0) {
                _currentEpgIndex.value = current
                updateCurrentEpg()
            }
        }
    }

    fun updateFocusedEpgIndex(index: Int) {
        _focusedEpgIndex.value = index
    }

    fun updateLiveProgramme(index: Int) {
        _liveProgrammeIndex.value = index
    }

    fun getEpgByIndex(index: Int): Epg? {
        return epgList.value?.getOrNull(index)
    }

    suspend fun getFirstAndLastEpgDays(channelId: String) {
        val firstAndLastEpgTimestamps = getFirstAndLastEpgTimestampsUseCase.getFirstAndLastEpgTimestamps(
            channelId
        )

        Log.i("FIRST AND LAST EPG TIMESTAMPS", firstAndLastEpgTimestamps.toString())

        if (firstAndLastEpgTimestamps.first == -1L) return

        val firstEpgCalendar = Utils.getCalendar(firstAndLastEpgTimestamps.first)
        val lastEpgCalendar = Utils.getCalendar(firstAndLastEpgTimestamps.second)

        val firstEpgDay = Utils.getCalendarDay(firstEpgCalendar)
        val lastEpgDay = Utils.getCalendarDay(lastEpgCalendar)

        Log.i("FIRST DAY", firstEpgDay.toString())
        Log.i("LAST DAY", lastEpgDay.toString())

        _firstAndLastEpgDay.value = Pair(firstEpgDay, lastEpgDay)
    }

    private var epgCollectionJob: Job? = null

    fun getEpgById(channelId: String, dvrRange: Pair<Long,Long>) {
        epgCollectionJob?.cancel()
        _liveProgrammeIndex.value = -1
        _focusedEpgIndex.value = -1
        _currentEpgIndex.value = -1
        _epgList.value = listOf()

        Log.i("epg month channel id", "called")
        val mappedEpgId = channelsIdToEpgIdMapper[channelId] ?: ""
        Log.i("epg month channel id", "playlist $channelId")
        Log.i("getEpgById called", dvrRange.toString())

        if (mappedEpgId == "") return

        Log.i("getEpgById called", "$channelId")

        epgCollectionJob = viewModelScope.launch {
            //val countryCode = getCountryCodeByIdUseCase.getCountryCodeById(mappedEpgId)
            Log.i("epg month channel id", "playlist $mappedEpgId")
            /*val epgMonth = getFirstAndLastEpgMonthUseCase.getFirstAndLastEpgMonth(mappedEpgId)
            Log.i("epg month", epgMonth.toString())
            if (epgMonth == -1) return@launch
            _epgMonth.value = epgMonth */

            getFirstAndLastEpgDays(mappedEpgId)

            val epgFlow = getEpgByIdUseCase.getEpgById(mappedEpgId, dvrRange)
            val allDaysEpgList = mutableListOf<Epg>()
            val currentTime = Utils.getGmtTime()

            epgFlow.collect { dayEpg ->
                var isPreviousDay = false
                var previousDayInsertionIndex = 0

                val calendar = Utils.getCalendar(dayEpg[1].startTime)

                val currentDay = Utils.getCalendarDay(calendar)
                Log.i("current fetched epg day", currentDay.toString())

                for (i in dayEpg.indices) {
                    val epg = dayEpg[i]
                    val startTime = epg.startTime
                    val stopTime = epg.stopTime
                    val title = epg.title

                    Log.i("COLLECTED EPG", "$epg $i")
                    Log.i("COLLECTED EPG", "START TIME: ${Utils.formatDate(startTime, "EEEE d MMMM HH:mm:ss", java.util.TimeZone.getTimeZone("Z"))}")

                    if (i == 0) {
                        if (startTime == -2L && stopTime == -2L && title == "") {
                            isPreviousDay = true
                        }
                    } else {
                        if (isPreviousDay) {
                            allDaysEpgList.add(previousDayInsertionIndex++, epg)
                        } else {
                            allDaysEpgList.add(epg)
                        }

                        if (_focusedEpgIndex.value == -1 && startTime <= currentTime && stopTime >= currentTime) {
                            //Log.i("CURRENT EPG", "$epg $i ${i-1} ${dayEpg.size}")
                            _focusedEpgIndex.value = i - 1
                            _currentEpgIndex.value = i - 1
                            _liveProgrammeIndex.value = i - 1
                        }

                        if (i == dayEpg.size - 1) {
                            //Log.i("FOCUSED PROGRAMME", _focusedEpgIndex.value.toString())
                            //Log.i("changed focused programme", "${dayEpg.size} ${allDaysEpgList.size}")

                            _epgList.value = allDaysEpgList
                            if (isPreviousDay && _focusedEpgIndex.value != -1) {
                                _focusedEpgIndex.value?.let { focused ->
                                    Log.i("changed focused programme", "$focused ${dayEpg.size} ${allDaysEpgList.size} $channelId")
                                    _focusedEpgIndex.value = focused + dayEpg.size - 1
                                    _currentEpgIndex.value = focused + dayEpg.size - 1
                                }

                                _liveProgrammeIndex.value?.let { live ->
                                    _liveProgrammeIndex.value = live + dayEpg.size - 1
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}