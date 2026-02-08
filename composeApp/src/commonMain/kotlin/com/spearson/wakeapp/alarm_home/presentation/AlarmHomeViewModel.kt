package com.spearson.wakeapp.alarm_home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spearson.wakeapp.interval_alarm.domain.GenerateAlarmOccurrencesUseCase
import com.spearson.wakeapp.interval_alarm.domain.IntervalAlarmPlanRepository
import com.spearson.wakeapp.interval_alarm.domain.IntervalAlarmScheduler
import com.spearson.wakeapp.interval_alarm.domain.model.IntervalAlarmPlan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AlarmHomeViewModel(
    private val intervalAlarmPlanRepository: IntervalAlarmPlanRepository,
    private val intervalAlarmScheduler: IntervalAlarmScheduler,
    private val generateAlarmOccurrencesUseCase: GenerateAlarmOccurrencesUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(AlarmHomeState())
    val state = _state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = AlarmHomeState(),
    )

    fun onAction(action: AlarmHomeAction) {
        when (action) {
            AlarmHomeAction.OnScreenShown -> loadPlans()
            AlarmHomeAction.OnCreateAlarmClick -> requestCreateAlarmNavigation()
            AlarmHomeAction.OnCreateAlarmNavigationHandled -> markCreateAlarmNavigationHandled()
            is AlarmHomeAction.OnPlanEnabledChanged -> updatePlanEnabledState(action.planId, action.isEnabled)
            AlarmHomeAction.OnDismissStatusMessage -> dismissStatusMessage()
        }
    }

    private fun loadPlans() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val plans = intervalAlarmPlanRepository.getPlans().getOrElse { emptyList() }
            _state.update {
                it.copy(
                    isLoading = false,
                    plans = plans.sortedBy { plan -> plan.startTime.toMinutesOfDay() },
                )
            }
        }
    }

    private fun requestCreateAlarmNavigation() {
        _state.update {
            it.copy(shouldNavigateToCreateAlarm = true)
        }
    }

    private fun markCreateAlarmNavigationHandled() {
        _state.update {
            it.copy(shouldNavigateToCreateAlarm = false)
        }
    }

    private fun updatePlanEnabledState(
        planId: String,
        isEnabled: Boolean,
    ) {
        val currentPlan = _state.value.plans.firstOrNull { it.id == planId } ?: return
        val updatedPlan = currentPlan.copy(isEnabled = isEnabled)

        _state.update { current ->
            current.copy(
                plans = current.plans.map { plan ->
                    if (plan.id == planId) updatedPlan else plan
                },
            )
        }

        viewModelScope.launch {
            val persistResult = intervalAlarmPlanRepository.upsertPlan(updatedPlan)
            val syncResult = if (persistResult.isSuccess) {
                if (updatedPlan.isEnabled) {
                    val occurrences = generateAlarmOccurrencesUseCase(updatedPlan)
                    intervalAlarmScheduler.schedulePlan(updatedPlan, occurrences)
                } else {
                    intervalAlarmScheduler.cancelPlan(updatedPlan.id)
                }
            } else {
                Result.failure(persistResult.exceptionOrNull() ?: IllegalStateException("Unable to persist plan state."))
            }

            if (syncResult.isFailure) {
                _state.update {
                    it.copy(statusMessage = "Unable to sync alarm schedule.")
                }
            }
        }
    }

    private fun dismissStatusMessage() {
        _state.update {
            it.copy(statusMessage = null)
        }
    }
}
