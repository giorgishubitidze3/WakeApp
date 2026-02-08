package com.spearson.wakeapp.interval_alarm.domain

import com.spearson.wakeapp.interval_alarm.domain.model.IntervalAlarmPlan

interface IntervalAlarmPlanRepository {
    suspend fun getPlan(planId: String = IntervalAlarmPlan.DEFAULT_PLAN_ID): Result<IntervalAlarmPlan?>
    suspend fun upsertPlan(plan: IntervalAlarmPlan): Result<Unit>
}
