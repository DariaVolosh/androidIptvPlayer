package com.example.iptvplayer.data.repositories

import android.content.Context
import android.os.Looper
import android.util.Log
import com.example.iptvplayer.R
import com.example.iptvplayer.data.BACKEND_BASE_URL
import com.example.iptvplayer.retrofit.data.ChannelData
import com.example.iptvplayer.retrofit.data.StreamUrlTemplate
import com.example.iptvplayer.retrofit.data.StreamUrlTemplatesResponse
import com.example.iptvplayer.retrofit.services.ChannelsAndEpgService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.parse
import okhttp3.RequestBody.create
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

class ChannelsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val channelsAndEpgService: ChannelsAndEpgService
) {
    suspend fun parseChannelsData(
        token: String,
        streamUrlTemplate: String,
        channelsErrorCallback: (String, String) -> Unit
    ): List<ChannelData> =
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            val channelsData = mutableListOf<ChannelData>()

            val isOnMainThread = Looper.getMainLooper() == Looper.myLooper()
            Log.i("is on main thread channels list", "parse channels data $isOnMainThread")
            val response = channelsAndEpgService.getChannelsInfo(token)
            Log.i("channels response", response.toString())

            response.data?.let { data ->
                for (channelData in data) {
                    Log.i("is on main thread channels list", "parse channels data response $isOnMainThread")
                    val channel = channelData.channel[0]
                    val replacedSlashesUrl = channelData.channel[0].logo.replace('\\', '/')
                    val constructedLogoUrl = BACKEND_BASE_URL + replacedSlashesUrl
                    channel.logo = constructedLogoUrl

                    val channelUrl = streamUrlTemplate.replace("[channelname]", channel.name)
                    channel.channelUrl = channelUrl
                    channelsData.add(channel)
                    Log.i("channels repository", "parse channels data $channel")
                }
            }

            if (channelsData.isEmpty()) {
                channelsErrorCallback(
                    context.getString(R.string.no_channels),
                    context.getString(R.string.no_channels_descr)
                )
            }

            val stopTime = System.currentTimeMillis()
            Log.i("parsing time","${(stopTime-startTime)} channels repository parse channels data")

            channelsData
    }

    suspend fun getStreamsUrlTemplates(token: String): List<StreamUrlTemplate> =
        withContext(Dispatchers.IO) {
            var streamsUrlTemplates = CompletableDeferred<List<StreamUrlTemplate>>()
            Log.i("get templates token", token.split(" ")[1])
            val startTime = System.currentTimeMillis()

            val mediaType = parse("application/json")
            val requestBody = create(mediaType, "{}")

            channelsAndEpgService.getStreamsUrlTemplates(requestBody, token)
                .enqueue(object: Callback<StreamUrlTemplatesResponse> {
                    override fun onResponse(
                        p0: Call<StreamUrlTemplatesResponse>,
                        p1: Response<StreamUrlTemplatesResponse>
                    ) {
                        p1.body()?.data?.let { data ->
                            Log.i("template", "$data")
                            streamsUrlTemplates.complete(data.templates)
                        }
                    }

                    override fun onFailure(p0: Call<StreamUrlTemplatesResponse>, p1: Throwable) {
                        Log.i("on failure", "${p1.localizedMessage}")
                    }
                })

            val stopTime = System.currentTimeMillis()
            Log.i("parsing time", "${stopTime - startTime} channels repository get streams url templates")

            streamsUrlTemplates.await()
        }
}