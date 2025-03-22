package com.example.iptvplayer.domain

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.iptvplayer.data.repositories.EpgRepository
import javax.inject.Inject

class GetEpgMonthUseCase @Inject constructor(
    private val epgRepository: EpgRepository
) {
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getEpgMonth(channelId: String) =
        epgRepository.getEpgMonth(channelId)
}