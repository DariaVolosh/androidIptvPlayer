package com.example.iptvplayer.view.epg

import android.os.Build
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

    @RequiresApi(Build.VERSION_CODES.O)
    fun getEpgById(id: String) {
        viewModelScope.launch {
            val epgList = getEpgByIdUseCase.getEpgById(id)
            val epgTimesFormattedList = mutableListOf<Epg>()

            for (epg in epgList) {
                val formattedStartTime = Utils.convertToGmt4(epg.startTime.toLong())
                val formattedStopTime = Utils.convertToGmt4(epg.stopTime.toLong())
                val formattedTimeEpg = Epg(formattedStartTime, formattedStopTime, epg.title)
                epgTimesFormattedList += formattedTimeEpg
            }

            _epgList.postValue(epgTimesFormattedList)
        }
    }
}