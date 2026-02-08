package com.spearson.wakeapp.interval_alarm.presentation

import com.spearson.wakeapp.interval_alarm.domain.model.Weekday

sealed interface IntervalAlarmAction {
    data class OnStartHourChanged(val hour: Int): IntervalAlarmAction
    data class OnStartMinuteChanged(val minute: Int): IntervalAlarmAction
    data class OnEndHourChanged(val hour: Int): IntervalAlarmAction
    data class OnEndMinuteChanged(val minute: Int): IntervalAlarmAction
    data class OnIntervalMinutesChanged(val minutes: Int): IntervalAlarmAction
    data class OnWeekdayToggle(val weekday: Weekday): IntervalAlarmAction
    data class OnPlanEnabledChanged(val isEnabled: Boolean): IntervalAlarmAction
    data object OnSavePlanClick: IntervalAlarmAction
    data object OnDismissStatusMessage: IntervalAlarmAction
}
