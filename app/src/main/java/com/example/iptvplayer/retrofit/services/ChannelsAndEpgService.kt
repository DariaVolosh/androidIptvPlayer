package com.example.iptvplayer.retrofit.services

import com.example.iptvplayer.retrofit.data.ChannelsBackendInfoResponse
import com.example.iptvplayer.retrofit.data.EpgResponse
import com.example.iptvplayer.retrofit.data.StreamUrlTemplatesResponse
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ChannelsAndEpgService {
    @GET("Client/GetAllChannels")
    suspend fun getChannelsInfo(): ChannelsBackendInfoResponse

    @POST("Client/GetTemplates")
    suspend fun getStreamsUrlTemplates(
        @Body rawJson: RequestBody
    ): StreamUrlTemplatesResponse

    @GET("Client/epg")
    suspend fun getEpgForChannel(
        @Query("ChannelId") channelId: Int
    ): EpgResponse
}