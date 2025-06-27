package com.example.iptvplayer.retrofit.data

import androidx.annotation.Keep

@Keep
data class StreamUrlTemplate(
    val template: String
)

@Keep
data class StreamUrlTemplates(
    val templates: List<StreamUrlTemplate>
)

@Keep
data class StreamUrlTemplatesResponse(
    val data: StreamUrlTemplates?
)