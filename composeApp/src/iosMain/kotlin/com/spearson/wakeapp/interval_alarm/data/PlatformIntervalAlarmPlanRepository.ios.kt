package com.spearson.wakeapp.interval_alarm.data

import com.spearson.wakeapp.interval_alarm.domain.IntervalAlarmPlanRepository
import com.spearson.wakeapp.interval_alarm.domain.model.IntervalAlarmPlan
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import platform.Foundation.NSUserDefaults

class PlatformIntervalAlarmPlanRepository : IntervalAlarmPlanRepository {

    private val userDefaults = NSUserDefaults.standardUserDefaults
    private val json = Json {
        ignoreUnknownKeys = true
    }

    override suspend fun getPlans(): Result<List<IntervalAlarmPlan>> {
        return runCatching {
            val encodedPlans = userDefaults.stringForKey(storageKey()) ?: return@runCatching emptyList()
            json.decodeFromString(ListSerializer(IntervalAlarmPlan.serializer()), encodedPlans)
        }
    }

    override suspend fun upsertPlan(plan: IntervalAlarmPlan): Result<Unit> {
        return runCatching {
            val existingPlans = getPlans().getOrElse { emptyList() }
            val updatedPlans = buildList {
                addAll(existingPlans.filterNot { it.id == plan.id })
                add(plan)
            }.sortedBy { it.startTime.toMinutesOfDay() }
            val encodedPlans = json.encodeToString(ListSerializer(IntervalAlarmPlan.serializer()), updatedPlans)
            userDefaults.setObject(encodedPlans, forKey = storageKey())
        }
    }

    override suspend fun deletePlan(planId: String): Result<Unit> {
        return runCatching {
            val existingPlans = getPlans().getOrElse { emptyList() }
            val updatedPlans = existingPlans.filterNot { it.id == planId }
            val encodedPlans = json.encodeToString(ListSerializer(IntervalAlarmPlan.serializer()), updatedPlans)
            userDefaults.setObject(encodedPlans, forKey = storageKey())
        }
    }

    private fun storageKey(): String = "interval_plans"
}
