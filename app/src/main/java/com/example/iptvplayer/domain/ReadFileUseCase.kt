package com.example.iptvplayer.domain

import com.example.iptvplayer.data.FileUtilsRepository
import javax.inject.Inject

class ReadFileUseCase @Inject constructor(
    private val fileUtilsRepository: FileUtilsRepository
) {
    suspend fun readFile(playlistUrl: String) = fileUtilsRepository.readFile(playlistUrl)
}