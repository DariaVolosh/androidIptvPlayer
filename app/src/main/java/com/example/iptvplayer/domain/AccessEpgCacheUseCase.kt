package com.example.iptvplayer.domain

import com.example.iptvplayer.data.repositories.EpgRepository
import com.example.iptvplayer.retrofit.data.Epg
import javax.inject.Inject

class AccessEpgCacheUseCase @Inject constructor(
    private val epgRepository: EpgRepository
) {
    suspend fun saveEpgToCache(epgId: Int, epgList: List<Epg>) {
        epgRepository.saveEpgToCache(epgId, epgList)
    }

    suspend fun getCachedEpg(epgId: Int) = epgRepository.getCachedEpg(epgId)

    suspend fun isEpgCached(epgId: Int) = epgRepository.isEpgCached(epgId)
}