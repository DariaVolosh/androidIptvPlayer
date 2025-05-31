package com.example.iptvplayer.domain.epg

import com.example.iptvplayer.data.repositories.EpgRepository
import com.example.iptvplayer.retrofit.data.EpgListItem
import javax.inject.Inject

class AccessEpgCacheUseCase @Inject constructor(
    private val epgRepository: EpgRepository
) {
    suspend fun saveEpgToCache(epgId: Int, epgList: List<EpgListItem.Epg>) {
        epgRepository.saveEpgToCache(epgId, epgList)
    }

    suspend fun getCachedEpg(epgId: Int) = epgRepository.getCachedEpg(epgId)

    suspend fun isEpgCached(epgId: Int) = epgRepository.isEpgCached(epgId)
}