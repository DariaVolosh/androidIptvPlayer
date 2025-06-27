package com.example.iptvplayer.data.media

import com.example.iptvplayer.data.repositories.FileUtilsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TsExtractor @Inject constructor(
    private val fileUtilsRepository: FileUtilsRepository
) {

    fun extractBaseUrl(rootUrl: String): String {
        return rootUrl.substring(0, rootUrl.lastIndexOf("/") + 1)
    }

    fun constructUrl(vararg urlParts: String): String {
        return urlParts.joinToString("")
    }

    suspend fun extractTsSegmentUrls(url: String): List<String> {
        val fileContent = fileUtilsRepository.readFile(url) { title, description -> }
        val baseUrl = extractBaseUrl(url)

        val tsSegments = mutableListOf<String>()

        for (line in fileContent) {
            if (line.contains(".ts")) {
                tsSegments.add(constructUrl(baseUrl, line))
            }
        }

        return tsSegments
    }

    suspend fun extractNestedPlaylistUrls(url: String): List<String> {
        val fileContent = fileUtilsRepository.readFile(url) { title, description -> }
        val baseUrl = extractBaseUrl(url)

        val nestedPlaylistUrls = mutableListOf<String>()

        for (line in fileContent) {
            if (line.contains(".m3u8")) {
                nestedPlaylistUrls.add(constructUrl(baseUrl, line))
            }
        }

        return nestedPlaylistUrls
    }
}