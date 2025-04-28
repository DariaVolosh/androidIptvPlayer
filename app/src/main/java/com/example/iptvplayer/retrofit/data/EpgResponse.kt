package com.example.iptvplayer.retrofit.data

data class EpgDataAndCurrentIndex(
    val data: List<Epg> = emptyList(),
    val currentEpgIndex: Int = 0,
    val liveEpgIndex: Int = 0
)

data class Epg(
    var epgVideoName: String = "", // epg screen name
    var epgVideoTimeRange: String = "", // this can be used in epg list rendering
    var start: String = "", // start of program (just data format from backend)
    var stop: String = "", // end of program
    // (this used for effective comparison, parsed from string during fetching in repository)
    var startSeconds: Long = 0, // start of program in seconds since epoch
    var stopSeconds: Long = 0, // end of program in seconds since epoch
    var length: Int = 0 // duration in minutes,
)

data class EpgData(
    var data: List<Epg>
)

data class EpgResponse(
    val data: EpgData
)
