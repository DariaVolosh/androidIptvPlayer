package com.example.iptvplayer.data.repositories

import android.util.Log
import com.example.iptvplayer.data.Utils
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
        ***REMOVED***
        val credentials = createBasicAuthCredentials("admin", "12345670")
        ***REMOVED***
        ***REMOVED***
        val authHeader = "Basic $credentials"

        Log.i("auth header", authHeader)

        Log.i("get dvr range stream name", streamName)

        val dvrRange = CompletableDeferred<Pair<Long, Long>>()
        val call = flussonicService.getDvrRange(streamName, authHeader)

        call.enqueue(object: Callback<DvrRangeResponse> {
            override fun onResponse(call: Call<DvrRangeResponse>, response: Response<DvrRangeResponse>) {
                if (response.isSuccessful) {
                    Log.i("dvr range call success", "is")
                    response.body()?.let { dvrRangeResponse ->
                        if (dvrRangeResponse.ranges.isEmpty()) {
                            Log.i("dvr range call success", "range empty")
                            dvrRange.complete(Pair(0,0))
                        } else {
                            val datePattern = "EEEE d MMMM HH:mm:ss"

                            var startSeconds = 0L
                            var endSeconds = 0L

                            Log.i("ranges size", dvrRangeResponse.ranges.size.toString())

                            if (dvrRangeResponse.ranges.size == 1) {
                                startSeconds = dvrRangeResponse.ranges[0].from
                                endSeconds = startSeconds + dvrRangeResponse.ranges[0].duration
                            } else {
                                // condition if ranges are multiple (there are parts that are not recorded)
                                // can be more flexible but for now i assume that these parts are not long and its okay to
                                // count them as recorded
                                var minStartSeconds = 0L
                                var maxEndSeconds = 0L

                                for (range in dvrRangeResponse.ranges) {
                                    if (minStartSeconds == 0L && maxEndSeconds == 0L) {
                                        minStartSeconds = range.from
                                        maxEndSeconds = minStartSeconds + range.duration
                                    }

                                    val start = range.from
                                    val end = start + range.duration

                                    if (start < minStartSeconds) minStartSeconds = start
                                    if (end > maxEndSeconds) maxEndSeconds = end
                                }

                                startSeconds = minStartSeconds
                                endSeconds = maxEndSeconds
                            }

                            Log.i("on response", "${Utils.formatDate(startSeconds, datePattern)} ${Utils.formatDate(endSeconds, datePattern)}")

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