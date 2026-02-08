package com.spearson.wakeapp.interval_alarm.data

import android.content.Context
import com.spearson.wakeapp.interval_alarm.data.android.PLAN_PREFS
import com.spearson.wakeapp.interval_alarm.domain.IntervalAlarmPlanRepository
import com.spearson.wakeapp.interval_alarm.domain.model.IntervalAlarmPlan
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

    override suspend fun getPlan(planId: String): Result<IntervalAlarmPlan?> {
        return runCatching {
            val encodedPlan = preferences.getString(storageKey(planId), null) ?: return@runCatching null
            json.decodeFromString<IntervalAlarmPlan>(encodedPlan)
        }
    }

    override suspend fun upsertPlan(plan: IntervalAlarmPlan): Result<Unit> {
        return runCatching {
            val encodedPlan = json.encodeToString(plan)
            preferences.edit()
                .putString(storageKey(plan.id), encodedPlan)
                .apply()
        }
    }

    private fun storageKey(planId: String): String = "interval_plan_$planId"
}
