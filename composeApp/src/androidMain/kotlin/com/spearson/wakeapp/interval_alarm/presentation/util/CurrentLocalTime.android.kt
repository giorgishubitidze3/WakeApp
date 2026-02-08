package com.spearson.wakeapp.interval_alarm.presentation.util

import com.spearson.wakeapp.interval_alarm.domain.model.TimeOfDay
import java.util.Calendar

actual fun currentLocalTimeOfDay(): TimeOfDay {
    val calendar = Calendar.getInstance()
    return TimeOfDay(
        hour = calendar.get(Calendar.HOUR_OF_DAY),
        minute = calendar.get(Calendar.MINUTE),
    )
}
