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

    var currentProgramme = 0

    @RequiresApi(Build.VERSION_CODES.O)
    fun getEpgById(id: String) {
        viewModelScope.launch {
            val epgList = getEpgByIdUseCase.getEpgById(id)
            val epgGmtConvertedList = mutableListOf<Epg>()

            val currentTimeLong = Utils.convertToGmt4(Utils.getCurrentTime())

            for (i in epgList.indices) {
                val epg = epgList[i]
                val startTimeConverted = Utils.convertToGmt4(epg.startTime)
                val stopTimeConverted = Utils.convertToGmt4(epg.stopTime)

                if (startTimeConverted < currentTimeLong) {
                    currentProgramme = i
                    Log.i("programme", epg.toString())
                }

                epgGmtConvertedList += Epg(startTimeConverted, stopTimeConverted, epg.duration, epg.title)
            }

            _epgList.postValue(epgGmtConvertedList)
        }
    }
}