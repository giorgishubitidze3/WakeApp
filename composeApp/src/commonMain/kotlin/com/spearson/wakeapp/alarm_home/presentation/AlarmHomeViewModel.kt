package com.spearson.wakeapp.alarm_home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spearson.wakeapp.interval_alarm.domain.GenerateAlarmOccurrencesUseCase
import com.spearson.wakeapp.interval_alarm.domain.IntervalAlarmPlanRepository
import com.spearson.wakeapp.interval_alarm.domain.IntervalAlarmScheduler
import com.spearson.wakeapp.interval_alarm.domain.model.IntervalAlarmPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            is AlarmHomeAction.OnPlanClick -> requestEditAlarmNavigation(action.planId)
            is AlarmHomeAction.OnPlanLongPress -> requestDeletePlan(action.planId)
            AlarmHomeAction.OnDeletePlanConfirm -> confirmDeletePlan()
            AlarmHomeAction.OnDeletePlanDismiss -> dismissDeletePlan()
            AlarmHomeAction.OnNavigationHandled -> markNavigationHandled()
            is AlarmHomeAction.OnPlanEnabledChanged -> updatePlanEnabledState(action.planId, action.isEnabled)
            AlarmHomeAction.OnDismissStatusMessage -> dismissStatusMessage()
        }
    }

    private fun loadPlans() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val plansResult = intervalAlarmPlanRepository.getPlans()
            _state.update { current ->
                current.copy(
                    isLoading = false,
                    plans = sortPlans(plansResult.getOrElse { emptyList() }),
                    statusMessage = if (plansResult.isFailure) {
                        "Unable to load alarms."
                    } else {
                        current.statusMessage
                    },
                )
            }
        }
    }

    private fun requestCreateAlarmNavigation() {
        _state.update {
            it.copy(navigationEvent = AlarmHomeNavigationEvent.Create)
        }
    }

    private fun requestEditAlarmNavigation(planId: String) {
        _state.update {
            it.copy(navigationEvent = AlarmHomeNavigationEvent.Edit(planId))
        }
    }

    private fun requestDeletePlan(planId: String) {
        _state.update {
            it.copy(pendingDeletePlanId = planId)
        }
    }

    private fun dismissDeletePlan() {
        _state.update {
            it.copy(pendingDeletePlanId = null)
        }
    }

    private fun confirmDeletePlan() {
        val planId = _state.value.pendingDeletePlanId ?: return

        _state.update { current ->
            current.copy(
                pendingDeletePlanId = null,
                plans = current.plans.filterNot { it.id == planId },
            )
        }

        viewModelScope.launch {
            val deleteResult = intervalAlarmPlanRepository.deletePlan(planId)
            val cancelResult = intervalAlarmScheduler.cancelPlan(planId)
            if (cancelResult.isFailure || deleteResult.isFailure) {
                _state.update {
                    it.copy(statusMessage = "Unable to delete alarm.")
                }
                loadPlans()
            }
        }
    }

    private fun markNavigationHandled() {
        _state.update {
            it.copy(navigationEvent = null)
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
                plans = sortPlans(
                    current.plans.map { plan ->
                        if (plan.id == planId) updatedPlan else plan
                    },
                ),
            )
        }

        viewModelScope.launch {
            val persistResult = intervalAlarmPlanRepository.upsertPlan(updatedPlan)
            val syncResult = if (persistResult.isSuccess) {
                if (updatedPlan.isEnabled) {
                    val occurrences = withContext(Dispatchers.Default) {
                        generateAlarmOccurrencesUseCase(updatedPlan)
                    }
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

    private fun sortPlans(plans: List<IntervalAlarmPlan>): List<IntervalAlarmPlan> {
        return plans.sortedWith(
            compareByDescending<IntervalAlarmPlan> { it.isEnabled }
                .thenBy { it.startTime.toMinutesOfDay() }
                .thenBy { it.endTime.toMinutesOfDay() },
        )
    }
}
