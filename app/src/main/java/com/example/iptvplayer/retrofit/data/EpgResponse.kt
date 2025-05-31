package com.example.iptvplayer.retrofit.data

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

sealed class EpgListItem {
    @Immutable
    @Keep
    data class Epg(
        var epgVideoName: String = "", // epg screen name
        var epgVideoTimeRange: String = "", // this can be used in epg list rendering
        var start: String = "", // start of program (just data format from backend)
        var stop: String = "", // end of program
        // (this used for effective comparison, parsed from string during fetching in repository)
        var epgVideoTimeRangeSeconds: EpgTimeRangeInSeconds = EpgTimeRangeInSeconds(),
        var length: Int = 0, // duration in minutes
        val uniqueId: String = java.util.UUID.randomUUID().toString()
    ): EpgListItem()

    @Immutable
    @Keep
    data class Header(
        var title: String = ""
    ): EpgListItem()
}

@Keep
data class EpgTimeRangeInSeconds(
    val start: Long = 0,
    val stop: Long = 0
)

@Keep
data class EpgData(
    var data: List<EpgListItem.Epg>
)

@Keep
data class EpgResponse(
    val data: EpgData
)
