package com.spearson.wakeapp

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.spearson.wakeapp.di.initKoinAndroid

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        initKoinAndroid(applicationContext)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        requestAlarmCapabilitiesIfNeeded()

        setContent {
            App()
        }
    }

    private fun requestAlarmCapabilitiesIfNeeded() {
        requestNotificationPermissionIfNeeded()
        requestExactAlarmPermissionIfNeeded()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val hasNotificationPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (hasNotificationPermission) {
            Log.d(TAG, "POST_NOTIFICATIONS already granted.")
            return
        }

        Log.w(TAG, "Requesting POST_NOTIFICATIONS permission for alarm notifications.")
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            POST_NOTIFICATIONS_REQUEST_CODE,
        )
    }

    private fun requestExactAlarmPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (alarmManager.canScheduleExactAlarms()) {
            Log.d(TAG, "Exact alarm capability available.")
            return
        }

        Log.w(TAG, "Exact alarm capability missing, opening system settings.")
        runCatching {
            startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                },
            )
        }.onFailure { throwable ->
            Log.e(TAG, "Failed to open exact alarm permission screen.", throwable)
        }
    }

    private companion object {
        const val TAG = "WakeAppMain"
        const val POST_NOTIFICATIONS_REQUEST_CODE = 101
    }
}
