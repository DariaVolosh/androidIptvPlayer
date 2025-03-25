package com.example.iptvplayer.domain

import com.example.iptvplayer.data.repositories.EpgRepository
import javax.inject.Inject

class GetEpgMonthUseCase @Inject constructor(
    private val epgRepository: EpgRepository
) {
    suspend fun getEpgMonth(channelId: String) =
        epgRepository.getEpgMonth(channelId)
}