package com.spearson.wakeapp.interval_alarm.presentation.util

import com.spearson.wakeapp.interval_alarm.domain.model.TimeOfDay
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSDate

actual fun currentLocalTimeOfDay(): TimeOfDay {
    val components = NSCalendar.currentCalendar.components(
        unitFlags = NSCalendarUnitHour or NSCalendarUnitMinute,
        fromDate = NSDate(),
    )
    return TimeOfDay(
        hour = components.hour.toInt(),
        minute = components.minute.toInt(),
    )
}
