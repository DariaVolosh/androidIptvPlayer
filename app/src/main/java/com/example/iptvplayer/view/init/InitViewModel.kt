package com.example.iptvplayer.view.init

import androidx.lifecycle.ViewModel
import com.example.iptvplayer.di.IoDispatcher
import com.example.iptvplayer.domain.auth.AuthOrchestrator
import com.example.iptvplayer.domain.auth.AuthState
import com.example.iptvplayer.domain.channels.ChannelsOrchestrator
import com.example.iptvplayer.domain.channels.ChannelsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AppState {
    INITIALIZING,
    INITIALIZED,
    ERROR
}

@HiltViewModel
class InitViewModel @Inject constructor(
    private val authOrchestrator: AuthOrchestrator,
    private val channelsOrchestrator: ChannelsOrchestrator,
    @IoDispatcher private val viewModelScope: CoroutineScope
): ViewModel() {

    private val _appState: MutableStateFlow<AppState> = MutableStateFlow(AppState.INITIALIZING)
    val appState: StateFlow<AppState> = _appState

    private val authState: StateFlow<AuthState> = authOrchestrator.authState.stateIn(
        viewModelScope, SharingStarted.Eagerly, AuthState.AUTHENTICATING
    )

    private val channelsState: StateFlow<ChannelsState> = channelsOrchestrator.channelsState.stateIn(
        viewModelScope, SharingStarted.Eagerly, ChannelsState.FETCHING
    )

    init {
        viewModelScope.launch {
            authOrchestrator.getBackendToken()
            authState.collect { authState ->
                when (authState) {
                    AuthState.AUTHENTICATING -> {

                    }

                    AuthState.AUTHENTICATED -> {
                        channelsOrchestrator.fetchChannelsData()
                    }

                    AuthState.ERROR -> updateAppState(AppState.ERROR)
                }
            }
        }

        viewModelScope.launch {
            channelsState.collect { channelsState ->
                when (channelsState) {
                    ChannelsState.FETCHING, ChannelsState.INITIALIZING -> {

                    }

                    ChannelsState.FETCHED -> {
                        updateAppState(AppState.INITIALIZED)
                    }

                    ChannelsState.ERROR -> updateAppState(AppState.ERROR)
                }
            }
        }
    }

    private fun updateAppState(updatedState: AppState) {
        _appState.value = updatedState
    }
}