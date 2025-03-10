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

    private val _focusedProgramme: MutableLiveData<Int> = MutableLiveData()
    val focusedProgramme: LiveData<Int> = _focusedProgramme

    // defining live programme as a live data because in main composable inside launched effect
    // the new value will not be captured if it is not specified as a key in launched effect
    private var _liveProgramme: MutableLiveData<Int> = MutableLiveData()
    val liveProgramme: LiveData<Int> = _liveProgramme

    fun updateFocusedProgramme(index: Int) {
        _focusedProgramme.value = index
    }

    fun updateLiveProgramme(index: Int) {
        _liveProgramme.value = index
    }

    private var epgCollectionJob: Job? = null

    @RequiresApi(Build.VERSION_CODES.O)
    fun getEpgById(channelId: String) {
        epgCollectionJob?.cancel()
        _liveProgramme.value = -1
        _focusedProgramme.value = -1
        epgCollectionJob = viewModelScope.launch {
            val epgFlow = getEpgByIdUseCase.getEpgById(channelId)
            val epgList = mutableListOf<Epg>()

            val currentTime = Utils.getGmtTime()
            var currentProgrammeI = -1

            var currentDayFetched = false
            var previousDayFetched = false

            var oneDayEpgFetched = 0
            var previousDayInsertionIndex = 0

            epgFlow.collect { epg ->
                // last epg of the day was fetched
                if (epg.startTime == 0L && epg.stopTime == 0L) {
                    if (!currentDayFetched) {
                        currentDayFetched = true
                    } else {
                        previousDayFetched = !previousDayFetched
                        if (previousDayFetched) {
                            previousDayInsertionIndex = 0
                            _focusedProgramme.value?.let { focused ->
                                Log.i("SHIT LMAO", (focused + oneDayEpgFetched).toString())
                                _focusedProgramme.value = focused + oneDayEpgFetched
                                _liveProgramme.value = focused + oneDayEpgFetched
                                Log.i("LIVE", "VIEW MODEL $_liveProgramme")
                            }
                        }
                    }

                    oneDayEpgFetched = 0

                    Log.i("VIEWMODEL", epgList.toString())
                    _epgList.value = epgList.toList()
                    Log.i("VIEWMODEL", epgList.size.toString())
                } else {
                    if (!currentDayFetched) {
                        epgList += epg
                    } else {
                        if (!previousDayFetched) {
                            epgList.add(previousDayInsertionIndex++, epg)
                        } else {
                            epgList.add(epg)
                        }
                    }

                    Log.i("COMPARED", "${epg.startTime} $currentTime ${epg.stopTime} ${epg.title}")
                    if (_focusedProgramme.value == null && epg.startTime <= currentTime && epg.stopTime >= currentTime) {
                        Log.i("CAUGHT", "SS")
                        _focusedProgramme.value = currentProgrammeI
                        _liveProgramme.value = currentProgrammeI
                    }

                    if (_focusedProgramme.value == null) currentProgrammeI += 1
                    oneDayEpgFetched++
                }
            }
        }
    }
}