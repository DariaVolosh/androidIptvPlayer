package com.example.iptvplayer.domain

import com.example.iptvplayer.data.repositories.AuthRepository
import okhttp3.RequestBody
import javax.inject.Inject

class GetAuthTokenUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend fun getAuthToken(requestBody: RequestBody) = "Bearer ${authRepository.getAuthToken(requestBody)}"
}