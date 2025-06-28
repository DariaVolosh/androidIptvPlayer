package com.example.iptvplayer.domain.media

import com.example.iptvplayer.data.repositories.MediaPlaybackRepository
import javax.inject.Inject

class GetMediaDataSourceUseCase @Inject constructor(
    private val mediaPlaybackRepository: MediaPlaybackRepository
) {
    fun getMediaDataSource()
        = mediaPlaybackRepository.getMediaDataSource()
}