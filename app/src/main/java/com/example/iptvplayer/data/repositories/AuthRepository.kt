package com.example.iptvplayer.data.repositories

import android.content.Context
import com.example.iptvplayer.R
import com.example.iptvplayer.retrofit.services.AuthService
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.RequestBody
import javax.inject.Inject

class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authService: AuthService,
) {
    suspend fun getAuthToken(
        requestBody: RequestBody,
        authErrorCallback: (String, String) -> Unit
    ): String {
        val token = authService.getAuthToken(requestBody).data?.token

        if (token == null) {
            authErrorCallback(
                context.getString(R.string.auth_failed),
                context.getString(R.string.auth_failed_descr)
            )
        }

        return token ?: ""
    }
}