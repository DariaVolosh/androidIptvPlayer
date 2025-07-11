package com.example.iptvplayer.data.repositories

import com.example.iptvplayer.data.PlaylistChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface PlaylistRepository {
    fun parsePlaylistData(playlistContent: List<String>): List<PlaylistChannel>
    fun extractTsSegments(
        rootUrl: String,
        isLiveStream: Boolean,
        readFile: suspend (String) -> List<String>
    ): Flow<String>
}

class M3U8PlaylistRepository @Inject constructor(): PlaylistRepository {
    var emittedSegments = mutableSetOf<String>()

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

    fun extractBaseUrl(rootUrl: String): String {
        return rootUrl.substring(0, rootUrl.lastIndexOf("/") + 1)
    }

    fun constructUrl(vararg urlParts: String): String {
        return urlParts.joinToString("")
    }

    fun checkEmittedSegmentsBuffer() {
        if (emittedSegments.size >= 20) {
            var removedSegments = 0
            val newEmittedSegments = mutableSetOf<String>()

            for (segment in emittedSegments) {
                if (removedSegments < 10) {
                    removedSegments++
                } else {
                    newEmittedSegments.add(segment)
                }
            }
            emittedSegments = newEmittedSegments
        }
    }

    override fun extractTsSegments(
        rootUrl: String,
        isLiveStream: Boolean,
        readFile: suspend (String) -> List<String>
    ) = flow {
        suspend fun recursiveExtractTsSegments(rootUrl: String) {
            val fileContent = readFile(rootUrl)
            val baseUrl = extractBaseUrl(rootUrl)

            for (line in fileContent) {
                if (line.indexOf("m3u8") != -1) {
                    recursiveExtractTsSegments(constructUrl(baseUrl, line))
                } else {
                    if (line.indexOf(".ts") != -1) {
                        if (isLiveStream) {
                            val constructedUrl = constructUrl(baseUrl, line)
                            if (constructedUrl!in emittedSegments) {
                                emittedSegments.add(constructedUrl)
                                emit(constructedUrl)
                            }
                        } else {
                            emit(baseUrl + line)
                        }
                    }
                }
            }
        }

        recursiveExtractTsSegments(rootUrl)

        while (isLiveStream) {
            checkEmittedSegmentsBuffer()
            delay(4000)
            recursiveExtractTsSegments(rootUrl)
        }
    }
}