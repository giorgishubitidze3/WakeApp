package com.spearson.wakeapp.interval_alarm.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun IntervalAlarmRoot(
    planId: String?,
    onClose: () -> Unit,
    viewModel: IntervalAlarmViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(planId) {
        viewModel.onAction(IntervalAlarmAction.OnScreenOpened(planId))
    }

    LaunchedEffect(state.shouldCloseScreen) {
        if (state.shouldCloseScreen) {
            onClose()
            viewModel.onAction(IntervalAlarmAction.OnCloseHandled)
        }
    }

    IntervalAlarmScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}
