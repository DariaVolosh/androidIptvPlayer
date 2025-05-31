package com.example.iptvplayer.view

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iptvplayer.R
import com.example.iptvplayer.domain.auth.GetAuthTokenUseCase
import com.example.iptvplayer.view.errors.ErrorData
import com.example.iptvplayer.view.errors.ErrorManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.parse
import okhttp3.RequestBody.create
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val getAuthTokenUseCase: GetAuthTokenUseCase,
    private val errorManager: ErrorManager
): ViewModel() {
    private val _token: MutableStateFlow<String> = MutableStateFlow("")
    var token: StateFlow<String> = _token

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

    fun getBackendToken() {
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val mediaType = parse("application/json")
            val requestBody = create(mediaType, rawJsonString)
            Log.i("token called", "real")

            val token = getAuthTokenUseCase.getAuthToken(
                requestBody
            ) { title, description ->
                errorManager.publishError(
                    ErrorData(title, description, R.drawable.error_icon)
                )
            }
            _token.value = token

            val stopTime = System.currentTimeMillis()
            Log.i("parsing time", "${stopTime - startTime} auth view model get backend token")
        }
    }
}