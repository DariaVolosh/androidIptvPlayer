package com.example.iptvplayer.data.media

import com.example.iptvplayer.R
import com.example.iptvplayer.data.repositories.FileUtilsRepository
import com.example.iptvplayer.domain.errors.ErrorManager
import com.example.iptvplayer.view.errors.ErrorData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TsExtractor @Inject constructor(
    private val fileUtilsRepository: FileUtilsRepository,
    private val errorManager: ErrorManager
) {

    fun extractBaseUrl(rootUrl: String): String {
        return rootUrl.substring(0, rootUrl.lastIndexOf("/") + 1)
    }

    fun constructUrl(vararg urlParts: String): String {
        return urlParts.joinToString("")
    }

    suspend fun extractTsSegmentUrls(url: String): List<String> {
        val fileContent = fileUtilsRepository.readFile(url) { title, description ->
            errorManager.publishError(
                ErrorData(
                    errorTitle = title,
                    errorDescription =  description,
                    errorIcon = R.drawable.error_icon
                )
            )
        }

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