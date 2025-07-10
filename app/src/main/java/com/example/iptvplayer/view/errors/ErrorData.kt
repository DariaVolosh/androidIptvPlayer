package com.example.iptvplayer.view.errors

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class ErrorDismissButtonData(
    @StringRes val buttonText: Int,
    val onDismissCallback: () -> Unit
)

data class ErrorData(
    val errorTitle: String,
    val errorDescription: String,
    @DrawableRes val errorIcon: Int,
    val errorDismissButton: ErrorDismissButtonData? = null
)
