package com.example.iptvplayer.retrofit.data

data class StreamUrlTemplate(
    val template: String
)

data class StreamUrlTemplates(
    val templates: List<StreamUrlTemplate>
)

data class StreamUrlTemplatesResponse(
    val data: StreamUrlTemplates
)