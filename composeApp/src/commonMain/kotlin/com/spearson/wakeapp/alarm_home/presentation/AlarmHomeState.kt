package com.spearson.wakeapp.alarm_home.presentation

import com.spearson.wakeapp.interval_alarm.domain.model.IntervalAlarmPlan

data class AlarmHomeState(
    val plans: List<IntervalAlarmPlan> = emptyList(),
    val isLoading: Boolean = false,
    val statusMessage: String? = null,
    val pendingDeletePlanId: String? = null,
    val navigationEvent: AlarmHomeNavigationEvent? = null,
)

sealed interface AlarmHomeNavigationEvent {
    data object Create: AlarmHomeNavigationEvent
    data class Edit(val planId: String): AlarmHomeNavigationEvent
}
