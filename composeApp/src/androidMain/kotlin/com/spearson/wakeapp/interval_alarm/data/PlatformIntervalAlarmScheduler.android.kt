package com.spearson.wakeapp.interval_alarm.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
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
import com.spearson.wakeapp.interval_alarm.data.android.SCHEDULER_PREFS
import com.spearson.wakeapp.interval_alarm.data.android.WAKE_ALARM_ACTION
import com.spearson.wakeapp.interval_alarm.data.android.WAKE_ALARM_MAINTENANCE_ACTION
import com.spearson.wakeapp.interval_alarm.domain.IntervalAlarmScheduler
import com.spearson.wakeapp.interval_alarm.domain.model.AlarmOccurrence
import com.spearson.wakeapp.interval_alarm.domain.model.IntervalAlarmPlan
import com.spearson.wakeapp.interval_alarm.domain.model.TimeOfDay
import com.spearson.wakeapp.interval_alarm.domain.model.Weekday
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
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
    private val schedulerMutex = Mutex()

    override suspend fun schedulePlan(
        plan: IntervalAlarmPlan,
        occurrences: List<AlarmOccurrence>,
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                schedulerMutex.withLock {
                    schedulePlanLocked(plan, occurrences)
                }
            }.onFailure { throwable ->
                Log.e(TAG, "Failed scheduling plan=${plan.id}", throwable)
            }
        }
    }

    override suspend fun cancelPlan(planId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                schedulerMutex.withLock {
                    cancelPlanLocked(planId)
                }
            }.onFailure { throwable ->
                Log.e(TAG, "Failed canceling plan=$planId", throwable)
            }
        }
    }

    private fun schedulePlanLocked(
        plan: IntervalAlarmPlan,
        occurrences: List<AlarmOccurrence>,
    ) {
        Log.d(TAG, "Scheduling plan=${plan.id}, enabled=${plan.isEnabled}, occurrences=${occurrences.size}")
        cancelPlanLocked(plan.id)
        if (!plan.isEnabled) {
            Log.d(TAG, "Plan=${plan.id} disabled. Schedule skipped after cancel.")
            cancelMaintenanceSyncIfIdleLocked()
            return
        }

        val scheduledCandidates = selectCandidatesForRollingWindow(
            planId = plan.id,
            occurrences = occurrences,
        )
        val requestCodes = HashSet<String>(scheduledCandidates.size)
        var scheduleErrors = 0
        for (candidate in scheduledCandidates) {
            val requestCode = requestCodeFor(plan.id, candidate.occurrence)
            val alarmIntent = buildAlarmIntent(
                planId = plan.id,
                requestCode = requestCode,
                time = candidate.occurrence.time,
                snoozeMinutes = plan.snoozeMinutes,
                ringtoneId = plan.ringtoneId,
                ringtoneVolumePercent = plan.ringtoneVolumePercent,
                hapticsPattern = plan.hapticsPattern.name,
                hapticsOnly = plan.hapticsOnly,
                hapticsEscalateOverTime = plan.hapticsEscalateOverTime,
            )
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                alarmIntent,
                pendingIntentMutableFlags(),
            )
            val scheduled = trySetExactAlarm(
                triggerAtMillis = candidate.triggerAtMillis,
                pendingIntent = pendingIntent,
            )
            if (!scheduled) {
                scheduleErrors++
                Log.w(
                    TAG,
                    "Unable to schedule alarm for plan=${plan.id} requestCode=$requestCode. " +
                        "Stopping this sync to keep existing scheduled set consistent.",
                )
                break
            }
            requestCodes.add(requestCode.toString())
        }

        schedulerPreferences.edit()
            .putStringSet(requestCodesKey(plan.id), requestCodes)
            .apply()

        if (requestCodes.isNotEmpty()) {
            scheduleMaintenanceSyncLocked()
        } else {
            cancelMaintenanceSyncIfIdleLocked()
        }

        Log.d(
            TAG,
            "Finished scheduling plan=${plan.id} codes=${requestCodes.size} " +
                "requested=${occurrences.size} queued=${scheduledCandidates.size} errors=$scheduleErrors",
        )
    }

    private fun cancelPlanLocked(planId: String) {
        val requestCodes = schedulerPreferences
            .getStringSet(requestCodesKey(planId), emptySet())
            .orEmpty()

        var canceledCount = 0
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
                canceledCount++
            }
        }
        if (requestCodes.isEmpty()) {
            canceledCount += cancelLegacyOrphanAlarms(planId)
        }

        schedulerPreferences.edit()
            .remove(requestCodesKey(planId))
            .apply()
        cancelMaintenanceSyncIfIdleLocked()
        Log.d(
            TAG,
            "Finished canceling plan=$planId canceled=$canceledCount storedCodes=${requestCodes.size}",
        )
    }

    private fun cancelLegacyOrphanAlarms(planId: String): Int {
        var canceledCount = 0
        for (weekdayOrdinal in 0 until DAYS_IN_WEEK) {
            for (hour in 0 until HOURS_IN_DAY) {
                for (minute in 0 until MINUTES_IN_HOUR) {
                    val legacyRequestCode = "$planId:$weekdayOrdinal:$hour:$minute".hashCode()
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        legacyRequestCode,
                        buildBaseAlarmIntent(),
                        PendingIntent.FLAG_NO_CREATE or pendingIntentImmutableFlags(),
                    )
                    if (pendingIntent != null) {
                        alarmManager.cancel(pendingIntent)
                        pendingIntent.cancel()
                        canceledCount++
                    }
                }
            }
        }
        if (canceledCount > 0) {
            Log.d(TAG, "Canceled legacy orphan alarms for plan=$planId count=$canceledCount")
        }
        return canceledCount
    }

    private fun selectCandidatesForRollingWindow(
        planId: String,
        occurrences: List<AlarmOccurrence>,
    ): List<ScheduledCandidate> {
        if (occurrences.isEmpty()) return emptyList()

        val budget = availableAlarmSlotsForPlan(planId).coerceAtMost(MAX_SCHEDULED_PER_PLAN)
        if (budget <= 0) {
            Log.w(TAG, "No alarm slots available for plan=$planId. Will retry on next maintenance sync.")
            return emptyList()
        }

        val candidates = occurrences
            .asSequence()
            .map { occurrence ->
                ScheduledCandidate(
                    occurrence = occurrence,
                    triggerAtMillis = nextTriggerAtMillis(
                        weekday = occurrence.weekday,
                        time = occurrence.time,
                    ),
                )
            }
            .sortedBy(ScheduledCandidate::triggerAtMillis)
            .toList()

        if (candidates.size <= budget) return candidates

        Log.w(
            TAG,
            "Rolling window active for plan=$planId. budget=$budget total=${candidates.size}.",
        )
        return candidates.take(budget)
    }

    private fun availableAlarmSlotsForPlan(planId: String): Int {
        val usedByOtherPlans = schedulerPreferences.all.entries.sumOf { entry ->
            if (!entry.key.startsWith(REQUEST_CODES_KEY_PREFIX) || entry.key == requestCodesKey(planId)) {
                return@sumOf 0
            }
            (entry.value as? Set<*>)?.size ?: 0
        }
        return (MAX_TOTAL_SCHEDULED_ALARMS - RESERVED_INTERNAL_ALARMS - usedByOtherPlans)
            .coerceAtLeast(0)
    }

    private fun cancelMaintenanceSyncIfIdleLocked() {
        val hasTrackedPlanAlarms = schedulerPreferences.all.entries.any { entry ->
            entry.key.startsWith(REQUEST_CODES_KEY_PREFIX) &&
                ((entry.value as? Set<*>)?.isNotEmpty() == true)
        }
        if (hasTrackedPlanAlarms) return

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            MAINTENANCE_SYNC_REQUEST_CODE,
            buildMaintenanceIntent(),
            PendingIntent.FLAG_NO_CREATE or pendingIntentImmutableFlags(),
        ) ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        Log.d(TAG, "Canceled maintenance sync alarm because no tracked plan alarms remain.")
    }

    private fun scheduleMaintenanceSyncLocked() {
        val maintenancePendingIntent = PendingIntent.getBroadcast(
            context,
            MAINTENANCE_SYNC_REQUEST_CODE,
            buildMaintenanceIntent(),
            pendingIntentMutableFlags(),
        )
        val triggerAtMillis = System.currentTimeMillis() + MAINTENANCE_SYNC_INTERVAL_MILLIS
        val scheduled = trySetExactAlarm(
            triggerAtMillis = triggerAtMillis,
            pendingIntent = maintenancePendingIntent,
        )
        if (scheduled) {
            Log.d(TAG, "Scheduled maintenance sync at=$triggerAtMillis")
        } else {
            Log.w(TAG, "Skipped maintenance sync scheduling due to alarm cap/availability limits.")
        }
    }

    private fun requestCodesKey(planId: String): String = "$REQUEST_CODES_KEY_PREFIX$planId"

    private fun requestCodeFor(planId: String, occurrence: AlarmOccurrence): Int {
        return "$planId:${occurrence.weekday.ordinal}:${occurrence.time.hour}:${occurrence.time.minute}".hashCode()
    }

    private fun buildBaseAlarmIntent(): Intent {
        return Intent(context, AlarmTriggerReceiver::class.java).apply {
            action = WAKE_ALARM_ACTION
        }
    }

    private fun buildMaintenanceIntent(): Intent {
        return Intent(context, AlarmRescheduleReceiver::class.java).apply {
            action = WAKE_ALARM_MAINTENANCE_ACTION
        }
    }

    private fun buildAlarmIntent(
        planId: String,
        requestCode: Int,
        time: TimeOfDay,
        snoozeMinutes: Int,
        ringtoneId: String,
        ringtoneVolumePercent: Int,
        hapticsPattern: String,
        hapticsOnly: Boolean,
        hapticsEscalateOverTime: Boolean,
    ): Intent {
        return buildBaseAlarmIntent().apply {
            putExtra(EXTRA_PLAN_ID, planId)
            putExtra(EXTRA_REQUEST_CODE, requestCode)
            putExtra(EXTRA_HOUR, time.hour)
            putExtra(EXTRA_MINUTE, time.minute)
            putExtra(EXTRA_SNOOZE_MINUTES, snoozeMinutes)
            putExtra(EXTRA_IS_SNOOZE, false)
            putExtra(EXTRA_RINGTONE_ID, ringtoneId)
            putExtra(EXTRA_RINGTONE_VOLUME_PERCENT, ringtoneVolumePercent)
            putExtra(EXTRA_HAPTICS_PATTERN, hapticsPattern)
            putExtra(EXTRA_HAPTICS_ONLY, hapticsOnly)
            putExtra(EXTRA_HAPTICS_ESCALATE_OVER_TIME, hapticsEscalateOverTime)
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

    private fun trySetExactAlarm(
        triggerAtMillis: Long,
        pendingIntent: PendingIntent,
    ): Boolean {
        val result = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "Exact alarm permission missing. Falling back to inexact alarm scheduling.")
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent,
                )
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent,
                )
            }
        }

        result.exceptionOrNull()?.let { throwable ->
            if (throwable.isAlarmLimitException()) {
                Log.w(TAG, "Alarm registration skipped: concurrent alarm cap reached.")
            } else {
                Log.e(TAG, "Failed registering alarm at=$triggerAtMillis", throwable)
            }
        }
        return result.isSuccess
    }

    private fun Throwable.isAlarmLimitException(): Boolean {
        if (this !is IllegalStateException) return false
        val message = message ?: return false
        return message.contains("Maximum limit of concurrent alarms", ignoreCase = true)
    }

    private companion object {
        const val DAYS_IN_WEEK = 7
        const val REQUEST_CODES_KEY_PREFIX = "scheduled_codes_"
        const val MAX_TOTAL_SCHEDULED_ALARMS = 450
        const val RESERVED_INTERNAL_ALARMS = 8
        const val MAX_SCHEDULED_PER_PLAN = 450
        const val MAINTENANCE_SYNC_REQUEST_CODE = 0x57A90
        const val MAINTENANCE_SYNC_INTERVAL_MILLIS = 4L * 60L * 60L * 1000L
        const val HOURS_IN_DAY = 24
        const val MINUTES_IN_HOUR = 60
        const val TAG = "WakeAppScheduler"
    }

    private data class ScheduledCandidate(
        val occurrence: AlarmOccurrence,
        val triggerAtMillis: Long,
    )
}
