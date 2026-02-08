package com.spearson.wakeapp.interval_alarm.data

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.spearson.wakeapp.MainActivity
import com.spearson.wakeapp.R
import com.spearson.wakeapp.interval_alarm.data.android.EXTRA_HOUR
import com.spearson.wakeapp.interval_alarm.data.android.EXTRA_MINUTE
import com.spearson.wakeapp.interval_alarm.data.android.EXTRA_REQUEST_CODE
import com.spearson.wakeapp.interval_alarm.data.android.WAKE_ALARM_ACTION
import com.spearson.wakeapp.interval_alarm.data.android.WAKE_ALARM_CHANNEL_ID
import com.spearson.wakeapp.interval_alarm.data.android.WAKE_ALARM_CHANNEL_NAME

class AlarmTriggerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val requestCode = intent.getIntExtra(EXTRA_REQUEST_CODE, 0)
        val hour = intent.getIntExtra(EXTRA_HOUR, 7)
        val minute = intent.getIntExtra(EXTRA_MINUTE, 0)
        Log.d(TAG, "Alarm fired requestCode=$requestCode at ${formatTime(hour, minute)}")

        maybeShowNotification(
            context = context,
            requestCode = requestCode,
            hour = hour,
            minute = minute,
        )
        scheduleOneWeekLater(
            context = context,
            requestCode = requestCode,
            originalIntent = intent,
        )
    }

    private fun maybeShowNotification(
        context: Context,
        requestCode: Int,
        hour: Int,
        minute: Int,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionState = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            )
            if (permissionState != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "POST_NOTIFICATIONS not granted. Alarm notification suppressed.")
                return
            }
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                WAKE_ALARM_CHANNEL_ID,
                WAKE_ALARM_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "WakeApp interval alarms"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            requestCode,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentImmutableFlag(),
        )
        val title = "Wake up"
        val content = "Interval alarm at ${formatTime(hour, minute)}"

        val notification = NotificationCompat.Builder(context, WAKE_ALARM_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        NotificationManagerCompat.from(context).notify(requestCode, notification)
        Log.d(TAG, "Alarm notification posted for requestCode=$requestCode")
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
