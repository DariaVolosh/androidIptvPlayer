package com.example.iptvplayer.retrofit

import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface ChannelsAndEpgService {
    @POST("Client/Login")
    fun getAuthToken(@Body rawJson: RequestBody): Call<ChannelsAndEpgAuthResponse>

    @GET("Client/GetAllChannels")
    fun getChannelsInfo(
        @Header("Authorization") credentials: String
    ): Call<ChannelsBackendInfoResponse>
}