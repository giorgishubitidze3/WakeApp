package com.spearson.wakeapp.stopwatch.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.TimeSource

class StopwatchViewModel : ViewModel() {

    private var hasInitialized = false
    private var tickerJob: Job? = null
    private var runStartMark: kotlin.time.TimeMark? = null
    private var accumulatedElapsedMillis: Long = 0L

    private val _state = MutableStateFlow(StopwatchState())
    val state = _state
        .onStart {
            if (!hasInitialized) {
                hasInitialized = true
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = StopwatchState(),
        )

    fun onAction(action: StopwatchAction) {
        when (action) {
            StopwatchAction.OnStartClick -> start()
            StopwatchAction.OnPauseClick -> pause()
            StopwatchAction.OnContinueClick -> resume()
            StopwatchAction.OnStopClick -> stop()
            StopwatchAction.OnLapClick -> addLap()
        }
    }

    private fun start() {
        if (_state.value.isRunning) return
        accumulatedElapsedMillis = 0L
        runStartMark = TimeSource.Monotonic.markNow()
        _state.update {
            it.copy(
                isRunning = true,
                elapsedMillis = 0L,
                laps = emptyList(),
            )
        }
        startTicker()
    }

    private fun pause() {
        if (!_state.value.isRunning) return
        accumulatedElapsedMillis = currentElapsedMillis()
        runStartMark = null
        stopTicker()
        _state.update {
            it.copy(
                isRunning = false,
                elapsedMillis = accumulatedElapsedMillis,
            )
        }
    }

    private fun resume() {
        if (_state.value.isRunning) return
        if (_state.value.elapsedMillis <= 0L) return
        runStartMark = TimeSource.Monotonic.markNow()
        _state.update { it.copy(isRunning = true) }
        startTicker()
    }

    private fun stop() {
        stopTicker()
        accumulatedElapsedMillis = 0L
        runStartMark = null
        _state.update {
            StopwatchState()
        }
    }

    private fun addLap() {
        if (!_state.value.isRunning) return
        val totalElapsed = currentElapsedMillis()
        val previousLapTotal = _state.value.laps.sumOf { it.durationMillis }
        val lapDuration = (totalElapsed - previousLapTotal).coerceAtLeast(0L)
        val lap = StopwatchLap(
            lapNumber = _state.value.laps.size + 1,
            durationMillis = lapDuration,
        )
        _state.update {
            it.copy(
                elapsedMillis = totalElapsed,
                laps = it.laps + lap,
            )
        }
    }

    private fun startTicker() {
        stopTicker()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                _state.update { current ->
                    if (current.isRunning) {
                        current.copy(elapsedMillis = currentElapsedMillis())
                    } else {
                        current
                    }
                }
                delay(TICK_INTERVAL_MILLIS)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun currentElapsedMillis(): Long {
        return if (_state.value.isRunning) {
            val runningElapsed = runStartMark?.elapsedNow()?.inWholeMilliseconds ?: 0L
            accumulatedElapsedMillis + runningElapsed
        } else {
            accumulatedElapsedMillis
        }
    }

    override fun onCleared() {
        stopTicker()
        super.onCleared()
    }

    private companion object {
        const val TICK_INTERVAL_MILLIS = 16L
    }
}
