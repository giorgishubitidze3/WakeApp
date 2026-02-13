package com.spearson.wakeapp.interval_alarm.data

import android.app.AlarmManager
import android.app.ActivityOptions
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.spearson.wakeapp.R
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
import com.spearson.wakeapp.interval_alarm.data.android.AlarmHapticsPatterns
import com.spearson.wakeapp.interval_alarm.data.android.WAKE_ALARM_ACTION
import com.spearson.wakeapp.interval_alarm.data.android.WAKE_ALARM_CHANNEL_ID
import com.spearson.wakeapp.interval_alarm.data.android.WAKE_ALARM_CHANNEL_NAME
import com.spearson.wakeapp.interval_alarm.domain.model.HapticsPattern
import java.util.Calendar

class AlarmAlertService : Service() {

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_ALARM
        val payload = AlarmPayload.fromIntent(intent)
        Log.d(TAG, "Alarm service action=$action requestCode=${payload.requestCode}")

        return when (action) {
            ACTION_STOP_ALARM -> {
                stopAlarmAndService()
                START_NOT_STICKY
            }

            ACTION_SNOOZE_ALARM -> {
                scheduleSnooze(payload)
                stopAlarmAndService()
                START_NOT_STICKY
            }

            ACTION_START_ALARM -> {
                startOrUpdateAlarm(payload)
                START_STICKY
            }

            else -> START_NOT_STICKY
        }
    }

    override fun onDestroy() {
        stopPlayback()
        super.onDestroy()
    }

    private fun startOrUpdateAlarm(payload: AlarmPayload) {
        createAlarmChannelIfNeeded()
        val notificationId = notificationIdFor(payload.requestCode)
        val shouldUseFullScreenUi = shouldUseFullScreenAlarmUi()
        val fullScreenIntent = buildFullScreenIntent(payload)
        val alarmNotification = buildAlarmNotification(
            payload = payload,
            fullScreenIntent = fullScreenIntent,
            useFullScreenUi = shouldUseFullScreenUi,
        )
        startForeground(notificationId, alarmNotification)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            Log.d(TAG, "Full-screen intent permission granted=${notificationManager.canUseFullScreenIntent()}")
        }
        startPlayback(payload)
        if (!shouldUseFullScreenUi) {
            Log.d(TAG, "Skipping full-screen alarm UI because device is interactive and unlocked.")
        }
    }

    private fun scheduleSnooze(payload: AlarmPayload) {
        val snoozeMinutes = payload.snoozeMinutes.coerceAtLeast(1)
        val triggerAtMillis = System.currentTimeMillis() + snoozeMinutes * MILLIS_PER_MINUTE
        val triggerCalendar = Calendar.getInstance().apply {
            timeInMillis = triggerAtMillis
        }
        val snoozeRequestCode = "${payload.requestCode}:$triggerAtMillis".hashCode()
        val snoozeIntent = Intent(this, AlarmTriggerReceiver::class.java).apply {
            action = WAKE_ALARM_ACTION
            payload.planId?.let { putExtra(EXTRA_PLAN_ID, it) }
            putExtra(EXTRA_REQUEST_CODE, snoozeRequestCode)
            putExtra(EXTRA_HOUR, triggerCalendar.get(Calendar.HOUR_OF_DAY))
            putExtra(EXTRA_MINUTE, triggerCalendar.get(Calendar.MINUTE))
            putExtra(EXTRA_SNOOZE_MINUTES, snoozeMinutes)
            putExtra(EXTRA_IS_SNOOZE, true)
            payload.ringtoneId?.let { putExtra(EXTRA_RINGTONE_ID, it) }
            putExtra(EXTRA_RINGTONE_VOLUME_PERCENT, payload.ringtoneVolumePercent)
            putExtra(EXTRA_HAPTICS_PATTERN, payload.hapticsPattern.name)
            putExtra(EXTRA_HAPTICS_ONLY, payload.hapticsOnly)
            putExtra(EXTRA_HAPTICS_ESCALATE_OVER_TIME, payload.hapticsEscalateOverTime)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            this,
            snoozeRequestCode,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentImmutableFlags(),
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                snoozePendingIntent,
            )
            Log.w(TAG, "Snooze scheduled inexactly for requestCode=${payload.requestCode}")
            return
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            snoozePendingIntent,
        )
        Log.d(TAG, "Snooze scheduled for requestCode=${payload.requestCode} after $snoozeMinutes minutes")
    }

    private fun buildAlarmNotification(
        payload: AlarmPayload,
        fullScreenIntent: PendingIntent,
        useFullScreenUi: Boolean,
    ): Notification {
        val stopIntent = PendingIntent.getService(
            this,
            payload.requestCode,
            buildServiceIntent(ACTION_STOP_ALARM, payload),
            PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentImmutableFlags(),
        )
        val snoozeIntent = PendingIntent.getService(
            this,
            payload.requestCode + SNOOZE_REQUEST_CODE_OFFSET,
            buildServiceIntent(ACTION_SNOOZE_ALARM, payload),
            PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentImmutableFlags(),
        )

        val builder = NotificationCompat.Builder(this, WAKE_ALARM_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("Wake up")
            .setContentText("Interval alarm at ${payload.formattedTime}")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setAutoCancel(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(android.R.drawable.ic_lock_idle_alarm, "Snooze", snoozeIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopIntent)

        if (useFullScreenUi) {
            builder
                .setFullScreenIntent(fullScreenIntent, true)
                .setContentIntent(fullScreenIntent)
        } else {
            builder.setContentIntent(fullScreenIntent)
        }

        return builder.build()
    }

    private fun buildFullScreenIntent(payload: AlarmPayload): PendingIntent {
        val ringingIntent = AlarmRingingActivity.createIntent(
            context = this,
            requestCode = payload.requestCode,
            hour = payload.hour,
            minute = payload.minute,
            snoozeMinutes = payload.snoozeMinutes,
            planId = payload.planId,
            isSnooze = payload.isSnooze,
            ringtoneId = payload.ringtoneId,
            ringtoneVolumePercent = payload.ringtoneVolumePercent,
            hapticsPatternName = payload.hapticsPattern.name,
            hapticsOnly = payload.hapticsOnly,
            hapticsEscalateOverTime = payload.hapticsEscalateOverTime,
        )
        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentImmutableFlags()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return PendingIntent.getActivity(
                this,
                payload.requestCode,
                ringingIntent,
                pendingIntentFlags,
            )
        }

        val creatorOptions = ActivityOptions.makeBasic().apply {
            setPendingIntentCreatorBackgroundActivityStartMode(
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS,
            )
        }
        return PendingIntent.getActivity(
            this,
            payload.requestCode,
            ringingIntent,
            pendingIntentFlags,
            creatorOptions.toBundle(),
        )
    }

    private fun buildServiceIntent(action: String, payload: AlarmPayload): Intent {
        return Intent(this, AlarmAlertService::class.java).apply {
            this.action = action
            payload.planId?.let { putExtra(EXTRA_PLAN_ID, it) }
            putExtra(EXTRA_REQUEST_CODE, payload.requestCode)
            putExtra(EXTRA_HOUR, payload.hour)
            putExtra(EXTRA_MINUTE, payload.minute)
            putExtra(EXTRA_SNOOZE_MINUTES, payload.snoozeMinutes)
            putExtra(EXTRA_IS_SNOOZE, payload.isSnooze)
            payload.ringtoneId?.let { putExtra(EXTRA_RINGTONE_ID, it) }
            putExtra(EXTRA_RINGTONE_VOLUME_PERCENT, payload.ringtoneVolumePercent)
            putExtra(EXTRA_HAPTICS_PATTERN, payload.hapticsPattern.name)
            putExtra(EXTRA_HAPTICS_ONLY, payload.hapticsOnly)
            putExtra(EXTRA_HAPTICS_ESCALATE_OVER_TIME, payload.hapticsEscalateOverTime)
        }
    }

    private fun createAlarmChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existingChannel = notificationManager.getNotificationChannel(WAKE_ALARM_CHANNEL_ID)
        if (existingChannel != null) return

        val channel = NotificationChannel(
            WAKE_ALARM_CHANNEL_ID,
            WAKE_ALARM_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "WakeApp critical alarm alerts"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            enableVibration(true)
            setSound(null, null)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun shouldUseFullScreenAlarmUi(): Boolean {
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val isInteractive = powerManager?.isInteractive == true
        val isKeyguardLocked = keyguardManager?.isKeyguardLocked == true
        return !isInteractive || isKeyguardLocked
    }

    private fun startPlayback(payload: AlarmPayload) {
        stopPlayback()
        startVibration(payload)

        if (payload.hapticsOnly) {
            Log.d(TAG, "Haptics-only mode active; skipping ringtone playback.")
            return
        }

        val configuredAlarmUri = payload.ringtoneId
            ?.takeUnless { it.isBlank() }
            ?.let { ringtoneId -> runCatching { Uri.parse(ringtoneId) }.getOrNull() }
            ?.takeIf { parsedUri -> !parsedUri.scheme.isNullOrBlank() }
        val alarmUri = configuredAlarmUri
            ?: RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        ringtone = RingtoneManager.getRingtone(this, alarmUri)?.apply {
            audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                isLooping = true
                val normalizedVolume =
                    payload.ringtoneVolumePercent.coerceIn(MIN_VOLUME_PERCENT, MAX_FREE_VOLUME_PERCENT) / 100f
                setVolume(normalizedVolume)
            }
            play()
        }

        if (ringtone == null) {
            Log.w(TAG, "Unable to start ringtone playback for alarm.")
        }
    }

    private fun startVibration(payload: AlarmPayload) {
        if (!AlarmHapticsPatterns.shouldVibrate(payload.hapticsPattern)) {
            return
        }
        val resolvedVibrator = resolveVibrator() ?: return
        if (!resolvedVibrator.hasVibrator()) return

        vibrator = resolvedVibrator
        val waveform = AlarmHapticsPatterns.buildAlarmWaveform(
            pattern = payload.hapticsPattern,
            escalateOverTime = payload.hapticsEscalateOverTime,
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

    private fun resolveVibrator(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private fun stopPlayback() {
        runCatching {
            ringtone?.stop()
        }
        ringtone = null

        val activeVibrator = vibrator
        if (activeVibrator != null) {
            activeVibrator.cancel()
        }
        vibrator = null
    }

    private fun stopAlarmAndService() {
        stopPlayback()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun notificationIdFor(requestCode: Int): Int {
        return ALARM_NOTIFICATION_ID_OFFSET + (requestCode and 0x0FFF_FFFF)
    }

    private fun pendingIntentImmutableFlags(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }
    }

    private data class AlarmPayload(
        val requestCode: Int,
        val hour: Int,
        val minute: Int,
        val snoozeMinutes: Int,
        val planId: String?,
        val isSnooze: Boolean,
        val ringtoneId: String?,
        val ringtoneVolumePercent: Int,
        val hapticsPattern: HapticsPattern,
        val hapticsOnly: Boolean,
        val hapticsEscalateOverTime: Boolean,
    ) {
        val formattedTime: String
            get() {
                val hour12 = when (hour % 12) {
                    0 -> 12
                    else -> hour % 12
                }
                val amPm = if (hour < 12) "AM" else "PM"
                return "$hour12:${minute.toString().padStart(2, '0')} $amPm"
            }

        companion object {
            fun fromIntent(intent: Intent?): AlarmPayload {
                return AlarmPayload(
                    requestCode = intent?.getIntExtra(EXTRA_REQUEST_CODE, 0) ?: 0,
                    hour = intent?.getIntExtra(EXTRA_HOUR, 7) ?: 7,
                    minute = intent?.getIntExtra(EXTRA_MINUTE, 0) ?: 0,
                    snoozeMinutes = intent?.getIntExtra(EXTRA_SNOOZE_MINUTES, DEFAULT_SNOOZE_MINUTES)
                        ?: DEFAULT_SNOOZE_MINUTES,
                    planId = intent?.getStringExtra(EXTRA_PLAN_ID),
                    isSnooze = intent?.getBooleanExtra(EXTRA_IS_SNOOZE, false) ?: false,
                    ringtoneId = intent?.getStringExtra(EXTRA_RINGTONE_ID),
                    ringtoneVolumePercent = intent?.getIntExtra(EXTRA_RINGTONE_VOLUME_PERCENT, 100) ?: 100,
                    hapticsPattern = parseHapticsPattern(intent?.getStringExtra(EXTRA_HAPTICS_PATTERN)),
                    hapticsOnly = intent?.getBooleanExtra(EXTRA_HAPTICS_ONLY, false) ?: false,
                    hapticsEscalateOverTime = intent?.getBooleanExtra(EXTRA_HAPTICS_ESCALATE_OVER_TIME, false) ?: false,
                )
            }

            private fun parseHapticsPattern(name: String?): HapticsPattern {
                return name
                    ?.let { encoded -> runCatching { HapticsPattern.valueOf(encoded) }.getOrNull() }
                    ?: HapticsPattern.GentlePulse
            }
        }
    }

    companion object {
        private const val TAG = "WakeAppAlarmService"
        private const val ACTION_START_ALARM = "com.spearson.wakeapp.alarm.START"
        private const val ACTION_STOP_ALARM = "com.spearson.wakeapp.alarm.STOP"
        private const val ACTION_SNOOZE_ALARM = "com.spearson.wakeapp.alarm.SNOOZE"
        private const val ALARM_NOTIFICATION_ID_OFFSET = 20_000
        private const val SNOOZE_REQUEST_CODE_OFFSET = 10_000_000
        private const val MILLIS_PER_MINUTE = 60_000L
        private const val MIN_VOLUME_PERCENT = 0
        private const val MAX_FREE_VOLUME_PERCENT = 100

        fun startAlarm(
            context: Context,
            planId: String?,
            requestCode: Int,
            hour: Int,
            minute: Int,
            snoozeMinutes: Int,
            isSnooze: Boolean,
            ringtoneId: String?,
            ringtoneVolumePercent: Int,
            hapticsPatternName: String?,
            hapticsOnly: Boolean,
            hapticsEscalateOverTime: Boolean,
        ) {
            val startIntent = Intent(context, AlarmAlertService::class.java).apply {
                action = ACTION_START_ALARM
                planId?.let { putExtra(EXTRA_PLAN_ID, it) }
                putExtra(EXTRA_REQUEST_CODE, requestCode)
                putExtra(EXTRA_HOUR, hour)
                putExtra(EXTRA_MINUTE, minute)
                putExtra(EXTRA_SNOOZE_MINUTES, snoozeMinutes)
                putExtra(EXTRA_IS_SNOOZE, isSnooze)
                ringtoneId?.let { putExtra(EXTRA_RINGTONE_ID, it) }
                putExtra(EXTRA_RINGTONE_VOLUME_PERCENT, ringtoneVolumePercent)
                hapticsPatternName?.let { putExtra(EXTRA_HAPTICS_PATTERN, it) }
                putExtra(EXTRA_HAPTICS_ONLY, hapticsOnly)
                putExtra(EXTRA_HAPTICS_ESCALATE_OVER_TIME, hapticsEscalateOverTime)
            }
            ContextCompat.startForegroundService(context, startIntent)
        }

        fun requestStop(
            context: Context,
            requestCode: Int,
            planId: String?,
            hour: Int,
            minute: Int,
            snoozeMinutes: Int,
            isSnooze: Boolean,
            ringtoneId: String?,
            ringtoneVolumePercent: Int,
            hapticsPatternName: String?,
            hapticsOnly: Boolean,
            hapticsEscalateOverTime: Boolean,
        ) {
            context.startService(
                Intent(context, AlarmAlertService::class.java).apply {
                    action = ACTION_STOP_ALARM
                    planId?.let { putExtra(EXTRA_PLAN_ID, it) }
                    putExtra(EXTRA_REQUEST_CODE, requestCode)
                    putExtra(EXTRA_HOUR, hour)
                    putExtra(EXTRA_MINUTE, minute)
                    putExtra(EXTRA_SNOOZE_MINUTES, snoozeMinutes)
                    putExtra(EXTRA_IS_SNOOZE, isSnooze)
                    ringtoneId?.let { putExtra(EXTRA_RINGTONE_ID, it) }
                    putExtra(EXTRA_RINGTONE_VOLUME_PERCENT, ringtoneVolumePercent)
                    hapticsPatternName?.let { putExtra(EXTRA_HAPTICS_PATTERN, it) }
                    putExtra(EXTRA_HAPTICS_ONLY, hapticsOnly)
                    putExtra(EXTRA_HAPTICS_ESCALATE_OVER_TIME, hapticsEscalateOverTime)
                },
            )
        }

        fun requestSnooze(
            context: Context,
            requestCode: Int,
            planId: String?,
            hour: Int,
            minute: Int,
            snoozeMinutes: Int,
            ringtoneId: String?,
            ringtoneVolumePercent: Int,
            hapticsPatternName: String?,
            hapticsOnly: Boolean,
            hapticsEscalateOverTime: Boolean,
        ) {
            context.startService(
                Intent(context, AlarmAlertService::class.java).apply {
                    action = ACTION_SNOOZE_ALARM
                    planId?.let { putExtra(EXTRA_PLAN_ID, it) }
                    putExtra(EXTRA_REQUEST_CODE, requestCode)
                    putExtra(EXTRA_HOUR, hour)
                    putExtra(EXTRA_MINUTE, minute)
                    putExtra(EXTRA_SNOOZE_MINUTES, snoozeMinutes)
                    putExtra(EXTRA_IS_SNOOZE, false)
                    ringtoneId?.let { putExtra(EXTRA_RINGTONE_ID, it) }
                    putExtra(EXTRA_RINGTONE_VOLUME_PERCENT, ringtoneVolumePercent)
                    hapticsPatternName?.let { putExtra(EXTRA_HAPTICS_PATTERN, it) }
                    putExtra(EXTRA_HAPTICS_ONLY, hapticsOnly)
                    putExtra(EXTRA_HAPTICS_ESCALATE_OVER_TIME, hapticsEscalateOverTime)
                },
            )
        }
    }
}
