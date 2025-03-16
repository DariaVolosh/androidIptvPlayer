package com.example.iptvplayer.view.programDatePicker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramDatePickerModal(
    modifier: Modifier
) {
    val datePickerState = rememberDatePickerState()

    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.secondary.copy(0.8f))
            .padding(25.dp)
    ) {
        DatePicker(
            Modifier.fillMaxWidth(0.6f)
        )
        TimePicker()
    }
}