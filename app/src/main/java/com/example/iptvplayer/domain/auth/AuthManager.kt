package com.example.iptvplayer.domain.auth

import com.example.iptvplayer.data.repositories.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.RequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor(
    private val authRepository: AuthRepository
) {
    private val _token: MutableStateFlow<String> = MutableStateFlow("")
    var token: StateFlow<String> = _token

    suspend fun getAuthToken(
        requestBody: RequestBody,
        authErrorCallback: (String, String) -> Unit
    ): String {
        return authRepository.getAuthToken(requestBody, authErrorCallback)
    }

    fun updateToken(token: String) {
        _token.value = token
    }

    fun retrieveToken(): String {
        return _token.value
    }
}