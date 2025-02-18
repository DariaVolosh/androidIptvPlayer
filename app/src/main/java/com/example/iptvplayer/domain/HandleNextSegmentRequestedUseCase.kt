package com.example.iptvplayer.domain

import com.example.iptvplayer.data.MediaRepository
import javax.inject.Inject

class HandleNextSegmentRequestedUseCase @Inject constructor(
    private val mediaRepository: MediaRepository
) {
    fun setOnNextSegmentRequestedCallback(callback: () -> Unit) {
        mediaRepository.setOnNextSegmentRequestedCallback(callback)
    }
}