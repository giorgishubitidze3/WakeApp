package com.spearson.wakeapp.interval_alarm.data

import com.spearson.wakeapp.interval_alarm.domain.IntervalAlarmScheduler
import com.spearson.wakeapp.interval_alarm.domain.model.AlarmOccurrence
import com.spearson.wakeapp.interval_alarm.domain.model.IntervalAlarmPlan

class PlatformIntervalAlarmScheduler : IntervalAlarmScheduler {

    override suspend fun schedulePlan(
        plan: IntervalAlarmPlan,
        occurrences: List<AlarmOccurrence>,
    ): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun cancelPlan(planId: String): Result<Unit> {
        return Result.success(Unit)
    }
}
