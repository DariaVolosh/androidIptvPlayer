package com.example.iptvplayer.domain

import com.example.iptvplayer.data.repositories.ChannelsRepository
import javax.inject.Inject

class GetChannelsDataUseCase @Inject constructor(
    private val channelsRepository: ChannelsRepository
){
    suspend fun getChannelsData(token: String) = channelsRepository.parseChannelsData(token)
}