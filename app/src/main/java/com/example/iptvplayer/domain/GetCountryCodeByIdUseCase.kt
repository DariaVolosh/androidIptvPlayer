package com.example.iptvplayer.domain

import com.example.iptvplayer.data.repositories.EpgRepository
import javax.inject.Inject

class GetCountryCodeByIdUseCase @Inject constructor(
    private val epgRepository: EpgRepository
) {
    suspend fun getCountryCodeById(channelId: String) =
        epgRepository.getCountryCodeById(channelId)
}