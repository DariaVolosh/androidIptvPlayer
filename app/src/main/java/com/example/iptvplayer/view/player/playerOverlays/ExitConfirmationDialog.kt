package com.example.iptvplayer.view.player.playerOverlays

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.tv.material3.Text
import com.example.iptvplayer.R

@Composable
fun ExitConfirmationDialog(
    stayInsideApp: () -> Unit,
    exitApp: () -> Unit
) {
    var isStayButtonFocused by remember { mutableStateOf(true) }

    val stayButtonFocusRequester = remember { FocusRequester() }
    val exitButtonFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isStayButtonFocused) {
        if (!isStayButtonFocused) {
            exitButtonFocusRequester.requestFocus()
        } else {
            stayButtonFocusRequester.requestFocus()
        }
    }

    Box(
        modifier =  Modifier
            .background(color = Color.Black.copy(0.8f))
            .fillMaxSize()
            .zIndex(99f)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Text(
                text = stringResource(R.string.app_exit_confirmation),
                color = MaterialTheme.colorScheme.onSecondary,
                fontSize = 22.sp
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    modifier = Modifier
                        .focusable()
                        .focusRequester(stayButtonFocusRequester)
                        .onKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown) {
                                if (event.key == Key.DirectionRight) {
                                    isStayButtonFocused = false
                                }
                            }

                            true
                        },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSecondary),
                    onClick = stayInsideApp,
                    colors = ButtonDefaults.buttonColors(
                        containerColor =
                        if (isStayButtonFocused) MaterialTheme.colorScheme.primary
                        else Color.Transparent
                    )
                ) {
                    Text(
                        text = stringResource(R.string.app_stay_label),
                        color = MaterialTheme.colorScheme.onSecondary,
                        fontSize = 19.sp
                    )
                }

                Button(
                    modifier = Modifier
                        .focusable()
                        .focusRequester(exitButtonFocusRequester)
                        .onKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown) {
                                if (event.key == Key.DirectionLeft) {
                                    isStayButtonFocused = true
                                }
                            }

                            true
                        },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSecondary),
                    onClick = exitApp,
                    colors = ButtonDefaults.buttonColors(
                        containerColor =
                        if (!isStayButtonFocused) MaterialTheme.colorScheme.primary
                        else Color.Transparent
                    )
                ) {
                    Text(
                        text = stringResource(R.string.app_exit_label),
                        color = MaterialTheme.colorScheme.onSecondary,
                        fontSize = 19.sp
                    )
                }
            }
        }
    }
}