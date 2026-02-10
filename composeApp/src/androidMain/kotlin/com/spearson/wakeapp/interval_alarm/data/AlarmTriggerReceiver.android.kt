package com.spearson.wakeapp.interval_alarm.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.spearson.wakeapp.interval_alarm.data.android.DEFAULT_SNOOZE_MINUTES
import com.spearson.wakeapp.interval_alarm.data.android.EXTRA_HOUR
import com.spearson.wakeapp.interval_alarm.data.android.EXTRA_IS_SNOOZE
import com.spearson.wakeapp.interval_alarm.data.android.EXTRA_MINUTE
import com.spearson.wakeapp.interval_alarm.data.android.EXTRA_PLAN_ID
import com.spearson.wakeapp.interval_alarm.data.android.EXTRA_REQUEST_CODE
import com.spearson.wakeapp.interval_alarm.data.android.EXTRA_RINGTONE_ID
import com.spearson.wakeapp.interval_alarm.data.android.EXTRA_RINGTONE_VOLUME_PERCENT
import com.spearson.wakeapp.interval_alarm.data.android.EXTRA_HAPTICS_PATTERN
import com.spearson.wakeapp.interval_alarm.data.android.EXTRA_HAPTICS_ONLY
import com.spearson.wakeapp.interval_alarm.data.android.EXTRA_HAPTICS_ESCALATE_OVER_TIME
import com.spearson.wakeapp.interval_alarm.data.android.EXTRA_SNOOZE_MINUTES
import com.spearson.wakeapp.interval_alarm.data.android.WAKE_ALARM_ACTION

class AlarmTriggerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val requestCode = intent.getIntExtra(EXTRA_REQUEST_CODE, 0)
        val hour = intent.getIntExtra(EXTRA_HOUR, 7)
        val minute = intent.getIntExtra(EXTRA_MINUTE, 0)
        val planId = intent.getStringExtra(EXTRA_PLAN_ID)
        val snoozeMinutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, DEFAULT_SNOOZE_MINUTES)
        val isSnooze = intent.getBooleanExtra(EXTRA_IS_SNOOZE, false)
        val ringtoneId = intent.getStringExtra(EXTRA_RINGTONE_ID)
        val ringtoneVolumePercent = intent.getIntExtra(EXTRA_RINGTONE_VOLUME_PERCENT, 100)
        val hapticsPatternName = intent.getStringExtra(EXTRA_HAPTICS_PATTERN)
        val hapticsOnly = intent.getBooleanExtra(EXTRA_HAPTICS_ONLY, false)
        val hapticsEscalateOverTime = intent.getBooleanExtra(EXTRA_HAPTICS_ESCALATE_OVER_TIME, false)
        Log.d(TAG, "Alarm fired requestCode=$requestCode at ${formatTime(hour, minute)}")

        runCatching {
            AlarmAlertService.startAlarm(
                context = context,
                planId = planId,
                requestCode = requestCode,
                hour = hour,
                minute = minute,
                snoozeMinutes = snoozeMinutes,
                isSnooze = isSnooze,
                ringtoneId = ringtoneId,
                ringtoneVolumePercent = ringtoneVolumePercent,
                hapticsPatternName = hapticsPatternName,
                hapticsOnly = hapticsOnly,
                hapticsEscalateOverTime = hapticsEscalateOverTime,
            )
        }.onFailure { throwable ->
            Log.e(TAG, "Unable to start alarm foreground service for requestCode=$requestCode", throwable)
            runCatching {
                val fallbackIntent = AlarmRingingActivity.createIntent(
                    context = context,
                    requestCode = requestCode,
                    hour = hour,
                    minute = minute,
                    snoozeMinutes = snoozeMinutes,
                    planId = planId,
                    isSnooze = isSnooze,
                    ringtoneId = ringtoneId,
                    ringtoneVolumePercent = ringtoneVolumePercent,
                    hapticsPatternName = hapticsPatternName,
                    hapticsOnly = hapticsOnly,
                    hapticsEscalateOverTime = hapticsEscalateOverTime,
                )
                context.startActivity(fallbackIntent)
            }.onFailure { fallbackError ->
                Log.e(TAG, "Fallback alarm activity launch failed for requestCode=$requestCode", fallbackError)
            }
        }

        if (!isSnooze) {
            scheduleOneWeekLater(
                context = context,
                requestCode = requestCode,
                originalIntent = intent,
            )
        }
    }

    private fun scheduleOneWeekLater(
        context: Context,
        requestCode: Int,
        originalIntent: Intent,
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(originalIntent).apply {
                action = WAKE_ALARM_ACTION
                setClass(context, AlarmTriggerReceiver::class.java)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentImmutableFlag(),
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Log.w(TAG, "Exact alarm permission missing. Falling back to inexact weekly reschedule.")
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + ONE_WEEK_MILLIS,
                pendingIntent,
            )
            return
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + ONE_WEEK_MILLIS,
            pendingIntent,
        )
        Log.d(TAG, "Rescheduled weekly alarm requestCode=$requestCode")
    }

    private fun formatTime(hour24: Int, minute: Int): String {
        val hour12 = when (hour24 % 12) {
            0 -> 12
            else -> hour24 % 12
        }
        val amPm = if (hour24 < 12) "AM" else "PM"
        return "$hour12:${minute.toString().padStart(2, '0')} $amPm"
    }

    private fun pendingIntentImmutableFlag(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }
    }

    private companion object {
        const val ONE_WEEK_MILLIS = 7L * 24L * 60L * 60L * 1000L
        const val TAG = "WakeAppAlarmTrigger"
    }
}
