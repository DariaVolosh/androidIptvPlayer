package com.example.iptvplayer.view.epg

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iptvplayer.data.Epg
import com.example.iptvplayer.data.Utils
import com.example.iptvplayer.domain.GetEpgByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EpgViewModel @Inject constructor(
    private val getEpgByIdUseCase: GetEpgByIdUseCase
): ViewModel() {
    private val _epgList: MutableLiveData<List<Epg>> = MutableLiveData()
    val epgList: LiveData<List<Epg>> = _epgList

    private val _focusedEpgIndex: MutableLiveData<Int> = MutableLiveData()
    val focusedEpgIndex: LiveData<Int> = _focusedEpgIndex

    private val _focusedEpg: MutableLiveData<Epg> = MutableLiveData()
    val focusedEpg: LiveData<Epg> = _focusedEpg

    // defining live programme as a live data because in main composable inside launched effect
    // the new value will not be captured if it is not specified as a key in launched effect
    private var _liveProgramme: MutableLiveData<Int> = MutableLiveData()
    val liveProgramme: LiveData<Int> = _liveProgramme

    private fun updateFocusedEpg() {
        _focusedEpg.value = epgList.value?.getOrNull(focusedEpgIndex.value ?: 0)
    }

    fun updateFocusedEpgIndex(focused: Int) {
        epgList.value?.size?.let { epgSize ->
            if (focused < epgSize && focused >= 0) {
                _focusedEpgIndex.value = focused
                updateFocusedEpg()
            }
        }
    }

    fun updateLiveProgramme(index: Int) {
        _liveProgramme.value = index
    }

    fun getEpgByIndex(index: Int): Epg? {
        return epgList.value?.getOrNull(index)
    }

    private var epgCollectionJob: Job? = null

    @RequiresApi(Build.VERSION_CODES.O)
    fun getEpgById(channelId: String) {
        if (channelId == "-1") return
        epgCollectionJob?.cancel()
        _liveProgramme.value = -1
        _focusedEpgIndex.value = -1
        _epgList.value = listOf()

        epgCollectionJob = viewModelScope.launch {
            val epgFlow = getEpgByIdUseCase.getEpgById(channelId)
            val allDaysEpgList = mutableListOf<Epg>()

            val currentTime = Utils.getGmtTime()

            epgFlow.collect { dayEpg ->
                var isPreviousDay = false
                var previousDayInsertionIndex = 0

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
                            Log.i("CURRENT EPG", "$epg $i ${i-1} ${dayEpg.size}")
                            _focusedEpgIndex.value = i - 1
                            _liveProgramme.value = i - 1
                        }

                        if (i == dayEpg.size - 1) {
                            Log.i("FOCUSED PROGRAMME", _focusedEpgIndex.value.toString())
                            Log.i("changed focused programme", "${dayEpg.size} ${allDaysEpgList.size}")
                            if (isPreviousDay && _focusedEpgIndex.value != -1) {
                                _focusedEpgIndex.value?.let { focused ->
                                    Log.i("changed focused programme", "$focused ${dayEpg.size} ${allDaysEpgList.size}")
                                    _focusedEpgIndex.value = focused + dayEpg.size - 1
                                }

                                _liveProgramme.value?.let { live ->
                                    _liveProgramme.value = live + dayEpg.size - 1
                                }
                            }

                            _epgList.value = allDaysEpgList
                        }
                    }
                }
            }
        }
    }
}