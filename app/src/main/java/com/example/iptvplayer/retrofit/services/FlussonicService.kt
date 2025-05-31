package com.example.iptvplayer.retrofit.services

import com.example.iptvplayer.retrofit.data.DvrRangeResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface FlussonicService {
    @GET("https://streamer.airnet.ge/streamer/api/v3/streams/{name}/dvr/ranges")
    suspend fun getDvrRange(
        @Path("name") streamName: String,
        @Header("Authorization") credentials: String
    ): DvrRangeResponse

    @GET("{name}/{timestamp}.jpg")
    fun getStreamThumbnail(
        @Path("name") streamName: String,
        @Path("timestamp") imageTimestamp: Long
    )
}