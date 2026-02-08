package com.spearson.wakeapp.interval_alarm.data

import com.spearson.wakeapp.interval_alarm.domain.IntervalAlarmPlanRepository
import com.spearson.wakeapp.interval_alarm.domain.model.IntervalAlarmPlan

class InMemoryIntervalAlarmPlanRepository : IntervalAlarmPlanRepository {

    private var cachedPlan: IntervalAlarmPlan? = null

    override suspend fun getPlan(planId: String): Result<IntervalAlarmPlan?> {
        return Result.success(cachedPlan?.takeIf { it.id == planId })
    }

    override suspend fun upsertPlan(plan: IntervalAlarmPlan): Result<Unit> {
        cachedPlan = plan
        return Result.success(Unit)
    }
}
