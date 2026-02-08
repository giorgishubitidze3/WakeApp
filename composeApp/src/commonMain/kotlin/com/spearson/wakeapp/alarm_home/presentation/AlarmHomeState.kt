package com.spearson.wakeapp.alarm_home.presentation

import com.spearson.wakeapp.interval_alarm.domain.model.IntervalAlarmPlan

data class AlarmHomeState(
    val plans: List<IntervalAlarmPlan> = emptyList(),
    val isLoading: Boolean = false,
    val statusMessage: String? = null,
    val shouldNavigateToCreateAlarm: Boolean = false,
)
