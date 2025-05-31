package com.example.iptvplayer.view.player.playerOverlays

import android.util.Log
import androidx.compose.runtime.Composable
import com.example.iptvplayer.view.errors.ErrorData

enum class PlayerOverlayState {
    SHOW_ERROR,
    SHOW_LOADING_PROGRESS_BAR,
    SHOW_STREAM_REWIND_FRAME,
    SHOW_EXIT_CONFIRMATION
}

data class ExitConfirmationData(
    val stayInsideApp: () -> Unit,
    val exitApp: () -> Unit
)

data class StreamRewindData(
    val channelName: String,
    val currentTime: Long
)

@Composable
fun PlayerOverlay(
    currentOverlayState: PlayerOverlayState,
    exitConfirmationData: ExitConfirmationData,
    streamRewindData: StreamRewindData,
    errorData: ErrorData
) {

    Log.i("player overlay state", "current state $currentOverlayState")

    when (currentOverlayState) {
        PlayerOverlayState.SHOW_ERROR -> {
            ErrorPopup(errorData)
        }

        PlayerOverlayState.SHOW_LOADING_PROGRESS_BAR -> {
            LoadingPopup()
        }

        PlayerOverlayState.SHOW_STREAM_REWIND_FRAME -> {
            StreamRewindFrame(
                streamRewindData.channelName,
                streamRewindData.currentTime
            )
        }

        PlayerOverlayState.SHOW_EXIT_CONFIRMATION -> {
            ExitConfirmationDialog(
                exitConfirmationData.stayInsideApp,
                exitConfirmationData.exitApp,
            )
        }
    }
}