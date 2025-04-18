package com.example.iptvplayer.domain

import com.example.iptvplayer.data.repositories.M3U8PlaylistRepository
import javax.inject.Inject

class GetPlaylistDataUseCase @Inject constructor(
    private val playlistRepository: M3U8PlaylistRepository
) {
    fun getPlaylistData(playlistContent: List<String>) = playlistRepository.parsePlaylistData(playlistContent)
}