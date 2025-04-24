package com.example.iptvplayer.retrofit.services

import com.example.iptvplayer.retrofit.data.ChannelsAndEpgAuthResponse
import com.example.iptvplayer.retrofit.data.ChannelsBackendInfoResponse
import com.example.iptvplayer.retrofit.data.EpgResponse
import com.example.iptvplayer.retrofit.data.StreamUrlTemplatesResponse
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface ChannelsAndEpgService {
    @POST("Client/Login")
    fun getAuthToken(@Body rawJson: RequestBody): Call<ChannelsAndEpgAuthResponse>

    @GET("Client/GetAllChannels")
    fun getChannelsInfo(
        @Header("Authorization") credentials: String
    ): Call<ChannelsBackendInfoResponse>

    @POST("Client/GetTemplates")
    fun getStreamsUrlTemplates(
        @Body rawJson: RequestBody,
        @Header("Authorization") credentials: String
    ): Call<StreamUrlTemplatesResponse>

    @GET("Client/epg")
    fun getEpgForChannel(
        @Query("ChannelId") channelId: Int,
        @Header("Authorization") credentials: String,
    ): Call<EpgResponse>
}