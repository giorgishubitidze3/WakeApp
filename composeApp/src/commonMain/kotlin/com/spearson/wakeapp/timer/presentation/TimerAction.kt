package com.spearson.wakeapp.timer.presentation

sealed interface TimerAction {
    data object OnScreenOpened : TimerAction

    data class OnHourChanged(val hour: Int) : TimerAction
    data class OnMinuteChanged(val minute: Int) : TimerAction
    data class OnSecondChanged(val second: Int) : TimerAction

    data object OnStartClick : TimerAction
    data object OnPauseClick : TimerAction
    data object OnContinueClick : TimerAction
    data object OnCancelClick : TimerAction
    data class OnCompletedTimerClick(val durationMillis: Long) : TimerAction

    data object OnSoundClick : TimerAction
    data object OnSoundBackClick : TimerAction
    data object OnSoundDoneClick : TimerAction
    data class OnRingtoneSelected(val ringtoneId: String) : TimerAction
    data class OnRingtonePreviewToggle(val ringtoneId: String) : TimerAction
    data class OnVolumeChanged(val value: Float) : TimerAction

    data object OnStatusMessageShown : TimerAction
}
