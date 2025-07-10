package com.example.iptvplayer.view.errors

import androidx.lifecycle.ViewModel
import com.example.iptvplayer.R
import com.example.iptvplayer.di.IoDispatcher
import com.example.iptvplayer.domain.errors.ErrorManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ErrorViewModel @Inject constructor(
    private val errorManager: ErrorManager,
    @IoDispatcher private val viewModelScope: CoroutineScope
): ViewModel() {

    val currentError: StateFlow<ErrorData> = errorManager.error.stateIn(
        viewModelScope, SharingStarted.Eagerly, ErrorData("", "", R.drawable.error_icon)
    )

    fun publishError(error: ErrorData) {
        errorManager.publishError(error)
    }

    fun resetError() {
        errorManager.publishError(
            ErrorData("", "", R.drawable.error_icon)
        )
    }
}