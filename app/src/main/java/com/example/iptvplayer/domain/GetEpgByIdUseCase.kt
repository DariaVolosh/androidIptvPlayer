package com.example.iptvplayer.domain

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.iptvplayer.data.Epg
import com.example.iptvplayer.data.repositories.EpgRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetEpgByIdUseCase @Inject constructor(
    private val epgRepository: EpgRepository
) {
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getEpgById(channelId: String): Flow<List<Epg>> {
        val countryCode = epgRepository.getCountryCodeById(channelId)
        val epgFlow = epgRepository.getEpgById(channelId, countryCode)
        return epgFlow
    }
}