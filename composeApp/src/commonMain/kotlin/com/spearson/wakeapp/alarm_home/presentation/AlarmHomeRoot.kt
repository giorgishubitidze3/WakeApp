package com.spearson.wakeapp.alarm_home.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AlarmHomeRoot(
    onCreateAlarm: () -> Unit,
    viewModel: AlarmHomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onAction(AlarmHomeAction.OnScreenShown)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(state.shouldNavigateToCreateAlarm) {
        if (state.shouldNavigateToCreateAlarm) {
            onCreateAlarm()
            viewModel.onAction(AlarmHomeAction.OnCreateAlarmNavigationHandled)
        }
    }

    AlarmHomeScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}
