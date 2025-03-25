package com.example.iptvplayer.domain

import com.example.iptvplayer.data.Epg
import com.example.iptvplayer.data.repositories.EpgRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetEpgByIdUseCase @Inject constructor(
    private val epgRepository: EpgRepository
) {
    fun getEpgById(channelId: String, dvrRange: Pair<Long, Long>): Flow<List<Epg>> =
        epgRepository.getEpgById(channelId, dvrRange)
}