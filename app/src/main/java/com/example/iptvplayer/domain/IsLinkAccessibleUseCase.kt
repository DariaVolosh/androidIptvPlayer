package com.example.iptvplayer.domain

import com.example.iptvplayer.data.repositories.FileUtilsRepository
import javax.inject.Inject

class IsLinkAccessibleUseCase @Inject constructor(
    private val fileUtilsRepository: FileUtilsRepository
) {
    suspend fun isLinkAccessible(url: String) = fileUtilsRepository.isLinkAccessible(url)
}