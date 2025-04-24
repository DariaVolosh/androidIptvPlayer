package com.example.iptvplayer.domain

import com.example.iptvplayer.data.repositories.EpgRepository
import javax.inject.Inject

class GetFirstAndLastEpgMonthUseCase @Inject constructor(
    private val epgRepository: EpgRepository
) {
    /*suspend fun getFirstAndLastEpgMonth(channelId: String) =
        epgRepository.getFirstAndLastMonths(channelId) */
}