package com.example.iptvplayer.view

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iptvplayer.domain.GetAuthTokenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import okhttp3.MediaType.parse
import okhttp3.RequestBody.create
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val getAuthTokenUseCase: GetAuthTokenUseCase
): ViewModel() {
    private val _token: MutableLiveData<String> = MutableLiveData()
    var token: LiveData<String> = _token

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
            val mediaType = parse("application/json")
            val requestBody = create(mediaType, rawJsonString)
            Log.i("token called", "real")

            val token = getAuthTokenUseCase.getAuthToken(requestBody)
            if (token.isNotEmpty()) {
                _token.value = token
            }
        }
    }
}