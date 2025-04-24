package com.example.iptvplayer.domain

import com.example.iptvplayer.data.repositories.EpgRepository
import javax.inject.Inject

/*class GetEpgByIdUseCase @Inject constructor(
    private val epgRepository: EpgRepository
) {
    /*fun getEpgById(channelId: String, dvrRange: Pair<Long, Long>): Flow<List<Epg>> =
        epgRepository.getEpgById(channelId, dvrRange) */
} */

// NEWER

class GetEpgByIdUseCase @Inject constructor(
    private val epgRepository: EpgRepository
) {
        suspend fun getEpgById(channelId: Int, token: String) = epgRepository.getEpgById(channelId, token)
}