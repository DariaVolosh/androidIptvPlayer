package com.example.iptvplayer.domain

import com.example.iptvplayer.data.MediaDataSource
import com.example.iptvplayer.data.MediaRepository
import javax.inject.Inject

class SetMediaUrlUseCase @Inject constructor(
    private val mediaRepository: MediaRepository
) {
    suspend fun setMediaUrl(url: String, onUrlSet: (MediaDataSource) -> Unit) {
        mediaRepository.setMediaUrl(url, onUrlSet)
    }
}