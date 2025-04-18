package com.example.iptvplayer.view.mainScreen

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.iptvplayer.view.channels.ChannelList
import com.example.iptvplayer.view.epg.EpgList

@Composable
fun ChannelsAndEpgRow(
    isChannelClicked: Boolean,
    setMediaUrl: (String) -> Unit,
    setIsEpgListFocused: (Boolean) -> Unit,
    getEpgById: (String, Pair<Long, Long>) -> Unit
) {

    // here probably we wil inject channels and epg view models to aggregate data
    Row(
        Modifier.fillMaxSize()
    ) {
        ChannelList(
            Modifier.fillMaxWidth(0.4f),
            setMediaUrl,
            setIsEpgListFocused,
            getEpgById
        )

        EpgList(
            Modifier.fillMaxWidth(),
            isChannelClicked
        )
    }
}