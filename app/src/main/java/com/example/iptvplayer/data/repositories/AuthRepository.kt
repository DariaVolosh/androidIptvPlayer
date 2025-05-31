package com.example.iptvplayer.data.repositories

import android.content.Context
import android.util.Log
import com.example.iptvplayer.R
import com.example.iptvplayer.retrofit.services.ChannelsAndEpgService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.RequestBody
import javax.inject.Inject

class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val channelsAndEpgService: ChannelsAndEpgService
) {
    suspend fun getAuthToken(
        requestBody: RequestBody,
        authErrorCallback: (String, String) -> Unit
    ): String  =
        withContext(Dispatchers.IO) {
            Log.i("get auth token", requestBody.contentType().toString())
            val startTime = System.currentTimeMillis()
            val token = channelsAndEpgService.getAuthToken(requestBody).data?.token

            if (token == null) {
                authErrorCallback(
                    context.getString(R.string.auth_failed),
                    context.getString(R.string.auth_failed_descr)
                )
            }

            val stopTime = System.currentTimeMillis()
            Log.i("auth response", token.toString())
            Log.i("parsing time", "${stopTime - startTime} auth repository get auth token")

            token ?: ""
        }
}