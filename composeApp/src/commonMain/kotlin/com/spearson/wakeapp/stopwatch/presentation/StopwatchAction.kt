package com.spearson.wakeapp.stopwatch.presentation

sealed interface StopwatchAction {
    data object OnStartClick : StopwatchAction
    data object OnPauseClick : StopwatchAction
    data object OnContinueClick : StopwatchAction
    data object OnStopClick : StopwatchAction
    data object OnLapClick : StopwatchAction
}
