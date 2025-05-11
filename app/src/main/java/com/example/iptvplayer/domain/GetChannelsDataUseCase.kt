package com.example.iptvplayer.domain

import android.util.Log
import com.example.iptvplayer.data.repositories.ChannelsRepository
import com.example.iptvplayer.retrofit.data.ChannelData
import javax.inject.Inject

class GetChannelsDataUseCase @Inject constructor(
    private val channelsRepository: ChannelsRepository
){
    suspend fun getChannelsData(token: String): List<ChannelData> {
        val streamTemplates = channelsRepository.getStreamsUrlTemplates(token)
        Log.i("stream received", streamTemplates[0].toString())
        return channelsRepository.parseChannelsData(token, streamTemplates[0].template)
    }
}