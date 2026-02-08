package com.spearson.wakeapp.interval_alarm.domain.model

data class AlarmOccurrence(
    val weekday: Weekday,
    val time: TimeOfDay,
)
