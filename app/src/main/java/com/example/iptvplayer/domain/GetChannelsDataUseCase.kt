package com.example.iptvplayer.domain

import com.example.iptvplayer.data.repositories.M3U8PlaylistRepository
import javax.inject.Inject

class GetChannelsDataUseCase @Inject constructor(
    private val playlistRepository: M3U8PlaylistRepository
) {
    fun getChannelsData(playlistContent: List<String>) = playlistRepository.getChannelsData(playlistContent)
}