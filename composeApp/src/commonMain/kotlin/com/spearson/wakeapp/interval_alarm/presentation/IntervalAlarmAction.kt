package com.spearson.wakeapp.interval_alarm.presentation

import com.spearson.wakeapp.interval_alarm.domain.model.HapticsPattern
import com.spearson.wakeapp.interval_alarm.domain.model.Weekday

sealed interface IntervalAlarmAction {
    data class OnScreenOpened(val planId: String?): IntervalAlarmAction
    data object OnCancelClick: IntervalAlarmAction
    data object OnCloseHandled: IntervalAlarmAction
    data object OnAlarmSoundClick: IntervalAlarmAction
    data object OnAlarmSoundBackClick: IntervalAlarmAction
    data object OnAlarmSoundDoneClick: IntervalAlarmAction
    data class OnAlarmRingtoneSelected(val ringtoneId: String): IntervalAlarmAction
    data class OnAlarmRingtonePreviewToggle(val ringtoneId: String): IntervalAlarmAction
    data class OnAlarmVolumeSliderChanged(val sliderValue: Float): IntervalAlarmAction
    data object OnHapticsClick: IntervalAlarmAction
    data object OnHapticsBackClick: IntervalAlarmAction
    data object OnHapticsSaveClick: IntervalAlarmAction
    data class OnHapticsPatternSelected(val pattern: HapticsPattern): IntervalAlarmAction
    data class OnHapticsOnlyChanged(val enabled: Boolean): IntervalAlarmAction
    data class OnHapticsEscalateChanged(val enabled: Boolean): IntervalAlarmAction
    data class OnFocusedWindowChanged(val focusedWindow: FocusedWindow): IntervalAlarmAction
    data class OnHourChanged(val hour12: Int): IntervalAlarmAction
    data class OnMinuteChanged(val minute: Int): IntervalAlarmAction
    data class OnMeridiemChanged(val isAm: Boolean): IntervalAlarmAction
    data class OnIntervalInputChanged(val value: String): IntervalAlarmAction
    data object OnIntervalIncreaseClick: IntervalAlarmAction
    data object OnIntervalDecreaseClick: IntervalAlarmAction
    data class OnRandomizeIntervalChanged(val enabled: Boolean): IntervalAlarmAction
    data class OnWeekdayToggle(val weekday: Weekday): IntervalAlarmAction
    data object OnSavePlanClick: IntervalAlarmAction
    data object OnDismissStatusMessage: IntervalAlarmAction
}
