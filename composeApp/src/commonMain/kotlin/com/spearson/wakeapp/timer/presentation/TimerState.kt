package com.spearson.wakeapp.timer.presentation

import com.spearson.wakeapp.interval_alarm.domain.model.AlarmRingtoneOption
import com.spearson.wakeapp.interval_alarm.domain.model.IntervalAlarmPlan

data class TimerState(
    val selectedHours: Int = 0,
    val selectedMinutes: Int = 15,
    val selectedSeconds: Int = 0,
    val remainingMillis: Long = 0L,
    val runState: TimerRunState = TimerRunState.Idle,
    val completedTimers: List<CompletedTimerItem> = emptyList(),
    val selectedRingtoneId: String = IntervalAlarmPlan.DEFAULT_RINGTONE_ID,
    val selectedRingtoneName: String = IntervalAlarmPlan.DEFAULT_RINGTONE_NAME,
    val availableRingtones: List<AlarmRingtoneOption> = emptyList(),
    val previewPlayingRingtoneId: String? = null,
    val isLoadingRingtones: Boolean = false,
    val ringtoneVolumePercent: Int = DEFAULT_VOLUME_PERCENT,
    val screenMode: TimerScreenMode = TimerScreenMode.Main,
    val statusMessage: String? = null,
) {
    val selectedDurationMillis: Long
        get() = (
            selectedHours * MILLIS_PER_HOUR +
                selectedMinutes * MILLIS_PER_MINUTE +
                selectedSeconds * MILLIS_PER_SECOND
            ).toLong()

    val displayMillis: Long
        get() = when (runState) {
            TimerRunState.Idle -> selectedDurationMillis
            TimerRunState.Running,
            TimerRunState.Paused,
            TimerRunState.Completed,
            -> remainingMillis.coerceAtLeast(0L)
        }

    companion object {
        const val MILLIS_PER_SECOND = 1_000
        const val MILLIS_PER_MINUTE = 60 * MILLIS_PER_SECOND
        const val MILLIS_PER_HOUR = 60 * MILLIS_PER_MINUTE
        const val DEFAULT_VOLUME_PERCENT = 100
    }
}

data class CompletedTimerItem(
    val durationMillis: Long,
)

enum class TimerRunState {
    Idle,
    Running,
    Paused,
    Completed,
}

enum class TimerScreenMode {
    Main,
    SoundSelection,
}
