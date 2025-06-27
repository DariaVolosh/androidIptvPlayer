package com.example.iptvplayer.view.channelInfo.playbackControls

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iptvplayer.R
import com.example.iptvplayer.domain.archive.ArchiveManager
import com.example.iptvplayer.domain.channels.ChannelsManager
import com.example.iptvplayer.domain.epg.EpgManager
import com.example.iptvplayer.domain.media.MediaPlaybackOrchestrator
import com.example.iptvplayer.domain.time.TimeOrchestrator
import com.example.iptvplayer.retrofit.data.ChannelData
import com.example.iptvplayer.retrofit.data.EpgListItem
import com.example.iptvplayer.view.channelsAndEpgRow.CurrentDvrInfoState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaybackControlsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val epgManager: EpgManager,
    private val mediaPlaybackOrchestrator: MediaPlaybackOrchestrator,
    private val archiveManager: ArchiveManager,
    private val channelsManager: ChannelsManager,
    private val timeOrchestrator: TimeOrchestrator
): ViewModel() {

    val currentTime: StateFlow<Long> = timeOrchestrator.currentTime.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0L
    )

    val currentChannel: StateFlow<ChannelData> = channelsManager.currentChannel.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), ChannelData()
    )

    val currentEpgIndex: StateFlow<Int> = epgManager.currentEpgIndex.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), -1
    )

    val currentDvrInfoState: StateFlow<CurrentDvrInfoState> = archiveManager.currentChannelDvrInfoState.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), CurrentDvrInfoState.LOADING
    )

    // next program button
    fun handleNextProgramClick()  {
        //Log.i("FOCUSED", focusedEpgIndex.toString())
        //resetSecondsNotInteracted()
        val nextEpgIndex = epgManager.findFirstEpgIndexForward(currentEpgIndex.value + 1)
        val nextItem = epgManager.getEpgItemByIndex(nextEpgIndex)
        val isDvrAvailable =
                currentDvrInfoState.value != CurrentDvrInfoState.LOADING &&
                currentDvrInfoState.value != CurrentDvrInfoState.NOT_AVAILABLE_GLOBAL
        //Log.i("next program", nextItem.toString())


        // CHECK IF DVR IS AVAILABLE
        if (isDvrAvailable && nextItem != null && nextItem is EpgListItem.Epg)
        {
            viewModelScope.launch {
                mediaPlaybackOrchestrator.updateIsSeeking(true)
                mediaPlaybackOrchestrator.updateIsLive(false)
                timeOrchestrator.updateCurrentTime(nextItem.epgVideoTimeRangeSeconds.start)
                mediaPlaybackOrchestrator.resetPlayer()
                archiveManager.getArchiveUrl(currentChannel.value.channelUrl, currentTime.value)
                epgManager.updateCurrentEpgIndex(nextEpgIndex)
                epgManager.updateFocusedEpgIndex(nextEpgIndex)
                mediaPlaybackOrchestrator.updateIsSeeking(false)
            }
        } else {
            archiveManager.setRewindError(context.getString(R.string.no_next_program))
        }
    }
}