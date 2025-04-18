package com.example.iptvplayer.retrofit

data class ChannelData(
    var name: String,
    var logo: String,
    var channelScreenName: String,
    var epgChannelId: String
)

data class ChannelBackendInfo(
    val channel: List<ChannelData>
)

data class ChannelsBackendInfoResponse(
    val data: List<ChannelBackendInfo>
)
