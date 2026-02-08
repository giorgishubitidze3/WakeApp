package com.spearson.wakeapp.interval_alarm.presentation

import com.spearson.wakeapp.interval_alarm.domain.model.TimeOfDay
import com.spearson.wakeapp.interval_alarm.domain.model.Weekday

data class IntervalAlarmState(
    val startTime: TimeOfDay = TimeOfDay(hour = 7, minute = 0),
    val endTime: TimeOfDay = TimeOfDay(hour = 7, minute = 30),
    val intervalMinutes: Int = 5,
    val activeDays: Set<Weekday> = Weekday.weekdays(),
    val isPlanEnabled: Boolean = true,
    val alarmsPerActiveDay: Int = 0,
    val totalAlarmsPerWeek: Int = 0,
    val previewTimes: List<TimeOfDay> = emptyList(),
    val isSaving: Boolean = false,
    val statusMessage: String? = null,
)
