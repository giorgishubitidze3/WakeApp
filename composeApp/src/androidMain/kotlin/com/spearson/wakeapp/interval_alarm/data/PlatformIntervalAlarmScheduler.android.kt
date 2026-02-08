package com.spearson.wakeapp.interval_alarm.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.spearson.wakeapp.interval_alarm.data.android.EXTRA_HOUR
import com.spearson.wakeapp.interval_alarm.data.android.EXTRA_MINUTE
import com.spearson.wakeapp.interval_alarm.data.android.EXTRA_PLAN_ID
import com.spearson.wakeapp.interval_alarm.data.android.EXTRA_REQUEST_CODE
import com.spearson.wakeapp.interval_alarm.data.android.SCHEDULER_PREFS
import com.spearson.wakeapp.interval_alarm.data.android.WAKE_ALARM_ACTION
import com.spearson.wakeapp.interval_alarm.domain.IntervalAlarmScheduler
import com.spearson.wakeapp.interval_alarm.domain.model.AlarmOccurrence
import com.spearson.wakeapp.interval_alarm.domain.model.IntervalAlarmPlan
import com.spearson.wakeapp.interval_alarm.domain.model.TimeOfDay
import com.spearson.wakeapp.interval_alarm.domain.model.Weekday
import java.util.Calendar
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PlatformIntervalAlarmScheduler : IntervalAlarmScheduler, KoinComponent {

    private val context: Context by inject()
    private val alarmManager: AlarmManager by lazy {
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }
    private val schedulerPreferences by lazy {
        context.getSharedPreferences(SCHEDULER_PREFS, Context.MODE_PRIVATE)
    }

    override suspend fun schedulePlan(
        plan: IntervalAlarmPlan,
        occurrences: List<AlarmOccurrence>,
    ): Result<Unit> {
        return runCatching {
            Log.d(TAG, "Scheduling plan=${plan.id}, enabled=${plan.isEnabled}, occurrences=${occurrences.size}")
            cancelPlan(plan.id).getOrThrow()
            if (!plan.isEnabled) return@runCatching

            val requestCodes = mutableSetOf<String>()
            occurrences.forEach { occurrence ->
                val requestCode = requestCodeFor(plan.id, occurrence)
                val triggerAtMillis = nextTriggerAtMillis(
                    weekday = occurrence.weekday,
                    time = occurrence.time,
                )
                val alarmIntent = buildAlarmIntent(
                    planId = plan.id,
                    requestCode = requestCode,
                    time = occurrence.time,
                )
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    alarmIntent,
                    pendingIntentMutableFlags(),
                )
                setExactAlarm(
                    triggerAtMillis = triggerAtMillis,
                    pendingIntent = pendingIntent,
                )
                Log.d(
                    TAG,
                    "Scheduled requestCode=$requestCode plan=${plan.id} weekday=${occurrence.weekday} " +
                        "time=${occurrence.time.hour}:${occurrence.time.minute.toString().padStart(2, '0')} " +
                        "triggerAt=$triggerAtMillis",
                )
                requestCodes.add(requestCode.toString())
            }

            schedulerPreferences.edit()
                .putStringSet(requestCodesKey(plan.id), requestCodes)
                .apply()
            Log.d(TAG, "Finished scheduling plan=${plan.id} codes=${requestCodes.size}")
            Unit
        }.onFailure { throwable ->
            Log.e(TAG, "Failed scheduling plan=${plan.id}", throwable)
        }
    }

    override suspend fun cancelPlan(planId: String): Result<Unit> {
        return runCatching {
            val requestCodes = schedulerPreferences
                .getStringSet(requestCodesKey(planId), emptySet())
                .orEmpty()

            requestCodes.forEach { requestCodeString ->
                val requestCode = requestCodeString.toIntOrNull() ?: return@forEach
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    buildBaseAlarmIntent(),
                    PendingIntent.FLAG_NO_CREATE or pendingIntentImmutableFlags(),
                )
                if (pendingIntent != null) {
                    alarmManager.cancel(pendingIntent)
                    pendingIntent.cancel()
                    Log.d(TAG, "Canceled alarm requestCode=$requestCode for plan=$planId")
                }
            }

            schedulerPreferences.edit()
                .remove(requestCodesKey(planId))
                .apply()
            Log.d(TAG, "Finished canceling plan=$planId count=${requestCodes.size}")
            Unit
        }.onFailure { throwable ->
            Log.e(TAG, "Failed canceling plan=$planId", throwable)
        }
    }

    private fun requestCodesKey(planId: String): String = "scheduled_codes_$planId"

    private fun requestCodeFor(planId: String, occurrence: AlarmOccurrence): Int {
        return "$planId:${occurrence.weekday.ordinal}:${occurrence.time.hour}:${occurrence.time.minute}".hashCode()
    }

    private fun buildBaseAlarmIntent(): Intent {
        return Intent(context, AlarmTriggerReceiver::class.java).apply {
            action = WAKE_ALARM_ACTION
        }
    }

    private fun buildAlarmIntent(
        planId: String,
        requestCode: Int,
        time: TimeOfDay,
    ): Intent {
        return buildBaseAlarmIntent().apply {
            putExtra(EXTRA_PLAN_ID, planId)
            putExtra(EXTRA_REQUEST_CODE, requestCode)
            putExtra(EXTRA_HOUR, time.hour)
            putExtra(EXTRA_MINUTE, time.minute)
        }
    }

    private fun nextTriggerAtMillis(
        weekday: Weekday,
        time: TimeOfDay,
    ): Long {
        val now = Calendar.getInstance()
        val trigger = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, time.hour)
            set(Calendar.MINUTE, time.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            val dayOffset = dayOffset(
                from = get(Calendar.DAY_OF_WEEK),
                to = weekday.toCalendarDayOfWeek(),
            )
            add(Calendar.DAY_OF_YEAR, dayOffset)
            if (timeInMillis <= now.timeInMillis) {
                add(Calendar.DAY_OF_YEAR, DAYS_IN_WEEK)
            }
        }
        return trigger.timeInMillis
    }

    private fun dayOffset(from: Int, to: Int): Int {
        var offset = to - from
        if (offset < 0) {
            offset += DAYS_IN_WEEK
        }
        return offset
    }

    private fun Weekday.toCalendarDayOfWeek(): Int {
        return when (this) {
            Weekday.Sunday -> Calendar.SUNDAY
            Weekday.Monday -> Calendar.MONDAY
            Weekday.Tuesday -> Calendar.TUESDAY
            Weekday.Wednesday -> Calendar.WEDNESDAY
            Weekday.Thursday -> Calendar.THURSDAY
            Weekday.Friday -> Calendar.FRIDAY
            Weekday.Saturday -> Calendar.SATURDAY
        }
    }

    private fun pendingIntentImmutableFlags(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }
    }

    private fun pendingIntentMutableFlags(): Int {
        return PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentImmutableFlags()
    }

    private fun setExactAlarm(
        triggerAtMillis: Long,
        pendingIntent: PendingIntent,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Log.w(TAG, "Exact alarm permission missing. Falling back to inexact alarm scheduling.")
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent,
            )
            return
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent,
        )
    }

    private companion object {
        const val DAYS_IN_WEEK = 7
        const val TAG = "WakeAppScheduler"
    }
}
