package com.spearson.wakeapp.interval_alarm.data

import android.content.Context
import android.media.RingtoneManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import com.spearson.wakeapp.interval_alarm.domain.AlarmRingtonePreviewPlayer
import com.spearson.wakeapp.interval_alarm.domain.model.IntervalAlarmPlan

class PlatformAlarmRingtonePreviewPlayer(
    private val context: Context,
) : AlarmRingtonePreviewPlayer {

    private var mediaPlayer: MediaPlayer? = null

    override suspend fun playPreview(
        ringtoneId: String,
        volumePercent: Int,
    ): Result<Unit> {
        return runCatching {
            stopPreview()
            val ringtoneUri = resolveRingtoneUri(ringtoneId)
            val normalizedVolume = (volumePercent.coerceIn(MIN_VOLUME_PERCENT, MAX_FREE_VOLUME_PERCENT) / 100f)
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                setDataSource(context, ringtoneUri)
                isLooping = true
                prepare()
                setVolume(normalizedVolume, normalizedVolume)
                start()
            }
            mediaPlayer = player
        }
    }

    override fun stopPreview() {
        val player = mediaPlayer ?: return
        runCatching { player.stop() }
        runCatching { player.release() }
        mediaPlayer = null
    }

    private fun resolveRingtoneUri(ringtoneId: String): Uri {
        if (ringtoneId == IntervalAlarmPlan.DEFAULT_RINGTONE_ID) {
            return RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }
        return Uri.parse(ringtoneId)
    }

    private companion object {
        const val MIN_VOLUME_PERCENT = 0
        const val MAX_FREE_VOLUME_PERCENT = 100
    }
}
