package com.spearson.wakeapp.timer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spearson.wakeapp.interval_alarm.domain.AlarmRingtonePreviewPlayer
import com.spearson.wakeapp.interval_alarm.domain.AlarmRingtoneProvider
import com.spearson.wakeapp.interval_alarm.domain.model.AlarmRingtoneOption
import com.spearson.wakeapp.timer.domain.TimerCompletedHistoryRepository
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

class TimerViewModel(
    private val alarmRingtoneProvider: AlarmRingtoneProvider,
    private val alarmRingtonePreviewPlayer: AlarmRingtonePreviewPlayer,
    private val timerCompletedHistoryRepository: TimerCompletedHistoryRepository,
) : ViewModel() {

    private var hasLoadedInitialData = false
    private var countdownJob: Job? = null
    private var previewAutoStopJob: Job? = null
    private var countdownStartMark: kotlin.time.TimeMark? = null
    private var countdownStartRemainingMillis: Long = 0L

    private val _state = MutableStateFlow(TimerState())
    val state = _state
        .onStart {
            if (!hasLoadedInitialData) {
                hasLoadedInitialData = true
                loadRingtones()
                loadCompletedTimers()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = TimerState(),
        )

    fun onAction(action: TimerAction) {
        when (action) {
            TimerAction.OnScreenOpened -> {
                ensureRingtonesLoaded()
                ensureCompletedTimersLoaded()
            }
            is TimerAction.OnHourChanged -> updateHour(action.hour)
            is TimerAction.OnMinuteChanged -> updateMinute(action.minute)
            is TimerAction.OnSecondChanged -> updateSecond(action.second)
            TimerAction.OnStartClick -> startTimer()
            TimerAction.OnPauseClick -> pauseTimer()
            TimerAction.OnContinueClick -> continueTimer()
            TimerAction.OnCancelClick -> cancelTimer()
            is TimerAction.OnCompletedTimerClick -> restartFromCompletedTimer(action.durationMillis)
            TimerAction.OnSoundClick -> openSoundSelection()
            TimerAction.OnSoundBackClick -> closeSoundSelection()
            TimerAction.OnSoundDoneClick -> closeSoundSelection()
            is TimerAction.OnRingtoneSelected -> selectRingtone(action.ringtoneId)
            is TimerAction.OnRingtonePreviewToggle -> toggleRingtonePreview(action.ringtoneId)
            is TimerAction.OnVolumeChanged -> changeVolume(action.value)
            TimerAction.OnStatusMessageShown -> clearStatusMessage()
        }
    }

    private fun ensureRingtonesLoaded() {
        val current = _state.value
        if (!current.isLoadingRingtones && current.availableRingtones.isEmpty()) {
            loadRingtones()
        }
    }

    private fun loadRingtones() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingRingtones = true) }
            val result = alarmRingtoneProvider.getAvailableRingtones()
            _state.update { current ->
                val options = result.getOrNull().orEmpty()
                val selected = resolveSelectedRingtone(
                    currentSelectedId = current.selectedRingtoneId,
                    options = options,
                )
                current.copy(
                    isLoadingRingtones = false,
                    availableRingtones = options,
                    selectedRingtoneId = selected?.id ?: current.selectedRingtoneId,
                    selectedRingtoneName = selected?.name ?: current.selectedRingtoneName,
                    statusMessage = if (result.isFailure) {
                        result.exceptionOrNull()?.message ?: "Unable to load ringtones."
                    } else {
                        current.statusMessage
                    },
                )
            }
        }
    }

    private fun ensureCompletedTimersLoaded() {
        if (_state.value.completedTimers.isEmpty()) {
            loadCompletedTimers()
        }
    }

    private fun loadCompletedTimers() {
        viewModelScope.launch {
            val result = timerCompletedHistoryRepository.getCompletedDurationsMillis()
            _state.update { current ->
                if (result.isSuccess) {
                    current.copy(
                        completedTimers = result.getOrNull().orEmpty().toCompletedTimerItems(),
                    )
                } else {
                    current.copy(
                        statusMessage = result.exceptionOrNull()?.message ?: "Unable to load completed timers.",
                    )
                }
            }
        }
    }

    private fun updateHour(hour: Int) {
        if (_state.value.runState != TimerRunState.Idle) return
        _state.update {
            it.copy(
                selectedHours = hour.coerceIn(MIN_HOURS, MAX_HOURS),
                statusMessage = null,
            )
        }
    }

    private fun updateMinute(minute: Int) {
        if (_state.value.runState != TimerRunState.Idle) return
        _state.update {
            it.copy(
                selectedMinutes = minute.coerceIn(MIN_MINUTES, MAX_MINUTES),
                statusMessage = null,
            )
        }
    }

    private fun updateSecond(second: Int) {
        if (_state.value.runState != TimerRunState.Idle) return
        _state.update {
            it.copy(
                selectedSeconds = second.coerceIn(MIN_SECONDS, MAX_SECONDS),
                statusMessage = null,
            )
        }
    }

    private fun startTimer() {
        val current = _state.value
        if (current.runState != TimerRunState.Idle && current.runState != TimerRunState.Completed) return
        val durationMillis = current.selectedDurationMillis
        if (durationMillis <= 0L) {
            _state.update {
                it.copy(statusMessage = "Select a timer duration first.")
            }
            return
        }
        stopRingtonePlayback()
        stopRingtonePreview()
        beginCountdown(durationMillis)
    }

    private fun pauseTimer() {
        if (_state.value.runState != TimerRunState.Running) return
        val remainingMillis = currentRemainingMillis()
        stopCountdown()
        _state.update {
            it.copy(
                runState = TimerRunState.Paused,
                remainingMillis = remainingMillis,
            )
        }
    }

    private fun continueTimer() {
        val current = _state.value
        if (current.runState != TimerRunState.Paused) return
        if (current.remainingMillis <= 0L) {
            _state.update {
                it.copy(
                    runState = TimerRunState.Completed,
                    remainingMillis = 0L,
                )
            }
            return
        }
        beginCountdown(current.remainingMillis)
    }

    private fun cancelTimer() {
        val runState = _state.value.runState
        stopCountdown()
        stopRingtonePlayback()
        if (runState == TimerRunState.Idle) {
            _state.update {
                it.copy(
                    selectedHours = 0,
                    selectedMinutes = 0,
                    selectedSeconds = 0,
                    statusMessage = null,
                )
            }
            return
        }

        _state.update {
            it.copy(
                runState = TimerRunState.Idle,
                remainingMillis = 0L,
                statusMessage = null,
            )
        }
    }

    private fun restartFromCompletedTimer(durationMillis: Long) {
        val resolvedDurationMillis = durationMillis.coerceIn(
            minimumValue = TimerState.MILLIS_PER_SECOND.toLong(),
            maximumValue = MAX_TIMER_DURATION_MILLIS,
        )
        val totalSeconds = (resolvedDurationMillis / TimerState.MILLIS_PER_SECOND).toInt()
        val hours = (totalSeconds / SECONDS_PER_HOUR).coerceIn(MIN_HOURS, MAX_HOURS)
        val minutes = ((totalSeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE).coerceIn(MIN_MINUTES, MAX_MINUTES)
        val seconds = (totalSeconds % SECONDS_PER_MINUTE).coerceIn(MIN_SECONDS, MAX_SECONDS)
        val normalizedDurationMillis = (
            hours * TimerState.MILLIS_PER_HOUR +
                minutes * TimerState.MILLIS_PER_MINUTE +
                seconds * TimerState.MILLIS_PER_SECOND
            ).toLong()

        stopCountdown()
        stopRingtonePlayback()
        stopRingtonePreview()
        _state.update {
            it.copy(
                selectedHours = hours,
                selectedMinutes = minutes,
                selectedSeconds = seconds,
                screenMode = TimerScreenMode.Main,
                statusMessage = null,
            )
        }
        beginCountdown(normalizedDurationMillis)
    }

    private fun openSoundSelection() {
        stopRingtonePlayback()
        ensureRingtonesLoaded()
        _state.update {
            it.copy(screenMode = TimerScreenMode.SoundSelection)
        }
    }

    private fun closeSoundSelection() {
        stopRingtonePreview()
        _state.update {
            it.copy(screenMode = TimerScreenMode.Main)
        }
    }

    private fun selectRingtone(ringtoneId: String) {
        val selected = _state.value.availableRingtones.firstOrNull { it.id == ringtoneId } ?: return
        _state.update {
            it.copy(
                selectedRingtoneId = selected.id,
                selectedRingtoneName = selected.name,
            )
        }
    }

    private fun toggleRingtonePreview(ringtoneId: String) {
        val current = _state.value
        if (current.previewPlayingRingtoneId == ringtoneId) {
            stopRingtonePreview()
            return
        }

        val selected = current.availableRingtones.firstOrNull { it.id == ringtoneId } ?: return
        viewModelScope.launch {
            stopRingtonePlayback()
            stopRingtonePreview()
            val playResult = alarmRingtonePreviewPlayer.playPreview(
                ringtoneId = selected.id,
                volumePercent = _state.value.ringtoneVolumePercent,
            )
            _state.update {
                it.copy(
                    previewPlayingRingtoneId = if (playResult.isSuccess) selected.id else null,
                    statusMessage = if (playResult.isFailure) {
                        playResult.exceptionOrNull()?.message ?: "Unable to play preview."
                    } else {
                        it.statusMessage
                    },
                )
            }

            if (playResult.isSuccess) {
                previewAutoStopJob = viewModelScope.launch {
                    delay(PREVIEW_DURATION_MILLIS)
                    if (_state.value.previewPlayingRingtoneId == selected.id) {
                        stopRingtonePreview()
                    }
                }
            }
        }
    }

    private fun changeVolume(value: Float) {
        val volumePercent = (value.coerceIn(0f, 1f) * 100f).toInt()
        _state.update {
            it.copy(ringtoneVolumePercent = volumePercent.coerceIn(0, 100))
        }
    }

    private fun clearStatusMessage() {
        _state.update {
            it.copy(statusMessage = null)
        }
    }

    private fun beginCountdown(initialRemainingMillis: Long) {
        stopCountdown()
        countdownStartMark = TimeSource.Monotonic.markNow()
        countdownStartRemainingMillis = initialRemainingMillis
        _state.update {
            it.copy(
                runState = TimerRunState.Running,
                remainingMillis = initialRemainingMillis,
                statusMessage = null,
            )
        }
        countdownJob = viewModelScope.launch {
            while (isActive) {
                val remainingMillis = currentRemainingMillis()
                _state.update { current ->
                    if (current.runState == TimerRunState.Running) {
                        current.copy(remainingMillis = remainingMillis)
                    } else {
                        current
                    }
                }
                if (remainingMillis == 0L) {
                    handleTimerFinished()
                    break
                }
                delay(TIMER_TICK_MILLIS)
            }
        }
    }

    private suspend fun handleTimerFinished() {
        stopCountdown(cancelRunningJob = false)
        val current = _state.value
        _state.update {
            it.copy(
                runState = TimerRunState.Completed,
                remainingMillis = 0L,
            )
        }
        val historyResult = timerCompletedHistoryRepository.upsertCompletedDurationMillis(
            durationMillis = current.selectedDurationMillis,
        )
        _state.update { state ->
            if (historyResult.isSuccess) {
                state.copy(
                    completedTimers = historyResult.getOrNull().orEmpty().toCompletedTimerItems(),
                )
            } else {
                state.copy(
                    completedTimers = upsertDurationInMemory(
                        existingTimers = state.completedTimers,
                        durationMillis = current.selectedDurationMillis,
                    ),
                    statusMessage = historyResult.exceptionOrNull()?.message
                        ?: "Unable to persist completed timer history.",
                )
            }
        }
        val playResult = alarmRingtonePreviewPlayer.playPreview(
            ringtoneId = current.selectedRingtoneId,
            volumePercent = current.ringtoneVolumePercent,
        )
        if (playResult.isFailure) {
            _state.update {
                it.copy(
                    statusMessage = playResult.exceptionOrNull()?.message ?: "Unable to play timer ringtone.",
                )
            }
        }
    }

    private fun currentRemainingMillis(): Long {
        val startMark = countdownStartMark ?: return _state.value.remainingMillis
        val elapsedMillis = startMark.elapsedNow().inWholeMilliseconds
        return (countdownStartRemainingMillis - elapsedMillis).coerceAtLeast(0L)
    }

    private fun stopCountdown() {
        stopCountdown(cancelRunningJob = true)
    }

    private fun stopCountdown(cancelRunningJob: Boolean) {
        if (cancelRunningJob) {
            countdownJob?.cancel()
        }
        countdownJob = null
        countdownStartMark = null
        countdownStartRemainingMillis = 0L
    }

    private fun stopRingtonePreview() {
        previewAutoStopJob?.cancel()
        previewAutoStopJob = null
        alarmRingtonePreviewPlayer.stopPreview()
        _state.update {
            it.copy(previewPlayingRingtoneId = null)
        }
    }

    private fun stopRingtonePlayback() {
        alarmRingtonePreviewPlayer.stopPreview()
    }

    private fun resolveSelectedRingtone(
        currentSelectedId: String,
        options: List<AlarmRingtoneOption>,
    ): AlarmRingtoneOption? {
        if (options.isEmpty()) return null
        return options.firstOrNull { it.id == currentSelectedId } ?: options.first()
    }

    private fun List<Long>.toCompletedTimerItems(): List<CompletedTimerItem> {
        return asSequence()
            .map { it.coerceAtLeast(MIN_COMPLETED_TIMER_MILLIS) }
            .distinct()
            .take(MAX_COMPLETED_TIMERS)
            .map { duration ->
                CompletedTimerItem(durationMillis = duration)
            }
            .toList()
    }

    private fun upsertDurationInMemory(
        existingTimers: List<CompletedTimerItem>,
        durationMillis: Long,
    ): List<CompletedTimerItem> {
        val resolvedDuration = durationMillis.coerceAtLeast(MIN_COMPLETED_TIMER_MILLIS)
        return buildList {
            add(CompletedTimerItem(durationMillis = resolvedDuration))
            addAll(existingTimers.filterNot { it.durationMillis == resolvedDuration })
        }.take(MAX_COMPLETED_TIMERS)
    }

    override fun onCleared() {
        stopCountdown()
        stopRingtonePreview()
        stopRingtonePlayback()
        super.onCleared()
    }

    private companion object {
        const val MIN_HOURS = 0
        const val MAX_HOURS = 23
        const val MIN_MINUTES = 0
        const val MAX_MINUTES = 59
        const val MIN_SECONDS = 0
        const val MAX_SECONDS = 59
        const val SECONDS_PER_MINUTE = 60
        const val SECONDS_PER_HOUR = 60 * SECONDS_PER_MINUTE
        const val TIMER_TICK_MILLIS = 100L
        const val PREVIEW_DURATION_MILLIS = 3_000L
        const val MAX_COMPLETED_TIMERS = 12
        const val MIN_COMPLETED_TIMER_MILLIS = 1_000L
        const val MAX_TIMER_DURATION_MILLIS =
            ((MAX_HOURS * SECONDS_PER_HOUR + MAX_MINUTES * SECONDS_PER_MINUTE + MAX_SECONDS) * 1_000L)
    }
}
