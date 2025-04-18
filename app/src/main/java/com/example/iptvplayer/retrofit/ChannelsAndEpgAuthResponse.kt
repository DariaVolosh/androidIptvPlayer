package com.example.iptvplayer.retrofit

data class AuthInfo(
    val token: String
)

data class ChannelsAndEpgAuthResponse(
    val data: AuthInfo
)
