package com.example.iptvplayer.retrofit.services

import com.example.iptvplayer.retrofit.data.ChannelsAndEpgAuthResponse
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthService {
    @POST("Client/Login")
    suspend fun getAuthToken(@Body rawJson: RequestBody): ChannelsAndEpgAuthResponse
}