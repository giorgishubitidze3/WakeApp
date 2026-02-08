package com.spearson.wakeapp.interval_alarm.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spearson.wakeapp.interval_alarm.domain.GenerateAlarmOccurrencesUseCase
import com.spearson.wakeapp.interval_alarm.domain.IntervalAlarmPlanRepository
import com.spearson.wakeapp.interval_alarm.domain.IntervalAlarmScheduler
import com.spearson.wakeapp.interval_alarm.domain.model.IntervalAlarmPlan
import com.spearson.wakeapp.interval_alarm.domain.model.TimeOfDay
import com.spearson.wakeapp.interval_alarm.domain.model.Weekday
import com.spearson.wakeapp.interval_alarm.presentation.util.currentLocalTimeOfDay
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
            is IntervalAlarmAction.OnScreenOpened -> handleScreenOpened(action.planId)
            IntervalAlarmAction.OnCancelClick -> closeScreen()
            IntervalAlarmAction.OnCloseHandled -> markCloseHandled()
            is IntervalAlarmAction.OnFocusedWindowChanged -> handleFocusedWindowChanged(action.focusedWindow)
            is IntervalAlarmAction.OnHourChanged -> handleHourChanged(action.hour12)
            is IntervalAlarmAction.OnMinuteChanged -> handleMinuteChanged(action.minute)
            is IntervalAlarmAction.OnMeridiemChanged -> handleMeridiemChanged(action.isAm)
            is IntervalAlarmAction.OnIntervalInputChanged -> handleIntervalInputChanged(action.value)
            IntervalAlarmAction.OnIntervalIncreaseClick -> adjustInterval(by = 1)
            IntervalAlarmAction.OnIntervalDecreaseClick -> adjustInterval(by = -1)
            is IntervalAlarmAction.OnRandomizeIntervalChanged -> handleRandomizeIntervalChanged(action.enabled)
            is IntervalAlarmAction.OnWeekdayToggle -> handleWeekdayToggle(action.weekday)
            IntervalAlarmAction.OnSavePlanClick -> savePlan()
            IntervalAlarmAction.OnDismissStatusMessage -> dismissStatusMessage()
        }
    }

    private fun handleScreenOpened(planId: String?) {
        if (planId == null) {
            _state.value = newPlanInitialState()
            recalculatePreview()
            return
        }

        viewModelScope.launch {
            val plansResult = intervalAlarmPlanRepository.getPlans()
            val existingPlan = plansResult.getOrNull()?.firstOrNull { it.id == planId }
            if (existingPlan != null) {
                _state.value = existingPlan.toState()
                recalculatePreview()
            } else {
                _state.update {
                    it.copy(
                        editingPlanId = planId,
                        statusMessage = "Unable to load alarm.",
                    )
                }
            }
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

    private fun handleFocusedWindowChanged(focusedWindow: FocusedWindow) {
        _state.update {
            it.copy(focusedWindow = focusedWindow)
        }
    }

    private fun handleHourChanged(hour12: Int) {
        val normalizedHour12 = hour12.coerceIn(HOUR_12_MIN, HOUR_12_MAX)
        updateFocusedTime { focusedTime ->
            val updatedHour = to24Hour(
                hour12 = normalizedHour12,
                isAm = focusedTime.hour < HOURS_PER_HALF_DAY,
            )
            focusedTime.copy(hour = updatedHour)
        }
    }

    private fun handleMinuteChanged(minute: Int) {
        updateFocusedTime { focusedTime ->
            focusedTime.copy(minute = minute.coerceIn(MINUTE_MIN, MINUTE_MAX))
        }
    }

    private fun handleMeridiemChanged(isAm: Boolean) {
        updateFocusedTime { focusedTime ->
            val updatedHour = to24Hour(
                hour12 = focusedTime.toHour12(),
                isAm = isAm,
            )
            focusedTime.copy(hour = updatedHour)
        }
    }

    private fun updateFocusedTime(transform: (TimeOfDay) -> TimeOfDay) {
        _state.update { current ->
            current.withFocusedTime(transform(current.focusedTime()))
        }
        ensureRangeAfterFocusedTimeChange()
        recalculatePreview()
    }

    private fun ensureRangeAfterFocusedTimeChange() {
        _state.update { current ->
            when (current.focusedWindow) {
                FocusedWindow.Start -> {
                    if (current.startTime.toMinutesOfDay() > current.endTime.toMinutesOfDay()) {
                        current.copy(endTime = current.startTime)
                    } else {
                        current
                    }
                }

                FocusedWindow.End -> {
                    if (current.endTime.toMinutesOfDay() < current.startTime.toMinutesOfDay()) {
                        current.copy(startTime = current.endTime)
                    } else {
                        current
                    }
                }
            }
        }
    }

    private fun handleIntervalInputChanged(value: String) {
        val digitsOnly = value.filter(Char::isDigit).take(MAX_INTERVAL_DIGITS)
        if (digitsOnly.isEmpty()) {
            _state.update { it.copy(intervalInput = "") }
            return
        }

        val parsedInterval = digitsOnly.toIntOrNull() ?: return
        val clampedInterval = parsedInterval.coerceIn(INTERVAL_MIN, INTERVAL_MAX)

        _state.update {
            it.copy(
                intervalMinutes = clampedInterval,
                intervalInput = clampedInterval.toString(),
            )
        }
        recalculatePreview()
    }

    private fun adjustInterval(by: Int) {
        _state.update {
            val updatedInterval = (it.intervalMinutes + by).coerceIn(INTERVAL_MIN, INTERVAL_MAX)
            it.copy(
                intervalMinutes = updatedInterval,
                intervalInput = updatedInterval.toString(),
            )
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
            if (weekday in current.activeDays && current.activeDays.size == 1) {
                current
            } else {
                val updatedDays = if (weekday in current.activeDays) {
                    current.activeDays - weekday
                } else {
                    current.activeDays + weekday
                }
                current.copy(activeDays = updatedDays)
            }
        }
        recalculatePreview()
    }

    private fun savePlan() {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, statusMessage = null) }
            val existingPlanId = _state.value.editingPlanId
            val plan = _state.value.toPlan(
                planId = existingPlanId ?: createPlanId(),
            )
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
                    val errorMessage = syncResult.exceptionOrNull()?.message
                    it.copy(
                        isSaving = false,
                        statusMessage = errorMessage ?: "Unable to save alarm.",
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

    private fun recalculatePreview() {
        _state.update {
            val previewTimes = generateAlarmOccurrencesUseCase.generateTimes(
                startTime = it.startTime,
                endTime = it.endTime,
                intervalMinutes = it.intervalMinutes,
            )
            it.copy(
                previewTimes = previewTimes,
                alarmsPerActiveDay = previewTimes.size,
                totalAlarmsPerWeek = previewTimes.size * it.activeDays.size,
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
            isEnabled = isEnabled,
        )
    }

    private fun IntervalAlarmPlan.toState(): IntervalAlarmState {
        return IntervalAlarmState(
            editingPlanId = id,
            startTime = startTime,
            endTime = endTime,
            intervalMinutes = intervalMinutes,
            intervalInput = intervalMinutes.toString(),
            isEnabled = isEnabled,
            activeDays = activeDays,
        )
    }

    private fun createPlanId(): String {
        return "plan_${Random.nextLong(100_000_000L, Long.MAX_VALUE)}"
    }

    private fun newPlanInitialState(): IntervalAlarmState {
        val startTime = currentLocalTimeOfDay()
        val endMinutes = (startTime.toMinutesOfDay() + NEW_PLAN_DEFAULT_WINDOW_MINUTES)
            .coerceAtMost(MINUTES_PER_DAY - 1)
        val endTime = TimeOfDay.fromMinutesOfDay(endMinutes)
        return IntervalAlarmState(
            startTime = startTime,
            endTime = endTime,
        )
    }

    private fun IntervalAlarmState.focusedTime(): TimeOfDay {
        return when (focusedWindow) {
            FocusedWindow.Start -> startTime
            FocusedWindow.End -> endTime
        }
    }

    private fun IntervalAlarmState.withFocusedTime(updatedFocusedTime: TimeOfDay): IntervalAlarmState {
        return when (focusedWindow) {
            FocusedWindow.Start -> copy(startTime = updatedFocusedTime)
            FocusedWindow.End -> copy(endTime = updatedFocusedTime)
        }
    }

    private fun TimeOfDay.toHour12(): Int {
        return when (val converted = hour % HOUR_12_MAX) {
            0 -> HOUR_12_MAX
            else -> converted
        }
    }

    private fun to24Hour(hour12: Int, isAm: Boolean): Int {
        val normalizedHour = hour12.coerceIn(HOUR_12_MIN, HOUR_12_MAX) % HOUR_12_MAX
        return if (isAm) normalizedHour else normalizedHour + HOURS_PER_HALF_DAY
    }

    private companion object {
        const val INTERVAL_MIN = 1
        const val INTERVAL_MAX = 60
        const val MAX_INTERVAL_DIGITS = 3
        const val HOUR_12_MIN = 1
        const val HOUR_12_MAX = 12
        const val HOURS_PER_HALF_DAY = 12
        const val MINUTE_MIN = 0
        const val MINUTE_MAX = 59
        const val NEW_PLAN_DEFAULT_WINDOW_MINUTES = 30
        const val MINUTES_PER_DAY = 1_440
    }
}
