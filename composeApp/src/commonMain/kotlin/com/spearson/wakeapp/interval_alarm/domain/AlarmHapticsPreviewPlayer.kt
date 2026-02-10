package com.spearson.wakeapp.interval_alarm.domain

import com.spearson.wakeapp.interval_alarm.domain.model.HapticsPattern

interface AlarmHapticsPreviewPlayer {
    suspend fun playPreview(
        pattern: HapticsPattern,
        escalateOverTime: Boolean,
    ): Result<Unit>

    fun stopPreview()
}
