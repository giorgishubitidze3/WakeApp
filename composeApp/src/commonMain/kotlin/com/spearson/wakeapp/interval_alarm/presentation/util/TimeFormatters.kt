package com.spearson.wakeapp.interval_alarm.presentation.util

import com.spearson.wakeapp.interval_alarm.domain.model.TimeOfDay

fun TimeOfDay.toUiLabel(): String {
    val hour12 = when (hour % 12) {
        0 -> 12
        else -> hour % 12
    }
    val amPm = if (hour < 12) "AM" else "PM"
    val minutes = minute.toString().padStart(2, '0')
    return "$hour12:$minutes $amPm"
}
