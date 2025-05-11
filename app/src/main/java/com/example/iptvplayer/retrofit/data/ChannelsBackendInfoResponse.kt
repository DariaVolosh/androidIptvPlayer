package com.example.iptvplayer.retrofit.data

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Parcelize
@Keep
data class ChannelData(
    var name: String = "",
    var logo: String = "",
    var channelScreenName: String = "",
    var epgChannelId: String = "",
    var channelUrl: String = ""
): Parcelable

@Keep
data class BackendInfoResponse(
    val channel: List<ChannelData>
)

@Keep
data class ChannelsBackendInfoResponse(
    val data: List<BackendInfoResponse>
)
