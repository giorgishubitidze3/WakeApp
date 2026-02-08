package com.spearson.wakeapp.interval_alarm.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spearson.wakeapp.interval_alarm.domain.GenerateAlarmOccurrencesUseCase
import com.spearson.wakeapp.interval_alarm.domain.IntervalAlarmPlanRepository
import com.spearson.wakeapp.interval_alarm.domain.IntervalAlarmScheduler
import com.spearson.wakeapp.interval_alarm.domain.model.IntervalAlarmPlan
import com.spearson.wakeapp.interval_alarm.domain.model.Weekday
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class IntervalAlarmViewModel(
    private val generateAlarmOccurrencesUseCase: GenerateAlarmOccurrencesUseCase,
    private val intervalAlarmPlanRepository: IntervalAlarmPlanRepository,
    private val intervalAlarmScheduler: IntervalAlarmScheduler,
) : ViewModel() {

    private var hasLoadedInitialData = false

    private val _state = MutableStateFlow(IntervalAlarmState())
    val state = _state
        .onStart {
            if (!hasLoadedInitialData) {
                loadInitialData()
                hasLoadedInitialData = true
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = IntervalAlarmState(),
        )

    fun onAction(action: IntervalAlarmAction) {
        when (action) {
            is IntervalAlarmAction.OnStartHourChanged -> handleStartHourChanged(action.hour)
            is IntervalAlarmAction.OnStartMinuteChanged -> handleStartMinuteChanged(action.minute)
            is IntervalAlarmAction.OnEndHourChanged -> handleEndHourChanged(action.hour)
            is IntervalAlarmAction.OnEndMinuteChanged -> handleEndMinuteChanged(action.minute)
            is IntervalAlarmAction.OnIntervalMinutesChanged -> handleIntervalChanged(action.minutes)
            is IntervalAlarmAction.OnWeekdayToggle -> handleWeekdayToggle(action.weekday)
            is IntervalAlarmAction.OnPlanEnabledChanged -> handlePlanEnabledChanged(action.isEnabled)
            IntervalAlarmAction.OnSavePlanClick -> savePlan()
            IntervalAlarmAction.OnDismissStatusMessage -> dismissStatusMessage()
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val loadedPlan = intervalAlarmPlanRepository.getPlan().getOrNull()
            val stateFromPlan = loadedPlan?.toState() ?: IntervalAlarmState()
            updateStateWithPreview(stateFromPlan)
        }
    }

    private fun handleStartHourChanged(hour: Int) {
        _state.update { current ->
            current.copy(
                startTime = current.startTime.copy(hour = hour),
            )
        }
        ensureValidRangeAfterStartChange()
        recalculatePreview()
    }

    private fun handleStartMinuteChanged(minute: Int) {
        _state.update { current ->
            current.copy(
                startTime = current.startTime.copy(minute = minute),
            )
        }
        ensureValidRangeAfterStartChange()
        recalculatePreview()
    }

    private fun handleEndHourChanged(hour: Int) {
        _state.update { current ->
            current.copy(
                endTime = current.endTime.copy(hour = hour),
            )
        }
        ensureValidRangeAfterEndChange()
        recalculatePreview()
    }

    private fun handleEndMinuteChanged(minute: Int) {
        _state.update { current ->
            current.copy(
                endTime = current.endTime.copy(minute = minute),
            )
        }
        ensureValidRangeAfterEndChange()
        recalculatePreview()
    }

    private fun handleIntervalChanged(minutes: Int) {
        _state.update {
            it.copy(intervalMinutes = minutes)
        }
        recalculatePreview()
    }

    private fun handleWeekdayToggle(weekday: Weekday) {
        _state.update { current ->
            val updatedDays = if (weekday in current.activeDays) {
                current.activeDays - weekday
            } else {
                current.activeDays + weekday
            }

            current.copy(activeDays = updatedDays)
        }
        recalculatePreview()
    }

    private fun handlePlanEnabledChanged(isEnabled: Boolean) {
        _state.update {
            it.copy(isPlanEnabled = isEnabled)
        }
        recalculatePreview()
    }

    private fun savePlan() {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, statusMessage = null) }
            val plan = _state.value.toPlan()
            val saveResult = intervalAlarmPlanRepository.upsertPlan(plan)
            val syncResult = if (saveResult.isSuccess) {
                if (plan.isEnabled) {
                    val occurrences = generateAlarmOccurrencesUseCase(plan)
                    intervalAlarmScheduler.schedulePlan(plan, occurrences)
                } else {
                    intervalAlarmScheduler.cancelPlan(plan.id)
                }
            } else {
                Result.failure(saveResult.exceptionOrNull() ?: IllegalStateException("Plan save failed."))
            }
            _state.update {
                it.copy(
                    isSaving = false,
                    statusMessage = if (syncResult.isSuccess) {
                        "Plan saved."
                    } else {
                        "Unable to save and sync alarms."
                    },
                )
            }
        }
    }

    private fun dismissStatusMessage() {
        _state.update {
            it.copy(statusMessage = null)
        }
    }

    private fun ensureValidRangeAfterStartChange() {
        _state.update { current ->
            if (current.startTime.toMinutesOfDay() > current.endTime.toMinutesOfDay()) {
                current.copy(endTime = current.startTime)
            } else {
                current
            }
        }
    }

    private fun ensureValidRangeAfterEndChange() {
        _state.update { current ->
            if (current.endTime.toMinutesOfDay() < current.startTime.toMinutesOfDay()) {
                current.copy(startTime = current.endTime)
            } else {
                current
            }
        }
    }

    private fun recalculatePreview() {
        updateStateWithPreview(_state.value)
    }

    private fun updateStateWithPreview(state: IntervalAlarmState) {
        val previewTimes = generateAlarmOccurrencesUseCase.generateTimes(
            startTime = state.startTime,
            endTime = state.endTime,
            intervalMinutes = state.intervalMinutes,
        )
        val alarmsPerActiveDay = if (state.isPlanEnabled) previewTimes.size else 0
        val totalAlarmsPerWeek = alarmsPerActiveDay * state.activeDays.size

        _state.update {
            state.copy(
                previewTimes = previewTimes,
                alarmsPerActiveDay = alarmsPerActiveDay,
                totalAlarmsPerWeek = totalAlarmsPerWeek,
            )
        }
    }

    private fun IntervalAlarmPlan.toState(): IntervalAlarmState {
        return IntervalAlarmState(
            startTime = startTime,
            endTime = endTime,
            intervalMinutes = intervalMinutes,
            activeDays = activeDays,
            isPlanEnabled = isEnabled,
            previewTimes = generateAlarmOccurrencesUseCase.generateTimes(
                startTime = startTime,
                endTime = endTime,
                intervalMinutes = intervalMinutes,
            ),
        )
    }

    private fun IntervalAlarmState.toPlan(): IntervalAlarmPlan {
        return IntervalAlarmPlan(
            startTime = startTime,
            endTime = endTime,
            intervalMinutes = intervalMinutes,
            activeDays = activeDays,
            isEnabled = isPlanEnabled,
        )
    }
}
