package com.spearson.wakeapp.interval_alarm.data

import com.spearson.wakeapp.interval_alarm.domain.IntervalAlarmPlanRepository
import com.spearson.wakeapp.interval_alarm.domain.model.IntervalAlarmPlan
import kotlinx.serialization.json.Json
import platform.Foundation.NSUserDefaults

class PlatformIntervalAlarmPlanRepository : IntervalAlarmPlanRepository {

    private val userDefaults = NSUserDefaults.standardUserDefaults
    private val json = Json {
        ignoreUnknownKeys = true
    }

    override suspend fun getPlan(planId: String): Result<IntervalAlarmPlan?> {
        return runCatching {
            val encodedPlan = userDefaults.stringForKey(storageKey(planId)) ?: return@runCatching null
            json.decodeFromString<IntervalAlarmPlan>(encodedPlan)
        }
    }

    override suspend fun upsertPlan(plan: IntervalAlarmPlan): Result<Unit> {
        return runCatching {
            val encodedPlan = json.encodeToString(plan)
            userDefaults.setObject(encodedPlan, forKey = storageKey(plan.id))
        }
    }

    private fun storageKey(planId: String): String = "interval_plan_$planId"
}
