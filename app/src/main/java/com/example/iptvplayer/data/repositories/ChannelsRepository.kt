package com.example.iptvplayer.data.repositories

import android.content.Context
import com.example.iptvplayer.R
import com.example.iptvplayer.data.BACKEND_BASE_URL
import com.example.iptvplayer.retrofit.data.ChannelData
import com.example.iptvplayer.retrofit.data.StreamUrlTemplate
import com.example.iptvplayer.retrofit.services.ChannelsAndEpgService
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.parse
import okhttp3.RequestBody.create
import javax.inject.Inject
import javax.net.ssl.SSLHandshakeException

class ChannelsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val channelsAndEpgService: ChannelsAndEpgService
) {
    suspend fun parseChannelsData(
        streamUrlTemplate: String,
        channelsErrorCallback: (String, String) -> Unit
    ): List<ChannelData> {
        var channelsData = emptyList<ChannelData>()

        try {
            val data = channelsAndEpgService.getChannelsInfo().data

            if (data == null) {
                channelsErrorCallback(
                    context.getString(R.string.no_channels),
                    context.getString(R.string.no_channels_descr)
                )
            } else {
                val mappedChannelData = mutableListOf<ChannelData>()

                for (channelData in data) {
                    val channel = channelData.channel[0]
                    val replacedSlashesUrl = channelData.channel[0].logo.replace('\\', '/')
                    val constructedLogoUrl = BACKEND_BASE_URL + replacedSlashesUrl
                    channel.logo = constructedLogoUrl

                    val channelUrl = streamUrlTemplate.replace("[channelname]", channel.name)
                    channel.channelUrl = channelUrl
                    mappedChannelData.add(channel)
                }

                channelsData = mappedChannelData
            }
        } catch (e: Exception) {
            channelsErrorCallback(
                context.getString(R.string.no_channels),
                context.getString(R.string.no_channels_descr)
            )
        }

        return channelsData
    }

    suspend fun getStreamsUrlTemplates(
        channelsErrorCallback: (String, String) -> Unit
    ): List<StreamUrlTemplate> {
        var streamsUrlTemplates = emptyList<StreamUrlTemplate>()

        val mediaType = parse("application/json")
        val requestBody = create(mediaType, "{}")

        try {
            val templates = channelsAndEpgService.getStreamsUrlTemplates(requestBody).data?.templates

            if (templates == null) {
                channelsErrorCallback(
                    context.getString(R.string.no_channels),
                    context.getString(R.string.no_channels_descr)
                )
            } else {
                streamsUrlTemplates = templates
            }
        } catch (e: Exception) {
            if (e is SSLHandshakeException) {
                channelsErrorCallback(
                    context.getString(R.string.no_channels),
                    context.getString(R.string.timezone_misconfig_description)
                )
            } else {
                channelsErrorCallback(
                    context.getString(R.string.no_channels),
                    context.getString(R.string.no_channels_descr)
                )
            }
        }

        return streamsUrlTemplates
    }
}