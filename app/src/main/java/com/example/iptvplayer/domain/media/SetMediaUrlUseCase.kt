package com.example.iptvplayer.domain.media

import com.example.iptvplayer.data.repositories.MediaDataSource
import com.example.iptvplayer.data.repositories.MediaRepository
import javax.inject.Inject

class SetMediaUrlUseCase @Inject constructor(
    private val mediaRepository: MediaRepository
) {
    suspend fun setMediaUrl(url: String, onUrlSet: (MediaDataSource) -> Unit) {
        mediaRepository.setMediaUrl(url, onUrlSet)
    }
}