package com.spearson.wakeapp.interval_alarm.data

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.spearson.wakeapp.interval_alarm.data.android.AlarmHapticsPatterns
import com.spearson.wakeapp.interval_alarm.domain.AlarmHapticsPreviewPlayer
import com.spearson.wakeapp.interval_alarm.domain.model.HapticsPattern

class PlatformAlarmHapticsPreviewPlayer(
    private val context: Context,
) : AlarmHapticsPreviewPlayer {

    private val vibrator: Vibrator?
        get() = resolveVibrator()

    override suspend fun playPreview(
        pattern: HapticsPattern,
        escalateOverTime: Boolean,
    ): Result<Unit> {
        return runCatching {
            stopPreview()
            if (!AlarmHapticsPatterns.shouldVibrate(pattern)) return@runCatching
            val resolvedVibrator = vibrator ?: return@runCatching
            if (!resolvedVibrator.hasVibrator()) return@runCatching
            val waveform = AlarmHapticsPatterns.buildPreviewWaveform(
                pattern = pattern,
                escalateOverTime = escalateOverTime,
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (resolvedVibrator.hasAmplitudeControl()) {
                    resolvedVibrator.vibrate(
                        VibrationEffect.createWaveform(
                            waveform.timings,
                            waveform.amplitudes,
                            waveform.repeatIndex,
                        ),
                    )
                } else {
                    resolvedVibrator.vibrate(
                        VibrationEffect.createWaveform(
                            waveform.timings,
                            waveform.repeatIndex,
                        ),
                    )
                }
            } else {
                @Suppress("DEPRECATION")
                resolvedVibrator.vibrate(waveform.timings, waveform.repeatIndex)
            }
        }
    }

    override fun stopPreview() {
        vibrator?.cancel()
    }

    private fun resolveVibrator(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}
