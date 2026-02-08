package com.spearson.wakeapp.alarm_home.presentation

sealed interface AlarmHomeAction {
    data object OnScreenShown: AlarmHomeAction
    data object OnCreateAlarmClick: AlarmHomeAction
    data class OnPlanClick(val planId: String): AlarmHomeAction
    data class OnPlanLongPress(val planId: String): AlarmHomeAction
    data object OnDeletePlanConfirm: AlarmHomeAction
    data object OnDeletePlanDismiss: AlarmHomeAction
    data object OnNavigationHandled: AlarmHomeAction
    data class OnPlanEnabledChanged(val planId: String, val isEnabled: Boolean): AlarmHomeAction
    data object OnDismissStatusMessage: AlarmHomeAction
}
