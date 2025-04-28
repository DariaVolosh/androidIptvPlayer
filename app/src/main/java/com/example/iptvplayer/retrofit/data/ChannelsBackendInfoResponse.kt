package com.example.iptvplayer.retrofit.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ChannelData(
    var name: String,
    var logo: String,
    var channelScreenName: String,
    var epgChannelId: String,
    var channelUrl: String
): Parcelable

data class ChannelBackendInfo(
    val channel: List<ChannelData>
)

data class ChannelsBackendInfoResponse(
    val data: List<ChannelBackendInfo>
)
