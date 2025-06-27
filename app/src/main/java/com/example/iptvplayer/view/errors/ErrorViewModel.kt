package com.example.iptvplayer.view.errors

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iptvplayer.R
import com.example.iptvplayer.domain.errors.ErrorManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ErrorViewModel @Inject constructor(
    private val errorManager: ErrorManager
): ViewModel() {
    private val _currentError = MutableStateFlow(
        ErrorData("", "", R.drawable.error_icon)
    )
    val currentError: StateFlow<ErrorData> = _currentError

    init {
        viewModelScope.launch {
            errorManager.error.collect { error ->
                _currentError.value = error
                Log.i("collected error", error.toString())
            }
        }
    }

    fun publishError(error: ErrorData) {
        errorManager.publishError(error)
    }

    fun resetError() {
        errorManager.publishError(
            ErrorData("", "", R.drawable.error_icon)
        )
    }
}