package com.spearson.wakeapp.interval_alarm.data

import com.spearson.wakeapp.interval_alarm.domain.GenerateAlarmOccurrencesUseCase
import com.spearson.wakeapp.interval_alarm.domain.IntervalAlarmPlanRepository
import com.spearson.wakeapp.interval_alarm.domain.IntervalAlarmScheduler
import com.spearson.wakeapp.interval_alarm.domain.model.AlarmOccurrence
import com.spearson.wakeapp.interval_alarm.domain.model.IntervalAlarmPlan
import com.spearson.wakeapp.interval_alarm.domain.model.TimeOfDay
import com.spearson.wakeapp.interval_alarm.domain.model.Weekday
import kotlinx.cinterop.BetaInteropApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitSecond
import platform.Foundation.NSCalendarUnitWeekday
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSClassFromString
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.NSLog
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIApplicationWillEnterForegroundNotification
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

class PlatformIntervalAlarmScheduler(
    private val intervalAlarmPlanRepository: IntervalAlarmPlanRepository,
    private val generateAlarmOccurrencesUseCase: GenerateAlarmOccurrencesUseCase,
) : IntervalAlarmScheduler {

    private val notificationCenter = UNUserNotificationCenter.currentNotificationCenter()
    private val calendar = NSCalendar.currentCalendar
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val syncMutex = Mutex()

    init {
        NSNotificationCenter.defaultCenter.addObserverForName(
            name = UIApplicationWillEnterForegroundNotification,
            `object` = null,
            queue = null,
        ) {
            syncScope.launch {
                syncAllPlans().onFailure {
                    NSLog("WakeApp: failed to sync alarms on foreground: ${it.message}")
                }
            }
        }

        syncScope.launch {
            syncAllPlans().onFailure {
                NSLog("WakeApp: failed to sync alarms on scheduler init: ${it.message}")
            }
        }
    }

    override suspend fun schedulePlan(
        plan: IntervalAlarmPlan,
        occurrences: List<AlarmOccurrence>,
    ): Result<Unit> {
        return syncAllPlans()
    }

    override suspend fun cancelPlan(planId: String): Result<Unit> {
        return syncAllPlans()
    }

    private suspend fun syncAllPlans(): Result<Unit> {
        return runCatching {
            syncMutex.withLock {
                if (requestNativeBridgeSync()) {
                    NSLog("WakeApp: delegated scheduling to native iOS alarm engine bridge.")
                    return@withLock
                }
                throw IllegalStateException(
                    "WakeAppAlarmEngineBridge unavailable. AlarmKit-only mode cannot schedule alarms.",
                )
            }
        }.onFailure { throwable ->
            NSLog("WakeApp: scheduler sync failed: ${throwable.message}")
        }
    }
    
    @OptIn(BetaInteropApi::class)
    private fun requestNativeBridgeSync(): Boolean {
        val hasNativeBridge =
            NSClassFromString("WakeAppAlarmEngineBridge") != null ||
                NSClassFromString("iosApp.WakeAppAlarmEngineBridge") != null
        if (!hasNativeBridge) return false

        NSNotificationCenter.defaultCenter.postNotificationName(
            aName = NATIVE_SYNC_NOTIFICATION_NAME,
            `object` = null,
        )
        return true
    }

    private suspend fun ensureNotificationPermission(): Boolean {
        val status = currentAuthorizationStatus() ?: return false
        return when (status) {
            UNAuthorizationStatusAuthorized,
            UNAuthorizationStatusProvisional,
            UNAuthorizationStatusEphemeral,
            -> true

            UNAuthorizationStatusDenied -> false
            else -> requestNotificationPermission()
        }
    }

    private suspend fun currentAuthorizationStatus(): Long? {
        return suspendCancellableCoroutine { continuation ->
            notificationCenter.getNotificationSettingsWithCompletionHandler { settings ->
                continuation.resume(settings?.authorizationStatus)
            }
        }
    }

    private suspend fun requestNotificationPermission(): Boolean {
        return suspendCancellableCoroutine { continuation ->
            notificationCenter.requestAuthorizationWithOptions(
                options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge,
            ) { granted, _ ->
                continuation.resume(granted)
            }
        }
    }

    private fun buildUpcomingOccurrences(
        plans: List<IntervalAlarmPlan>,
        now: NSDate,
    ): List<QueuedOccurrence> {
        val nowComponents = calendar.components(
            unitFlags = NSCalendarUnitHour or NSCalendarUnitMinute,
            fromDate = now,
        )
        val nowMinutesOfDay = nowComponents.hour.toInt() * MINUTES_PER_HOUR + nowComponents.minute.toInt()

        return buildList {
            plans.forEach { plan ->
                val intervalTimes = generateAlarmOccurrencesUseCase.generateTimes(
                    startTime = plan.startTime,
                    endTime = plan.endTime,
                    intervalMinutes = plan.intervalMinutes,
                )
                if (intervalTimes.isEmpty()) return@forEach

                repeat(MAX_QUEUE_DAYS) { dayOffset ->
                    val dayDate = calendar.dateByAddingUnit(
                        unit = NSCalendarUnitDay,
                        value = dayOffset.toLong(),
                        toDate = now,
                        options = 0u,
                    ) ?: return@repeat

                    val weekday = dayDate.toWeekday() ?: return@repeat
                    if (weekday !in plan.activeDays) return@repeat

                    intervalTimes.forEach { time ->
                        val triggerDate = buildTriggerDate(
                            dayDate = dayDate,
                            time = time,
                        ) ?: return@forEach

                        if (dayOffset == 0 && time.toMinutesOfDay() <= nowMinutesOfDay) return@forEach
                        add(
                            QueuedOccurrence(
                                planId = plan.id,
                                time = time,
                                fireDate = triggerDate,
                                dayOffset = dayOffset,
                            ),
                        )
                    }
                }
            }
        }.sortedWith(
            compareBy<QueuedOccurrence> { it.dayOffset }
                .thenBy { it.time.toMinutesOfDay() }
                .thenBy { it.planId },
        )
    }

    private fun selectQueueWindow(candidates: List<QueuedOccurrence>): List<QueuedOccurrence> {
        if (candidates.isEmpty()) return emptyList()

        var selectedDays = 1
        for (candidateDays in 1..MAX_QUEUE_DAYS) {
            val candidateCount = candidates.count { it.dayOffset < candidateDays }
            if (candidateCount <= MAX_PENDING_NOTIFICATIONS) {
                selectedDays = candidateDays
            } else {
                break
            }
        }

        val filteredByWindow = candidates.filter { it.dayOffset < selectedDays }
        return if (filteredByWindow.size <= MAX_PENDING_NOTIFICATIONS) {
            filteredByWindow
        } else {
            filteredByWindow.take(MAX_PENDING_NOTIFICATIONS)
        }
    }

    private fun buildTriggerDate(
        dayDate: NSDate,
        time: TimeOfDay,
    ): NSDate? {
        val dayComponents = calendar.components(
            unitFlags = NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay,
            fromDate = dayDate,
        )
        val triggerComponents = NSDateComponents().apply {
            year = dayComponents.year
            month = dayComponents.month
            day = dayComponents.day
            hour = time.hour.toLong()
            minute = time.minute.toLong()
            second = 0
        }
        return calendar.dateFromComponents(triggerComponents)
    }

    private suspend fun clearWakeRequests() {
        val requestIds = wakeRequestIdentifiers()
        if (requestIds.isNotEmpty()) {
            notificationCenter.removePendingNotificationRequestsWithIdentifiers(requestIds)
        }
    }

    private suspend fun wakeRequestIdentifiers(): List<String> {
        return suspendCancellableCoroutine { continuation ->
            notificationCenter.getPendingNotificationRequestsWithCompletionHandler { requests ->
                val ids = requests
                    ?.mapNotNull { request ->
                        val notificationRequest = request as? UNNotificationRequest ?: return@mapNotNull null
                        notificationRequest.identifier.takeIf { it.startsWith(REQUEST_PREFIX) }
                    }
                    .orEmpty()
                continuation.resume(ids)
            }
        }
    }

    private suspend fun scheduleNotification(occurrence: QueuedOccurrence) {
        NSLog(
            "WakeApp: queue notification plan=${occurrence.planId} dayOffset=${occurrence.dayOffset} " +
                "time=${occurrence.time.toUiLabel()}",
        )
        val content = UNMutableNotificationContent()
        content.setTitle("WakeApp Alarm")
        content.setBody("Interval alarm at ${occurrence.time.toUiLabel()}")
        content.setSound(UNNotificationSound.defaultSound)

        val triggerDateComponents = calendar.components(
            unitFlags = NSCalendarUnitYear or
                NSCalendarUnitMonth or
                NSCalendarUnitDay or
                NSCalendarUnitHour or
                NSCalendarUnitMinute or
                NSCalendarUnitSecond,
            fromDate = occurrence.fireDate,
        ).apply {
            hour = occurrence.time.hour.toLong()
            minute = occurrence.time.minute.toLong()
            second = 0
        }

        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
            dateComponents = triggerDateComponents,
            repeats = false,
        )
        val requestId = requestIdFor(occurrence)
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = requestId,
            content = content,
            trigger = trigger,
        )

        val addResult = suspendCancellableCoroutine<Result<Unit>> { continuation ->
            notificationCenter.addNotificationRequest(request) { error ->
                if (error != null) {
                    continuation.resume(Result.failure(IllegalStateException(error.localizedDescription)))
                } else {
                    continuation.resume(Result.success(Unit))
                }
            }
        }
        addResult.getOrThrow()
    }

    private fun requestIdFor(occurrence: QueuedOccurrence): String {
        val hourLabel = occurrence.time.hour.toString().padStart(2, '0')
        val minuteLabel = occurrence.time.minute.toString().padStart(2, '0')
        return "${REQUEST_PREFIX}_${occurrence.planId}_${occurrence.dayOffset}_${hourLabel}${minuteLabel}"
    }

    private fun NSDate.toWeekday(): Weekday? {
        val weekdayNumber = calendar.component(NSCalendarUnitWeekday, fromDate = this).toInt()
        return when (weekdayNumber) {
            1 -> Weekday.Sunday
            2 -> Weekday.Monday
            3 -> Weekday.Tuesday
            4 -> Weekday.Wednesday
            5 -> Weekday.Thursday
            6 -> Weekday.Friday
            7 -> Weekday.Saturday
            else -> null
        }
    }

    private fun TimeOfDay.toUiLabel(): String {
        val hour12 = when (hour % 12) {
            0 -> 12
            else -> hour % 12
        }
        val minuteLabel = minute.toString().padStart(2, '0')
        val suffix = if (hour < 12) "AM" else "PM"
        return "$hour12:$minuteLabel $suffix"
    }

    private data class QueuedOccurrence(
        val planId: String,
        val time: TimeOfDay,
        val fireDate: NSDate,
        val dayOffset: Int,
    )

    private companion object {
        const val MAX_PENDING_NOTIFICATIONS = 64
        const val MAX_QUEUE_DAYS = 7
        const val MINUTES_PER_HOUR = 60
        const val REQUEST_PREFIX = "wakeapp_interval"
        const val NATIVE_SYNC_NOTIFICATION_NAME = "wakeapp.alarm.sync.requested"
    }
}
