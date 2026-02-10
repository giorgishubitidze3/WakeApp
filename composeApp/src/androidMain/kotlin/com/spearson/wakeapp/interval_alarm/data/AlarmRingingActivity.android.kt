package com.spearson.wakeapp.interval_alarm.data

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spearson.wakeapp.core.theme.WakeAppTheme
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

class AlarmRingingActivity : ComponentActivity() {

    private var requestCode: Int = 0
    private var hour: Int = 7
    private var minute: Int = 0
    private var snoozeMinutes: Int = DEFAULT_SNOOZE_MINUTES
    private var planId: String? = null
    private var isSnooze: Boolean = false
    private var ringtoneId: String? = null
    private var ringtoneVolumePercent: Int = 100
    private var hapticsPatternName: String? = null
    private var hapticsOnly: Boolean = false
    private var hapticsEscalateOverTime: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureWindowForAlarm()
        consumeIntent(intent)
        renderContent()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        consumeIntent(intent)
        renderContent()
    }

    private fun consumeIntent(intent: Intent?) {
        requestCode = intent?.getIntExtra(EXTRA_REQUEST_CODE, 0) ?: 0
        hour = intent?.getIntExtra(EXTRA_HOUR, 7) ?: 7
        minute = intent?.getIntExtra(EXTRA_MINUTE, 0) ?: 0
        snoozeMinutes = intent?.getIntExtra(EXTRA_SNOOZE_MINUTES, DEFAULT_SNOOZE_MINUTES)
            ?.coerceAtLeast(1)
            ?: DEFAULT_SNOOZE_MINUTES
        planId = intent?.getStringExtra(EXTRA_PLAN_ID)
        isSnooze = intent?.getBooleanExtra(EXTRA_IS_SNOOZE, false) ?: false
        ringtoneId = intent?.getStringExtra(EXTRA_RINGTONE_ID)
        ringtoneVolumePercent = intent?.getIntExtra(EXTRA_RINGTONE_VOLUME_PERCENT, 100) ?: 100
        hapticsPatternName = intent?.getStringExtra(EXTRA_HAPTICS_PATTERN)
        hapticsOnly = intent?.getBooleanExtra(EXTRA_HAPTICS_ONLY, false) ?: false
        hapticsEscalateOverTime = intent?.getBooleanExtra(EXTRA_HAPTICS_ESCALATE_OVER_TIME, false) ?: false
    }

    private fun configureWindowForAlarm() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun renderContent() {
        setContent {
            WakeAppTheme {
                AlarmRingingScreen(
                    timeLabel = formatTime(hour, minute),
                    nextAlarmLabel = "Next alarm in $snoozeMinutes minutes",
                    onStopClick = {
                        AlarmAlertService.requestStop(
                            context = this,
                            requestCode = requestCode,
                            planId = planId,
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
                        finishAndRemoveTask()
                    },
                    onSnoozeClick = {
                        AlarmAlertService.requestSnooze(
                            context = this,
                            requestCode = requestCode,
                            planId = planId,
                            hour = hour,
                            minute = minute,
                            snoozeMinutes = snoozeMinutes,
                            ringtoneId = ringtoneId,
                            ringtoneVolumePercent = ringtoneVolumePercent,
                            hapticsPatternName = hapticsPatternName,
                            hapticsOnly = hapticsOnly,
                            hapticsEscalateOverTime = hapticsEscalateOverTime,
                        )
                        finishAndRemoveTask()
                    },
                )
            }
        }
    }

    private fun formatTime(hour24: Int, minute: Int): String {
        val hour12 = when (hour24 % 12) {
            0 -> 12
            else -> hour24 % 12
        }
        val suffix = if (hour24 < 12) "AM" else "PM"
        return "$hour12:${minute.toString().padStart(2, '0')}"
            .plus(" ")
            .plus(suffix)
    }

    companion object {
        fun createIntent(
            context: Context,
            requestCode: Int,
            hour: Int,
            minute: Int,
            snoozeMinutes: Int,
            planId: String?,
            isSnooze: Boolean,
            ringtoneId: String?,
            ringtoneVolumePercent: Int,
            hapticsPatternName: String?,
            hapticsOnly: Boolean,
            hapticsEscalateOverTime: Boolean,
        ): Intent {
            return Intent(context, AlarmRingingActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_REQUEST_CODE, requestCode)
                putExtra(EXTRA_HOUR, hour)
                putExtra(EXTRA_MINUTE, minute)
                putExtra(EXTRA_SNOOZE_MINUTES, snoozeMinutes)
                putExtra(EXTRA_IS_SNOOZE, isSnooze)
                planId?.let { putExtra(EXTRA_PLAN_ID, it) }
                ringtoneId?.let { putExtra(EXTRA_RINGTONE_ID, it) }
                putExtra(EXTRA_RINGTONE_VOLUME_PERCENT, ringtoneVolumePercent)
                hapticsPatternName?.let { putExtra(EXTRA_HAPTICS_PATTERN, it) }
                putExtra(EXTRA_HAPTICS_ONLY, hapticsOnly)
                putExtra(EXTRA_HAPTICS_ESCALATE_OVER_TIME, hapticsEscalateOverTime)
            }
        }
    }
}

@Composable
private fun AlarmRingingScreen(
    timeLabel: String,
    nextAlarmLabel: String,
    onStopClick: () -> Unit,
    onSnoozeClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2A5DA8),
                        Color(0xFF3D5B8E),
                        Color(0xFF4E4B57),
                    ),
                ),
            )
            .padding(horizontal = 22.dp, vertical = 26.dp),
    ) {
        Column(
            modifier = Modifier.align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                color = Color(0xFF5D81BD).copy(alpha = 0.35f),
                shape = MaterialTheme.shapes.large,
            ) {
                Text(
                    text = "WAKE UP CALL",
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFF5D4C8),
                    letterSpacing = 2.sp,
                )
            }
            Spacer(modifier = Modifier.height(200.dp))
            Text(
                text = timeLabel,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 86.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 88.sp,
                ),
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(22.dp))
            Surface(
                color = Color(0xFF5D81BD).copy(alpha = 0.35f),
                shape = MaterialTheme.shapes.large,
            ) {
                Text(
                    text = nextAlarmLabel,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF9DC4FF),
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Button(
                onClick = onStopClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E73F2),
                    contentColor = Color.White,
                ),
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Text(
                    text = "Stop",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                )
            }
            OutlinedButton(
                onClick = onSnoozeClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp),
                border = BorderStroke(
                    2.dp,
                    SolidColor(Color(0xFF72809A)),
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFB9C3D5),
                ),
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Text(
                    text = "Snooze",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
            }
            Text(
                text = "TAP TO ACT",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFB0B4BC).copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                letterSpacing = 1.4.sp,
            )
        }
    }
}
