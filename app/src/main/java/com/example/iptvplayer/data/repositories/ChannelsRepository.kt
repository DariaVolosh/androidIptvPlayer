package com.example.iptvplayer.data.repositories

import android.util.Log
import com.example.iptvplayer.data.backendBaseUrl
import com.example.iptvplayer.retrofit.ChannelBackendInfo
import com.example.iptvplayer.retrofit.ChannelsAndEpgService
import com.example.iptvplayer.retrofit.ChannelsBackendInfoResponse
import kotlinx.coroutines.CompletableDeferred
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

class ChannelsRepository @Inject constructor(
    private val channelsAndEpgService: ChannelsAndEpgService
) {
    suspend fun parseChannelsData(token: String): List<ChannelBackendInfo> {
        val channelsData = CompletableDeferred<List<ChannelBackendInfo>>()

        channelsAndEpgService.getChannelsInfo(token)
            .enqueue(object: Callback<ChannelsBackendInfoResponse> {
                override fun onResponse(
                    call: Call<ChannelsBackendInfoResponse>,
                    response: Response<ChannelsBackendInfoResponse>
                ) {
                    Log.i("channels data", response.code().toString())
                    if (response.isSuccessful && response.code() == 200) {
                        response.body()?.data?.let { data ->
                            for (channelData in data) {
                                val replacedSlashesUrl = channelData.channel[0].logo.replace('\\', '/')
                                val constructedLogoUrl = backendBaseUrl + replacedSlashesUrl
                                channelData.channel[0].logo = constructedLogoUrl
                            }
                            channelsData.complete(data)
                            return
                        }
                    }

                    channelsData.complete(emptyList())
                }

                override fun onFailure(
                    call: Call<ChannelsBackendInfoResponse>,
                    throwable: Throwable
                ) {
                    channelsData.complete(emptyList())
                }
            })

        return channelsData.await()
    }
}