package com.spearson.wakeapp.stopwatch.presentation

data class StopwatchState(
    val elapsedMillis: Long = 0L,
    val isRunning: Boolean = false,
    val laps: List<StopwatchLap> = emptyList(),
)

data class StopwatchLap(
    val lapNumber: Int,
    val durationMillis: Long,
)
