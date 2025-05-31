package com.example.iptvplayer.data.repositories

import android.util.Log
import com.example.iptvplayer.data.PlaylistChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface PlaylistRepository {
    fun parsePlaylistData(playlistContent: List<String>): List<PlaylistChannel>
    fun extractTsSegments(
        rootUrl: String,
        resetEmittedTsSegments: Boolean,
        readFile: suspend (String) -> List<String>
    ): Flow<String>
}

class M3U8PlaylistRepository @Inject constructor(): PlaylistRepository {
    val emittedSegments = mutableSetOf<String>()

    override fun parsePlaylistData(playlistContent: List<String>): List<PlaylistChannel> {
        val channelsData = mutableListOf<PlaylistChannel>()
        var currentChannelData = PlaylistChannel()

        for (line in playlistContent) {
            if (line.startsWith("#EXTM3U")) continue
            if (line.startsWith("#EXTINF")) {
                val nameStartIndex = line.indexOf("tvg-name") + 10
                val nameEndIndex = line.indexOf('"', nameStartIndex)

                val logoStartIndex = line.indexOf("tvg-logo") + 10
                val logoEndIndex = line.indexOf('"', logoStartIndex)

                val idStartIndex = line.indexOf("tvg-id") + 8
                val idEndIndex = line.indexOf('"', idStartIndex)

                currentChannelData.id = line.substring(idStartIndex, idEndIndex)
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
        resetEmittedTsSegments: Boolean,
        readFile: suspend (String) -> List<String>
    ) = flow {
        if (emittedSegments.size != 0) {
            Log.i("segments", "segments last segment ${emittedSegments.last()}")
        }
        Log.i("segments", "reset emitted ts segments: $resetEmittedTsSegments")
        if (resetEmittedTsSegments) emittedSegments.clear()

        Log.i("segments", "size after reset: ${emittedSegments.size}")

        suspend fun recursiveExtractTsSegments(rootUrl: String) {
            val fileContent = readFile(rootUrl)
            Log.i("rooturl", rootUrl)
            val baseUrl = rootUrl.substring(0, rootUrl.lastIndexOf("/") + 1)

            for (line in fileContent) {
                Log.i("loop line", line)
                if (line.indexOf("m3u8") != -1) {
                    val combinedUrl = baseUrl + line

                    recursiveExtractTsSegments(combinedUrl)
                } else {
                    if (line.indexOf(".ts") != -1) {
                        val noDvrLink = (baseUrl + line).replaceFirst("dvr-", "")
                        if (noDvrLink !in emittedSegments) {
                            emittedSegments.add(noDvrLink)
                            Log.i("segments", "emitted segment ${baseUrl + line}")
                            Log.i("segments", "segments quantity ${emittedSegments.size}")
                            emit(baseUrl + line)
                        }
                    }
                }
            }
        }

        recursiveExtractTsSegments(rootUrl)

        while (true) {
            recursiveExtractTsSegments(rootUrl)
            delay(4000)
        }
    }
}