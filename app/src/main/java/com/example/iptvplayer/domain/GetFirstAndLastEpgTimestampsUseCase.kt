package com.example.iptvplayer.domain

import com.example.iptvplayer.data.repositories.EpgRepository
import javax.inject.Inject

class GetFirstAndLastEpgTimestampsUseCase @Inject constructor(
    private val epgRepository: EpgRepository
){
    suspend fun getFirstAndLastEpgTimestamps(
        channelId: String,
    ): Pair<Long, Long> {
        return epgRepository.getFirstAndLastEpgTimestamps(channelId)
    }
}