package com.example.iptvplayer.data.repositories

import android.util.Log
import com.example.iptvplayer.data.Utils
import com.example.iptvplayer.retrofit.services.FlussonicService
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

        var dvrRange: Pair<Long, Long>
        val response = flussonicService.getDvrRange(streamName, authHeader)

        response.let { dvrRangeResponse ->
            if (dvrRangeResponse.ranges.isEmpty()) {
                Log.i("dvr range call success", "range empty")
                dvrRange = Pair(-1, -1)
            } else {
                val datePattern = "EEEE d MMMM HH:mm:ss"

                var startSeconds: Long
                var endSeconds: Long

                Log.i("ranges size", dvrRangeResponse.ranges.size.toString())

                if (dvrRangeResponse.ranges.size == 1) {
                    val range = dvrRangeResponse.ranges[0]
                    startSeconds = range.from
                    endSeconds = startSeconds + range.duration
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

                Log.i(
                    "on response dvr",
                    "${Utils.formatDate(startSeconds, datePattern)} ${
                        Utils.formatDate(
                            endSeconds,
                            datePattern
                        )
                    }"
                )

                dvrRange = Pair(startSeconds, endSeconds)
            }
        }

        return dvrRange
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun createBasicAuthCredentials(username: String, password: String): String {
        val credentials = "$username:$password"
        return Base64.encode(credentials.toByteArray())
    }
}