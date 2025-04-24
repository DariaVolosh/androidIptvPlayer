package com.example.iptvplayer.retrofit.services

import com.example.iptvplayer.retrofit.data.DvrRangeResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface FlussonicService {
    @GET("streamer/api/v3/streams/{name}/dvr/ranges")
    fun getDvrRange(
        @Path("name") streamName: String,
        @Header("Authorization") credentials: String
    ): Call<DvrRangeResponse>

    @GET("{name}/{timestamp}.jpg")
    fun getStreamThumbnail(
        @Path("name") streamName: String,
        @Path("timestamp") imageTimestamp: Long
    )
}