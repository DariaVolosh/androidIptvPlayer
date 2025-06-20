package com.example.iptvplayer.data.repositories

import android.util.Log
import com.example.iptvplayer.retrofit.data.DvrRange
import com.example.iptvplayer.retrofit.services.FlussonicService
import javax.inject.Inject
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class FlussonicRepository @Inject constructor(
    private val flussonicService: FlussonicService
){
    // stream name is the same as tvg-id tag in a m3u8 playlist
    suspend fun getDvrRanges(streamName: String): List<DvrRange> {

        ***REMOVED***
        val credentials = createBasicAuthCredentials("admin", "12345670")
        ***REMOVED***
        ***REMOVED***
        val authHeader = "Basic $credentials"

        Log.i("auth header", authHeader)
        Log.i("get dvr range stream name", streamName)

        try {
            val response = flussonicService.getDvrRange(streamName, authHeader)
            return response.ranges
        } catch (e: Exception) {
            Log.i("e caught", e.localizedMessage.toString())
            return emptyList()
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun createBasicAuthCredentials(username: String, password: String): String {
        val credentials = "$username:$password"
        return Base64.encode(credentials.toByteArray())
    }
}