package com.example.iptvplayer.data.repositories

import android.util.Log
import com.example.iptvplayer.retrofit.DvrRangeResponse
import com.example.iptvplayer.retrofit.FlussonicService
import kotlinx.coroutines.CompletableDeferred
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class FlussonicRepository @Inject constructor(
    private val flussonicService: FlussonicService
){
    // stream name is the same as tvg-id tag in a m3u8 playlist
    suspend fun getDvrRange(streamName: String): Pair<Long, Long> {
        val credentials = createBasicAuthCredentials("admin", "ysH2SAvQ")
        val authHeader = "Basic $credentials"

        Log.i("get dvr range stream name", streamName)

        val dvrRange = CompletableDeferred<Pair<Long, Long>>()
        val call = flussonicService.getDvrRange(streamName, authHeader)

        call.enqueue(object: Callback<DvrRangeResponse> {
            override fun onResponse(call: Call<DvrRangeResponse>, response: Response<DvrRangeResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let { dvrRangeResponse ->
                        if (dvrRangeResponse.ranges.isEmpty()) {
                            dvrRange.complete(Pair(0,0))
                        } else {
                            val startSeconds = dvrRangeResponse.ranges[0].from
                            val endSeconds = startSeconds + dvrRangeResponse.ranges[0].duration
                            dvrRange.complete(Pair(startSeconds, endSeconds))
                        }
                    }
                } else {
                    dvrRange.complete(Pair(0,0))
                }
            }

            override fun onFailure(call: Call<DvrRangeResponse>, exception: Throwable) {
                dvrRange.complete(Pair(0,0))

            }
        })

        return dvrRange.await()
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun createBasicAuthCredentials(username: String, password: String): String {
        val credentials = "$username:$password"
        return Base64.encode(credentials.toByteArray())
    }
}