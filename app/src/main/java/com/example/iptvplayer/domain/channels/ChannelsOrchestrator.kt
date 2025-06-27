package com.example.iptvplayer.domain.channels

import com.example.iptvplayer.R
import com.example.iptvplayer.di.IoDispatcher
import com.example.iptvplayer.domain.errors.ErrorManager
import com.example.iptvplayer.domain.sharedPrefs.SharedPreferencesUseCase
import com.example.iptvplayer.retrofit.data.ChannelData
import com.example.iptvplayer.view.channels.CURRENT_CHANNEL_INDEX_KEY
import com.example.iptvplayer.view.errors.ErrorData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

enum class ChannelsState {
    INITIALIZING,
    FETCHING,
    FETCHED,
    ERROR
}

@Singleton
class ChannelsOrchestrator @Inject constructor(
    private var channelsManager: ChannelsManager,
    private var sharedPreferencesUseCase: SharedPreferencesUseCase,
    private var errorManager: ErrorManager,
    @IoDispatcher private val orchestratorScope: CoroutineScope
) {
    val channelsData: StateFlow<List<ChannelData>> = channelsManager.channelsData.stateIn(
        orchestratorScope, SharingStarted.Eagerly, emptyList()
    )

    val currentChannelIndex: StateFlow<Int> = channelsManager.currentChannelIndex.stateIn(
        orchestratorScope, SharingStarted.Eagerly, -1
    )

    private val _channelsState: MutableStateFlow<ChannelsState> = MutableStateFlow(ChannelsState.INITIALIZING)
    val channelsState: StateFlow<ChannelsState> = _channelsState

    init {
        orchestratorScope.launch {
            channelsData.filter {
                channelsData -> channelsData.isNotEmpty()
            }.first().let { _ ->
                val cachedChannelIndex = getCachedChannelIndex()
                channelsManager.updateChannelIndex(cachedChannelIndex, true)
                channelsManager.updateChannelIndex(cachedChannelIndex, false)
            }
        }

        orchestratorScope.launch {
            currentChannelIndex.collect { currentChannelIndex ->
                if (currentChannelIndex != -1) {
                    sharedPreferencesUseCase.saveIntValue(CURRENT_CHANNEL_INDEX_KEY, currentChannelIndex)
                }
            }
        }
    }

    fun updateChannelsData(channelsData: List<ChannelData>) {
        channelsManager.updateChannelsData(channelsData)
    }

    fun updateChannelIndex(index: Int, isCurrent: Boolean) {
        channelsManager.updateChannelIndex(index, isCurrent)
    }

    fun getCachedChannelIndex(): Int {
        val cachedChannelIndex = sharedPreferencesUseCase.getIntValue(CURRENT_CHANNEL_INDEX_KEY)
        return if (cachedChannelIndex == -1) 0 else cachedChannelIndex
    }

    fun fetchChannelsData() {
        orchestratorScope.launch {
            updateChannelsState(ChannelsState.FETCHING)
            val data = channelsManager.getChannelsData { title, description ->
                errorManager.publishError(
                    ErrorData(title, description, R.drawable.error_icon)
                )
            }

            if (data.isNotEmpty()) updateChannelsState(ChannelsState.FETCHED)
            else updateChannelsState(ChannelsState.ERROR)

            updateChannelsData(data)
        }
    }

    private fun updateChannelsState(updatedState: ChannelsState) {
        _channelsState.value = updatedState
    }
}