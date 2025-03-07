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
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EpgViewModel @Inject constructor(
    private val getEpgByIdUseCase: GetEpgByIdUseCase
): ViewModel() {
    private val _epgList: MutableLiveData<List<Epg>> = MutableLiveData()
    val epgList: LiveData<List<Epg>> = _epgList

    private var _currentProgramme: MutableLiveData<Int> = MutableLiveData()
    val currentProgramme: LiveData<Int> = _currentProgramme

    fun updateCurrentProgramme(index: Int) {
        _currentProgramme.value = index
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getEpgById(channelId: String) {
        if (channelId == "ch003") {
            viewModelScope.launch {
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
                                _currentProgramme.value?.let { focused ->
                                    Log.i("SHIT LMAO", (focused + oneDayEpgFetched).toString())
                                    _currentProgramme.value = focused + oneDayEpgFetched
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
                        if (_currentProgramme.value == null && epg.startTime <= currentTime && epg.stopTime >= currentTime) {
                            Log.i("CAUGHT", "SS")
                            _currentProgramme.value = currentProgrammeI
                        }

                        if (_currentProgramme.value == null) currentProgrammeI += 1
                        oneDayEpgFetched++
                    }
                }
            }
        }
    }
}