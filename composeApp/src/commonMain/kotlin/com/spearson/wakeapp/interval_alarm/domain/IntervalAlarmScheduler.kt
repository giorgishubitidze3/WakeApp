package com.spearson.wakeapp.interval_alarm.domain

import com.spearson.wakeapp.interval_alarm.domain.model.AlarmOccurrence
import com.spearson.wakeapp.interval_alarm.domain.model.IntervalAlarmPlan

interface IntervalAlarmScheduler {
    suspend fun schedulePlan(
        plan: IntervalAlarmPlan,
        occurrences: List<AlarmOccurrence>,
    ): Result<Unit>

    suspend fun cancelPlan(
        planId: String,
    ): Result<Unit>
}
