package com.example.iptvplayer.domain

import com.example.iptvplayer.data.repositories.EpgRepository
import javax.inject.Inject

class FetchChannelEpgUseCase @Inject constructor(
    private val epgRepository: EpgRepository
) {
    suspend fun fetchChannelEpg(channelName: String) =
        epgRepository.fetchChannelEpg(channelName)
}