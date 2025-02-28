package com.example.iptvplayer.domain

import com.example.iptvplayer.data.Epg
import com.example.iptvplayer.data.repositories.EpgRepository
import javax.inject.Inject

class GetEpgByIdUseCase @Inject constructor(
    private val epgRepository: EpgRepository
) {
    suspend fun getEpgById(id: String): List<Epg> {
        val countryCode = epgRepository.getCountryCodeById(id)
        val epg = epgRepository.getEpgById(id, countryCode)
        return epg
    }
}