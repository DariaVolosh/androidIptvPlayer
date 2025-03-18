package com.example.iptvplayer.domain

import com.example.iptvplayer.data.repositories.FlussonicRepository
import javax.inject.Inject

class GetDvrRangeUseCase @Inject constructor(
    private val flussonicRepository: FlussonicRepository
) {
    suspend fun getDvrRange(streamName: String) =
        flussonicRepository.getDvrRange(streamName)
}