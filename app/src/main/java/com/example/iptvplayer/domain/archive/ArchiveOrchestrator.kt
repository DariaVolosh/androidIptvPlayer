package com.example.iptvplayer.domain.archive

import com.example.iptvplayer.di.IoDispatcher
import com.example.iptvplayer.domain.time.TimeManager
import com.example.iptvplayer.retrofit.data.DvrRange
import com.example.iptvplayer.view.archive.CurrentDvrInfoState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArchiveOrchestrator @Inject constructor(
    private val timeManager: TimeManager,
    private val archiveManager: ArchiveManager,
    @IoDispatcher private val orchestratorScope: CoroutineScope
) {
    private val currentChannelDvrRanges: StateFlow<List<DvrRange>> =
        archiveManager.currentChannelDvrRanges.stateIn(
            orchestratorScope, SharingStarted.Eagerly, emptyList()
        )

    private val currentChannelDvrInfoState: StateFlow<CurrentDvrInfoState> =
        archiveManager.currentChannelDvrInfoState.stateIn(
            orchestratorScope, SharingStarted.Eagerly, CurrentDvrInfoState.LOADING
        )

    private val focusedChannelDvrRanges: StateFlow<List<DvrRange>> =
        archiveManager.focusedChannelDvrRanges.stateIn(
            orchestratorScope, SharingStarted.Eagerly, emptyList()
        )

    private val focusedChannelDvrInfoState: StateFlow<CurrentDvrInfoState> =
        archiveManager.focusedChannelDvrInfoState.stateIn(
            orchestratorScope, SharingStarted.Eagerly, CurrentDvrInfoState.LOADING
        )

    suspend fun determineCurrentDvrRange(isCurrentChannel: Boolean, currentTime: Long) {
        println("determine $isCurrentChannel")
        var currentRanges = if (isCurrentChannel) currentChannelDvrRanges else focusedChannelDvrRanges
        var currentState = if (isCurrentChannel) currentChannelDvrInfoState else focusedChannelDvrInfoState
        if (currentState.value != CurrentDvrInfoState.LOADING) {
            for (i in currentRanges.value.indices) {
                println("range i $i")
                val range = currentRanges.value[i]

                if (currentTime >= range.from) {
                    if (currentTime <= range.from + range.duration) {
                        archiveManager.updateChannelCurrentDvrRange(isCurrentChannel, i)
                        archiveManager.updateDvrInfoState(
                            isCurrentChannel,
                            CurrentDvrInfoState.PLAYING_IN_RANGE
                        )
                        return
                    } else {
                        if (i+1 < currentRanges.value.size) {
                            val nextRange = currentRanges.value[i+1]

                            if (currentTime <= nextRange.from) {
                                timeManager.updateCurrentTime(nextRange.from)
                                archiveManager.getArchiveUrl(archiveManager.archiveSegmentUrl.value, nextRange.from)
                                archiveManager.updateChannelCurrentDvrRange(isCurrentChannel, -1)
                                archiveManager.updateDvrInfoState(
                                    isCurrentChannel,
                                    CurrentDvrInfoState.GAP_DETECTED_AND_WAITING
                                )
                                return
                            }
                        } else {
                            archiveManager.updateDvrInfoState(
                                isCurrentChannel,
                                CurrentDvrInfoState.END_OF_DVR_REACHED
                            )
                        }
                    }
                }
            }
        }
    }
}