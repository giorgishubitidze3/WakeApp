package com.spearson.wakeapp.timer.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TimerRoot(
    viewModel: TimerViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onAction(TimerAction.OnScreenOpened)
    }

    TimerScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}
