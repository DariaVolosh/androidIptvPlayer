package com.example.iptvplayer.domain

import com.example.iptvplayer.data.repositories.FileUtilsRepository
import com.example.iptvplayer.data.repositories.M3U8PlaylistRepository
import javax.inject.Inject

class GetTsSegmentsUseCase @Inject constructor(
    private val playlistRepository: M3U8PlaylistRepository,
    private val fileUtilsRepository: FileUtilsRepository
) {
   fun extractTsSegments(url: String, isLive: Boolean) =
        playlistRepository.extractTsSegments(
            url,
            isLive
        ) {url -> fileUtilsRepository.readFile(url)}
}