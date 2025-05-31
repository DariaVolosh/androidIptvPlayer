package com.example.iptvplayer.view.errors

import androidx.annotation.DrawableRes

data class ErrorData(
    val errorTitle: String,
    val errorDescription: String,
    @DrawableRes val errorIcon: Int
)
