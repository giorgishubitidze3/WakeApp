package com.spearson.wakeapp.interval_alarm.domain

interface AlarmRingtonePreviewPlayer {
    suspend fun playPreview(
        ringtoneId: String,
        volumePercent: Int,
    ): Result<Unit>

    fun stopPreview()
}
