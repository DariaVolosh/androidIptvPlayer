package com.example.iptvplayer.domain

import com.example.iptvplayer.data.MediaRepository
import javax.inject.Inject

class GetMediaDataSourceUseCase @Inject constructor(
    private val mediaRepository: MediaRepository
) {
    fun getMediaDataSource()
        = mediaRepository.getMediaDataSource()
}