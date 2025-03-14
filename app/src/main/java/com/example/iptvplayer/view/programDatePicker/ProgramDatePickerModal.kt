package com.example.iptvplayer.view.programDatePicker

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramDatePickerModal() {
    val datePickerState = rememberDatePickerState()

    Row() {
        DatePicker(datePickerState)
    }
}