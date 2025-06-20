package com.example.iptvplayer.domain.archive

import com.example.iptvplayer.data.repositories.FlussonicRepository
import javax.inject.Inject

class GetDvrRangesUseCase @Inject constructor(
    private val flussonicRepository: FlussonicRepository
) {
    suspend fun getDvrRanges(streamName: String) =
        flussonicRepository.getDvrRanges(streamName)
}