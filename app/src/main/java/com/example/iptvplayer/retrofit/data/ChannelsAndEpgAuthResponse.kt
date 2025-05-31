package com.example.iptvplayer.retrofit.data

data class AuthInfo(
    val token: String
)

data class ChannelsAndEpgAuthResponse(
    val data: AuthInfo?
)
