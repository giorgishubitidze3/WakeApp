package com.spearson.wakeapp.interval_alarm.presentation

import com.spearson.wakeapp.interval_alarm.domain.model.Weekday

sealed interface IntervalAlarmAction {
    data class OnScreenOpened(val planId: String?): IntervalAlarmAction
    data object OnCancelClick: IntervalAlarmAction
    data object OnCloseHandled: IntervalAlarmAction
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
