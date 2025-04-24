package com.example.iptvplayer.domain

import com.example.iptvplayer.data.repositories.ChannelsRepository
import com.example.iptvplayer.retrofit.data.ChannelBackendInfo
import javax.inject.Inject

class GetChannelsDataUseCase @Inject constructor(
    private val channelsRepository: ChannelsRepository
){
    suspend fun getChannelsData(token: String): List<ChannelBackendInfo> {
        val streamTemplates = channelsRepository.getStreamsUrlTemplates(token)
        return channelsRepository.parseChannelsData(token, streamTemplates[0].template)
    }
}