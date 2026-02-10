package com.spearson.wakeapp.interval_alarm.data

import com.spearson.wakeapp.interval_alarm.domain.AlarmRingtonePreviewPlayer

class PlatformAlarmRingtonePreviewPlayer : AlarmRingtonePreviewPlayer {
    override suspend fun playPreview(
        ringtoneId: String,
        volumePercent: Int,
    ): Result<Unit> {
        return Result.success(Unit)
    }

    override fun stopPreview() = Unit
}
