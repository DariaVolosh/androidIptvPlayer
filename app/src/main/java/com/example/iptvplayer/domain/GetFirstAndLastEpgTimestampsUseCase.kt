package com.example.iptvplayer.domain

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.iptvplayer.data.repositories.EpgRepository
import javax.inject.Inject

class GetFirstAndLastEpgTimestampsUseCase @Inject constructor(
    private val epgRepository: EpgRepository
){
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getFirstAndLastEpgTimestamps(
        channelId: String,
        month: String
    ): Pair<Long, Long> {
        return epgRepository.getFirstAndLastEpgTimestamps(channelId, month)
    }
}