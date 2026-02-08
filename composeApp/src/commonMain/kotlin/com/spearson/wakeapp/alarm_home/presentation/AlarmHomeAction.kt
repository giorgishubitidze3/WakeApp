package com.spearson.wakeapp.alarm_home.presentation

sealed interface AlarmHomeAction {
    data object OnScreenShown: AlarmHomeAction
    data object OnCreateAlarmClick: AlarmHomeAction
    data object OnCreateAlarmNavigationHandled: AlarmHomeAction
    data class OnPlanEnabledChanged(val planId: String, val isEnabled: Boolean): AlarmHomeAction
    data object OnDismissStatusMessage: AlarmHomeAction
}
