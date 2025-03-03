package com.example.iptvplayer.data

import androidx.compose.runtime.Immutable

@Immutable
data class PlaylistChannel(
    var id: String = "",
    var name: String = "",
    var logo: String = "",
    var url: String = ""
)