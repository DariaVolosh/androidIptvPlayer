package com.example.iptvplayer.domain.auth

import com.example.iptvplayer.R
import com.example.iptvplayer.di.IoDispatcher
import com.example.iptvplayer.domain.errors.ErrorManager
import com.example.iptvplayer.view.errors.ErrorData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.parse
import okhttp3.RequestBody.create
import javax.inject.Inject
import javax.inject.Singleton

enum class AuthState {
    AUTHENTICATING,
    AUTHENTICATED,
    ERROR
}

@Singleton
class AuthOrchestrator @Inject constructor(
    private val errorManager: ErrorManager,
    private val authManager: AuthManager,
    @IoDispatcher private val orchestratorScope: CoroutineScope
) {

    private val _authState: MutableStateFlow<AuthState> = MutableStateFlow(AuthState.AUTHENTICATING)
    val authState: StateFlow<AuthState> = _authState

    private val rawJsonString = """
        {
            "subscriberId": "000000003",
            "password": "Password@123",
            "deviceId": "string",
            "deviceModel": "string",
            "androidApiVersion": "string",
            "appVersion": "string"
        }
    """

    fun updateAuthState(updatedState: AuthState) {
        _authState.value = updatedState
    }

    fun getBackendToken() {
        orchestratorScope.launch {
            print("called get backend token")
            val mediaType = parse("application/json")
            val requestBody = create(mediaType, rawJsonString)

            val token = authManager.getAuthToken(
                requestBody
            ) { title, description ->
                errorManager.publishError(
                    ErrorData(title, description, R.drawable.error_icon)
                )
            }

            if (token.isNotEmpty()) {
                authManager.updateToken(token)
                updateAuthState(AuthState.AUTHENTICATED)
            } else {
                updateAuthState(AuthState.ERROR)
            }
        }
    }
}