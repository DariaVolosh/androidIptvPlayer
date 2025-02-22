package com.example.iptvplayer.view.epg

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.example.iptvplayer.room.Epg

@Composable
fun EpgList(
    modifier: Modifier,
    epg: List<Epg>,
) {

    LaunchedEffect(epg) {
        Log.i("EPGLIST", epg.toString() )
    }

    LazyColumn(
        modifier = modifier
            .background(MaterialTheme.colorScheme.secondary.copy(0.4f))
    ) {

    }
}