package com.spearson.wakeapp.interval_alarm.presentation

import com.spearson.wakeapp.interval_alarm.domain.model.AlarmRingtoneOption
import com.spearson.wakeapp.interval_alarm.domain.model.HapticsPattern
import com.spearson.wakeapp.interval_alarm.domain.model.IntervalAlarmPlan
import com.spearson.wakeapp.interval_alarm.domain.model.TimeOfDay
import com.spearson.wakeapp.interval_alarm.domain.model.Weekday

data class IntervalAlarmState(
    val editingPlanId: String? = null,
    val startTime: TimeOfDay = TimeOfDay(hour = 7, minute = 0),
    val endTime: TimeOfDay = TimeOfDay(hour = 7, minute = 30),
    val intervalMinutes: Int = 5,
    val intervalInput: String = "5",
    val randomizeInterval: Boolean = false,
    val isEnabled: Boolean = true,
    val activeDays: Set<Weekday> = Weekday.allDays(),
    val selectedRingtoneId: String = IntervalAlarmPlan.DEFAULT_RINGTONE_ID,
    val selectedRingtoneName: String = IntervalAlarmPlan.DEFAULT_RINGTONE_NAME,
    val ringtoneVolumePercent: Int = IntervalAlarmPlan.DEFAULT_RINGTONE_VOLUME_PERCENT,
    val selectedHapticsPattern: HapticsPattern = IntervalAlarmPlan.DEFAULT_HAPTICS_PATTERN,
    val hapticsOnly: Boolean = IntervalAlarmPlan.DEFAULT_HAPTICS_ONLY,
    val hapticsEscalateOverTime: Boolean = IntervalAlarmPlan.DEFAULT_HAPTICS_ESCALATE_OVER_TIME,
    val availableRingtones: List<AlarmRingtoneOption> = emptyList(),
    val isLoadingRingtones: Boolean = false,
    val previewPlayingRingtoneId: String? = null,
    val previewPlayingHapticsPattern: HapticsPattern? = null,
    val screenMode: IntervalAlarmScreenMode = IntervalAlarmScreenMode.Editor,
    val focusedWindow: FocusedWindow = FocusedWindow.Start,
    val alarmsPerActiveDay: Int = 0,
    val totalAlarmsPerWeek: Int = 0,
    val previewTimes: List<TimeOfDay> = emptyList(),
    val isSaving: Boolean = false,
    val statusMessage: String? = null,
    val shouldCloseScreen: Boolean = false,
)

enum class FocusedWindow {
    Start,
    End,
}

enum class IntervalAlarmScreenMode {
    Editor,
    SoundSelection,
    HapticsSelection,
}
