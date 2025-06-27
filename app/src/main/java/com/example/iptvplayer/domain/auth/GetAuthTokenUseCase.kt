package com.example.iptvplayer.domain.auth

import com.example.iptvplayer.data.repositories.AuthRepository
import okhttp3.RequestBody
import javax.inject.Inject

class GetAuthTokenUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend fun getAuthToken(
        requestBody: RequestBody,
        authErrorCallback: (String, String) -> Unit
    ): String {
        val token = "Bearer ${authRepository.getAuthToken(requestBody, authErrorCallback)}"
        return token
    }
}