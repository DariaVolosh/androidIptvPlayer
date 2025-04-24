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
    token: String,
    setMediaUrl: (String) -> Unit,
    dvrRange: Pair<Long, Long>,
    // inject epg view model in this composable to not pass those lambdas
    setIsEpgListFocused: (Boolean) -> Unit,
    updateCurrentEpgList: () -> Unit,
    fetchEpg: (List<Pair<Int, Int>>) -> Unit,
) {

    // here probably we wil inject channels and epg view models to aggregate data
    Row(
        Modifier.fillMaxSize()
    ) {
        ChannelList(
            Modifier.fillMaxWidth(0.4f),
            token,
            setMediaUrl,
            setIsEpgListFocused,
            updateCurrentEpgList,
            fetchEpg,
        )

        EpgList(
            Modifier.fillMaxWidth(),
            dvrRange,
            isChannelClicked
        )
    }
}