package com.example.iptvplayer.retrofit.data

import androidx.annotation.Keep

@Keep
data class DvrRange(
    val from: Long,
    val duration: Long
)

@Keep
data class DvrRangeResponse(
    val ranges: List<DvrRange>
)