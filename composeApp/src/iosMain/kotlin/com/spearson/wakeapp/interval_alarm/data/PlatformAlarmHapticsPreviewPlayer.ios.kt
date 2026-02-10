package com.spearson.wakeapp.interval_alarm.data

import com.spearson.wakeapp.interval_alarm.domain.AlarmHapticsPreviewPlayer
import com.spearson.wakeapp.interval_alarm.domain.model.HapticsPattern
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.AudioToolbox.AudioServicesPlaySystemSound
import platform.AudioToolbox.kSystemSoundID_Vibrate

class PlatformAlarmHapticsPreviewPlayer : AlarmHapticsPreviewPlayer {

    private val previewScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var previewJob: Job? = null

    override suspend fun playPreview(
        pattern: HapticsPattern,
        escalateOverTime: Boolean,
    ): Result<Unit> {
        return runCatching {
            stopPreview()
            if (pattern == HapticsPattern.NoneOff) return@runCatching
            val pulseDelays = buildPulseDelays(
                pattern = pattern,
                escalateOverTime = escalateOverTime,
            )
            previewJob = previewScope.launch {
                pulseDelays.forEachIndexed { index, delayMillis ->
                    if (delayMillis > 0L || index != 0) {
                        delay(delayMillis)
                    }
                    AudioServicesPlaySystemSound(kSystemSoundID_Vibrate)
                }
            }
        }
    }

    override fun stopPreview() {
        previewJob?.cancel()
        previewJob = null
    }

    private fun buildPulseDelays(
        pattern: HapticsPattern,
        escalateOverTime: Boolean,
    ): LongArray {
        val base = when (pattern) {
            HapticsPattern.NoneOff -> longArrayOf()
            HapticsPattern.GentlePulse -> longArrayOf(0L, 280L)
            HapticsPattern.Standard -> longArrayOf(0L, 220L, 220L)
            HapticsPattern.StrongBuzz -> longArrayOf(0L, 170L, 170L, 170L)
            HapticsPattern.Heartbeat -> longArrayOf(0L, 120L, 280L, 120L)
            HapticsPattern.RapidFire -> longArrayOf(0L, 110L, 110L, 110L, 110L, 110L)
        }
        if (!escalateOverTime || base.isEmpty()) return base
        return base + longArrayOf(90L, 90L, 90L)
    }
}
