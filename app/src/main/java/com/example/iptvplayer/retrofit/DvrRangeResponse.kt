package com.example.iptvplayer.retrofit

data class DvrRange(
    val from: Long,
    val duration: Long
)

data class DvrRangeResponse(
    val ranges: List<DvrRange>
)