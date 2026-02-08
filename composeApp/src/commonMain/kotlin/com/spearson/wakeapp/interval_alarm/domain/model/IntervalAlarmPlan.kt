package com.spearson.wakeapp.interval_alarm.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class IntervalAlarmPlan(
    val id: String = DEFAULT_PLAN_ID,
    val startTime: TimeOfDay = TimeOfDay(hour = 7, minute = 0),
    val endTime: TimeOfDay = TimeOfDay(hour = 7, minute = 30),
    val intervalMinutes: Int = 5,
    val activeDays: Set<Weekday> = Weekday.weekdays(),
    val snoozeMinutes: Int = 5,
    val isEnabled: Boolean = true,
) {
    companion object {
        const val DEFAULT_PLAN_ID = "default_plan"
    }
}
