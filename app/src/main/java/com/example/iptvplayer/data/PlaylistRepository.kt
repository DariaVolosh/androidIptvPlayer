package com.example.iptvplayer.data

import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface PlaylistRepository {
    fun getChannelsData(playlistContent: List<String>): List<PlaylistChannel>
    fun extractTsSegments(rootUrl: String, readFile: suspend (String) -> List<String>): Flow<String>
}

class M3U8PlaylistRepository @Inject constructor(): PlaylistRepository {
    override fun getChannelsData(playlistContent: List<String>): List<PlaylistChannel> {
        val channelsData = mutableListOf<PlaylistChannel>()
        var currentChannelData = PlaylistChannel()

        for (line in playlistContent) {
            if (line.startsWith("#EXTM3U")) continue
            if (line.startsWith("#EXTINF")) {
                val nameStartIndex = line.indexOf("tvg-name") + 10
                val nameEndIndex = line.indexOf('"', nameStartIndex)

                val logoStartIndex = line.indexOf("tvg-logo") + 10
                val logoEndIndex = line.indexOf('"', logoStartIndex)

                currentChannelData.name = line.substring(nameStartIndex, nameEndIndex)
                currentChannelData.logo = line.substring(logoStartIndex, logoEndIndex)
            } else {
                currentChannelData.url = line.substring(0, line.length)
                channelsData += currentChannelData
                currentChannelData = PlaylistChannel()
            }
        }

        return channelsData
    }

    override fun extractTsSegments(
        rootUrl: String,
        readFile: suspend (String) -> List<String>
    ) = flow {
        val emittedSegments = mutableSetOf<String>()

        suspend fun recursiveExtractTsSegments(rootUrl: String) {
            val fileContent = readFile(rootUrl)
            Log.i("content", fileContent.toString())
            val baseUrl = rootUrl.substring(0, rootUrl.lastIndexOf("/") + 1)

            for (line in fileContent) {
                if (line.indexOf("m3u8") != -1) {
                    val combinedUrl = baseUrl + line

                    recursiveExtractTsSegments(combinedUrl)
                }

                if (line.indexOf(".ts") != -1) {
                    if ((baseUrl + line !in emittedSegments)) {
                        Log.i("emission", "emitted ${baseUrl + line}")
                        emittedSegments.add(baseUrl + line)
                        emit(baseUrl + line)
                    }
                }
            }
        }

        while (true) {
            recursiveExtractTsSegments(rootUrl)
            delay(4000)
        }
    }
}