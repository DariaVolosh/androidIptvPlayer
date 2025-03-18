package com.example.iptvplayer.retrofit

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface FlussonicService {
    @GET("streams/{name}/dvr/ranges")
    fun getDvrRange(
        @Path("name") streamName: String,
        @Header("Authorization") credentials: String
    ): Call<DvrRangeResponse>
}