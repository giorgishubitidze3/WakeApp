package com.spearson.wakeapp.interval_alarm.domain

import com.spearson.wakeapp.interval_alarm.domain.model.IntervalAlarmPlan

interface IntervalAlarmPlanRepository {
    suspend fun getPlans(): Result<List<IntervalAlarmPlan>>
    suspend fun upsertPlan(plan: IntervalAlarmPlan): Result<Unit>
    suspend fun deletePlan(planId: String): Result<Unit>
}
