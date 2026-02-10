package com.spearson.wakeapp.interval_alarm.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class IntervalAlarmPlan(
    val id: String = DEFAULT_PLAN_ID,
    val startTime: TimeOfDay = TimeOfDay(hour = 7, minute = 0),
    val endTime: TimeOfDay = TimeOfDay(hour = 7, minute = 30),
    val intervalMinutes: Int = 5,
    val activeDays: Set<Weekday> = Weekday.allDays(),
    val snoozeMinutes: Int = 5,
    val ringtoneId: String = DEFAULT_RINGTONE_ID,
    val ringtoneName: String = DEFAULT_RINGTONE_NAME,
    val ringtoneVolumePercent: Int = DEFAULT_RINGTONE_VOLUME_PERCENT,
    val hapticsPattern: HapticsPattern = DEFAULT_HAPTICS_PATTERN,
    val hapticsOnly: Boolean = DEFAULT_HAPTICS_ONLY,
    val hapticsEscalateOverTime: Boolean = DEFAULT_HAPTICS_ESCALATE_OVER_TIME,
    val isEnabled: Boolean = true,
) {
    companion object {
        const val DEFAULT_PLAN_ID = "default_plan"
        const val DEFAULT_RINGTONE_ID = "system_default_alarm"
        const val DEFAULT_RINGTONE_NAME = "Morning Breeze"
        const val DEFAULT_RINGTONE_VOLUME_PERCENT = 100
        val DEFAULT_HAPTICS_PATTERN = HapticsPattern.GentlePulse
        const val DEFAULT_HAPTICS_ONLY = false
        const val DEFAULT_HAPTICS_ESCALATE_OVER_TIME = false
    }
}
