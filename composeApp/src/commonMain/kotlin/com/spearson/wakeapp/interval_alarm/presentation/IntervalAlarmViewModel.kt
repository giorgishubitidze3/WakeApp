package com.spearson.wakeapp.interval_alarm.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spearson.wakeapp.interval_alarm.domain.GenerateAlarmOccurrencesUseCase
import com.spearson.wakeapp.interval_alarm.domain.IntervalAlarmPlanRepository
import com.spearson.wakeapp.interval_alarm.domain.IntervalAlarmScheduler
import com.spearson.wakeapp.interval_alarm.domain.model.IntervalAlarmPlan
import com.spearson.wakeapp.interval_alarm.domain.model.Weekday
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class IntervalAlarmViewModel(
    private val generateAlarmOccurrencesUseCase: GenerateAlarmOccurrencesUseCase,
    private val intervalAlarmPlanRepository: IntervalAlarmPlanRepository,
    private val intervalAlarmScheduler: IntervalAlarmScheduler,
) : ViewModel() {

    private val _state = MutableStateFlow(IntervalAlarmState())
    val state = _state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = IntervalAlarmState(),
    )

    init {
        recalculatePreview()
    }

    fun onAction(action: IntervalAlarmAction) {
        when (action) {
            IntervalAlarmAction.OnCancelClick -> closeScreen()
            IntervalAlarmAction.OnCloseHandled -> markCloseHandled()
            is IntervalAlarmAction.OnStartHourChanged -> handleStartHourChanged(action.hour)
            is IntervalAlarmAction.OnStartMinuteChanged -> handleStartMinuteChanged(action.minute)
            is IntervalAlarmAction.OnEndHourChanged -> handleEndHourChanged(action.hour)
            is IntervalAlarmAction.OnEndMinuteChanged -> handleEndMinuteChanged(action.minute)
            is IntervalAlarmAction.OnIntervalMinutesChanged -> handleIntervalChanged(action.minutes)
            is IntervalAlarmAction.OnRandomizeIntervalChanged -> handleRandomizeIntervalChanged(action.enabled)
            is IntervalAlarmAction.OnWeekdayToggle -> handleWeekdayToggle(action.weekday)
            IntervalAlarmAction.OnSavePlanClick -> savePlan()
            IntervalAlarmAction.OnDismissStatusMessage -> dismissStatusMessage()
        }
    }

    private fun closeScreen() {
        _state.update {
            it.copy(shouldCloseScreen = true)
        }
    }

    private fun markCloseHandled() {
        _state.update {
            it.copy(shouldCloseScreen = false)
        }
    }

    private fun handleStartHourChanged(hour: Int) {
        _state.update { current ->
            current.copy(startTime = current.startTime.copy(hour = hour))
        }
        ensureValidRangeAfterStartChange()
        recalculatePreview()
    }

    private fun handleStartMinuteChanged(minute: Int) {
        _state.update { current ->
            current.copy(startTime = current.startTime.copy(minute = minute))
        }
        ensureValidRangeAfterStartChange()
        recalculatePreview()
    }

    private fun handleEndHourChanged(hour: Int) {
        _state.update { current ->
            current.copy(endTime = current.endTime.copy(hour = hour))
        }
        ensureValidRangeAfterEndChange()
        recalculatePreview()
    }

    private fun handleEndMinuteChanged(minute: Int) {
        _state.update { current ->
            current.copy(endTime = current.endTime.copy(minute = minute))
        }
        ensureValidRangeAfterEndChange()
        recalculatePreview()
    }

    private fun handleIntervalChanged(minutes: Int) {
        _state.update {
            it.copy(intervalMinutes = minutes.coerceIn(INTERVAL_MIN, INTERVAL_MAX))
        }
        recalculatePreview()
    }

    private fun handleRandomizeIntervalChanged(enabled: Boolean) {
        _state.update {
            it.copy(randomizeInterval = enabled)
        }
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

    private fun savePlan() {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, statusMessage = null) }
            val plan = _state.value.toPlan(planId = createPlanId())
            val persistResult = intervalAlarmPlanRepository.upsertPlan(plan)
            val syncResult = if (persistResult.isSuccess) {
                val occurrences = generateAlarmOccurrencesUseCase(plan)
                intervalAlarmScheduler.schedulePlan(plan, occurrences)
            } else {
                Result.failure(persistResult.exceptionOrNull() ?: IllegalStateException("Plan save failed."))
            }

            _state.update {
                if (syncResult.isSuccess) {
                    it.copy(
                        isSaving = false,
                        statusMessage = null,
                        shouldCloseScreen = true,
                    )
                } else {
                    it.copy(
                        isSaving = false,
                        statusMessage = "Unable to save alarm.",
                    )
                }
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
        _state.update {
            state.copy(
                previewTimes = previewTimes,
                alarmsPerActiveDay = previewTimes.size,
                totalAlarmsPerWeek = previewTimes.size * state.activeDays.size,
            )
        }
    }

    private fun IntervalAlarmState.toPlan(planId: String): IntervalAlarmPlan {
        return IntervalAlarmPlan(
            id = planId,
            startTime = startTime,
            endTime = endTime,
            intervalMinutes = intervalMinutes,
            activeDays = activeDays,
            isEnabled = true,
        )
    }

    private fun createPlanId(): String {
        return "plan_${Random.nextLong(100_000_000L, Long.MAX_VALUE)}"
    }

    private companion object {
        const val INTERVAL_MIN = 1
        const val INTERVAL_MAX = 10
    }
}
