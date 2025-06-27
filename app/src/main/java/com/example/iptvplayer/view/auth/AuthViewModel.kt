package com.example.iptvplayer.view.auth

import androidx.lifecycle.ViewModel
import com.example.iptvplayer.domain.auth.AuthOrchestrator
import com.example.iptvplayer.domain.auth.GetAuthTokenUseCase
import com.example.iptvplayer.domain.channels.ChannelsOrchestrator
import com.example.iptvplayer.domain.errors.ErrorManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val channelsOrchestrator: ChannelsOrchestrator,
    private val authOrchestrator: AuthOrchestrator,
    private val getAuthTokenUseCase: GetAuthTokenUseCase,
    private val errorManager: ErrorManager
): ViewModel() {

}