package com.example.iptvplayer.data.repositories

import android.util.Log
import com.example.iptvplayer.retrofit.services.ChannelsAndEpgService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.RequestBody
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val channelsAndEpgService: ChannelsAndEpgService
) {
    suspend fun getAuthToken(requestBody: RequestBody): String  =
        withContext(Dispatchers.IO) {
            Log.i("get auth token", requestBody.contentType().toString())
            val startTime = System.currentTimeMillis()
            val token: String = channelsAndEpgService.getAuthToken(requestBody).data.token

            val stopTime = System.currentTimeMillis()
            Log.i("parsing time", "${stopTime - startTime} auth repository get auth token")

            token
        }
}