package com.spearson.wakeapp.stopwatch.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun StopwatchRoot(
    viewModel: StopwatchViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    StopwatchScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}
