package com.example.iptvplayer.domain

import com.example.iptvplayer.data.repositories.ChannelsRepository
import javax.inject.Inject

class GetStreamsUrlTemplatesUseCase @Inject constructor(
    private val channelsRepository: ChannelsRepository
) {
    suspend fun getStreamsUrlTemplates(token: String) = channelsRepository.getStreamsUrlTemplates(token)
}