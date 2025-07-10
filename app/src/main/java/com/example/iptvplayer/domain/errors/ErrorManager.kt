package com.example.iptvplayer.domain.errors

import android.util.Log
import com.example.iptvplayer.R
import com.example.iptvplayer.view.errors.ErrorData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ErrorManager @Inject constructor(

) {
    private val _error: MutableStateFlow<ErrorData> = MutableStateFlow(
        ErrorData("", "", R.drawable.error_icon)
    )
    val error: StateFlow<ErrorData> = _error

    fun publishError(error: ErrorData) {
        Log.i("published error", error.toString())
        _error.value = error
    }

    fun resetError() {
        _error.value = ErrorData("", "", R.drawable.error_icon)
    }
}