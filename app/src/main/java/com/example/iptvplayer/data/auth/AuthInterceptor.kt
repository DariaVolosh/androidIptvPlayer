package com.example.iptvplayer.data.auth

import com.example.iptvplayer.domain.auth.AuthManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val authManager: AuthManager
): Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = authManager.retrieveToken()

        val requestBuilder = originalRequest.newBuilder()
        if (token.isNotEmpty()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        val request = requestBuilder.build()
        return chain.proceed(request)
    }
}