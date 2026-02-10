package com.spearson.wakeapp.interval_alarm.data.android

import com.spearson.wakeapp.interval_alarm.domain.model.HapticsPattern
import kotlin.math.roundToInt

internal data class AlarmHapticsWaveform(
    val timings: LongArray,
    val amplitudes: IntArray,
    val repeatIndex: Int,
)

internal object AlarmHapticsPatterns {

    fun shouldVibrate(pattern: HapticsPattern): Boolean = pattern != HapticsPattern.NoneOff

    fun buildPreviewWaveform(
        pattern: HapticsPattern,
        escalateOverTime: Boolean,
    ): AlarmHapticsWaveform {
        return buildWaveform(
            pattern = pattern,
            escalateOverTime = escalateOverTime,
            repeatForAlarm = false,
        )
    }

    fun buildAlarmWaveform(
        pattern: HapticsPattern,
        escalateOverTime: Boolean,
    ): AlarmHapticsWaveform {
        return buildWaveform(
            pattern = pattern,
            escalateOverTime = escalateOverTime,
            repeatForAlarm = true,
        )
    }

    private fun buildWaveform(
        pattern: HapticsPattern,
        escalateOverTime: Boolean,
        repeatForAlarm: Boolean,
    ): AlarmHapticsWaveform {
        val base = basePattern(pattern)
        if (!escalateOverTime) {
            return AlarmHapticsWaveform(
                timings = base.timings,
                amplitudes = base.amplitudes,
                repeatIndex = if (repeatForAlarm) 0 else NO_REPEAT,
            )
        }

        val levels = floatArrayOf(0.45f, 0.7f, 1.0f)
        val combinedTimings = LongArray(base.timings.size * levels.size)
        val combinedAmplitudes = IntArray(base.amplitudes.size * levels.size)
        var cursor = 0
        levels.forEach { level ->
            base.timings.forEachIndexed { index, duration ->
                combinedTimings[cursor + index] = duration
                val amplitude = base.amplitudes[index]
                combinedAmplitudes[cursor + index] =
                    if (amplitude == 0) 0 else (amplitude * level).roundToInt().coerceIn(1, MAX_AMPLITUDE)
            }
            cursor += base.timings.size
        }

        val repeatIndex = if (repeatForAlarm) base.timings.size * (levels.size - 1) else NO_REPEAT
        return AlarmHapticsWaveform(
            timings = combinedTimings,
            amplitudes = combinedAmplitudes,
            repeatIndex = repeatIndex,
        )
    }

    private fun basePattern(pattern: HapticsPattern): AlarmHapticsWaveform {
        return when (pattern) {
            HapticsPattern.NoneOff -> AlarmHapticsWaveform(
                timings = longArrayOf(0L, 40L),
                amplitudes = intArrayOf(0, 0),
                repeatIndex = NO_REPEAT,
            )

            HapticsPattern.GentlePulse -> AlarmHapticsWaveform(
                timings = longArrayOf(0L, 110L, 170L, 110L, 200L),
                amplitudes = intArrayOf(0, 80, 0, 95, 0),
                repeatIndex = NO_REPEAT,
            )

            HapticsPattern.Standard -> AlarmHapticsWaveform(
                timings = longArrayOf(0L, 180L, 140L, 180L, 180L),
                amplitudes = intArrayOf(0, 130, 0, 150, 0),
                repeatIndex = NO_REPEAT,
            )

            HapticsPattern.StrongBuzz -> AlarmHapticsWaveform(
                timings = longArrayOf(0L, 280L, 120L, 280L, 150L),
                amplitudes = intArrayOf(0, 190, 0, 215, 0),
                repeatIndex = NO_REPEAT,
            )

            HapticsPattern.Heartbeat -> AlarmHapticsWaveform(
                timings = longArrayOf(0L, 90L, 70L, 170L, 260L, 90L, 220L),
                amplitudes = intArrayOf(0, 130, 0, 170, 0, 145, 0),
                repeatIndex = NO_REPEAT,
            )

            HapticsPattern.RapidFire -> AlarmHapticsWaveform(
                timings = longArrayOf(0L, 65L, 60L, 65L, 60L, 65L, 60L, 65L, 160L),
                amplitudes = intArrayOf(0, 120, 0, 150, 0, 170, 0, 190, 0),
                repeatIndex = NO_REPEAT,
            )
        }
    }

    private const val NO_REPEAT = -1
    private const val MAX_AMPLITUDE = 255
}
