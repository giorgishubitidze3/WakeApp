package com.spearson.wakeapp.interval_alarm.data

import android.content.Context
import com.spearson.wakeapp.interval_alarm.data.android.PLAN_PREFS
import com.spearson.wakeapp.interval_alarm.domain.IntervalAlarmPlanRepository
import com.spearson.wakeapp.interval_alarm.domain.model.IntervalAlarmPlan
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PlatformIntervalAlarmPlanRepository : IntervalAlarmPlanRepository, KoinComponent {

    private val context: Context by inject()
    private val json = Json {
        ignoreUnknownKeys = true
    }
    private val preferences by lazy {
        context.getSharedPreferences(PLAN_PREFS, Context.MODE_PRIVATE)
    }

    override suspend fun getPlans(): Result<List<IntervalAlarmPlan>> {
        return runCatching {
            val encodedPlans = preferences.getString(storageKey(), null) ?: return@runCatching emptyList()
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
            preferences.edit()
                .putString(storageKey(), encodedPlans)
                .apply()
        }
    }

    override suspend fun deletePlan(planId: String): Result<Unit> {
        return runCatching {
            val existingPlans = getPlans().getOrElse { emptyList() }
            val updatedPlans = existingPlans.filterNot { it.id == planId }
            val encodedPlans = json.encodeToString(ListSerializer(IntervalAlarmPlan.serializer()), updatedPlans)
            preferences.edit()
                .putString(storageKey(), encodedPlans)
                .apply()
        }
    }

    private fun storageKey(): String = "interval_plans"
}
