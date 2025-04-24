package com.example.iptvplayer.data.repositories

import android.util.Log
import com.example.iptvplayer.data.backendBaseUrl
import com.example.iptvplayer.retrofit.data.ChannelBackendInfo
import com.example.iptvplayer.retrofit.data.ChannelsBackendInfoResponse
import com.example.iptvplayer.retrofit.data.StreamUrlTemplate
import com.example.iptvplayer.retrofit.data.StreamUrlTemplatesResponse
import com.example.iptvplayer.retrofit.services.ChannelsAndEpgService
import kotlinx.coroutines.CompletableDeferred
import okhttp3.MediaType.parse
import okhttp3.RequestBody.create
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

class ChannelsRepository @Inject constructor(
    private val channelsAndEpgService: ChannelsAndEpgService
) {
    suspend fun parseChannelsData(
        token: String,
        streamUrlTemplate: String
    ): List<ChannelBackendInfo> {
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
                                val channel = channelData.channel[0]
                                val replacedSlashesUrl = channelData.channel[0].logo.replace('\\', '/')
                                val constructedLogoUrl = backendBaseUrl + replacedSlashesUrl
                                channel.logo = constructedLogoUrl

                                val channelUrl = streamUrlTemplate.replace("[channelname]", channel.name)
                                channel.channelUrl = channelUrl
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

    suspend fun getStreamsUrlTemplates(token: String): List<StreamUrlTemplate> {
        val streamsUrlTemplates = CompletableDeferred<List<StreamUrlTemplate>>()
        Log.i("get templates token", token.toString())

        val mediaType = parse("application/json")
        val requestBody = create(mediaType, "{}")

        channelsAndEpgService.getStreamsUrlTemplates(requestBody, token)
            .enqueue(object: Callback<StreamUrlTemplatesResponse> {
                override fun onResponse(
                    call: Call<StreamUrlTemplatesResponse>,
                    response: Response<StreamUrlTemplatesResponse>
                ) {
                    Log.i("response code templ", response.code().toString())
                    if (response.code() == 200) {
                        response.body()?.data?.templates?.let { templates ->
                            streamsUrlTemplates.complete(templates)
                            Log.i("templates res", templates.toString())
                            return
                        }
                    }

                    streamsUrlTemplates.complete(emptyList())
                }

                override fun onFailure(
                    cal: Call<StreamUrlTemplatesResponse>,
                    throwable: Throwable
                ) {
                    Log.i("on failure", throwable.localizedMessage)
                    streamsUrlTemplates.complete(emptyList())
                }
            })

        return streamsUrlTemplates.await()
    }
}