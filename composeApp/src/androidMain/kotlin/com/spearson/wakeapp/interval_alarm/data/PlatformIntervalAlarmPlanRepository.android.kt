package com.spearson.wakeapp.interval_alarm.data

import android.content.Context
import com.spearson.wakeapp.interval_alarm.data.android.PLAN_PREFS
import com.spearson.wakeapp.interval_alarm.domain.IntervalAlarmPlanRepository
import com.spearson.wakeapp.interval_alarm.domain.model.IntervalAlarmPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
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
    private val repositoryMutex = Mutex()

    override suspend fun getPlans(): Result<List<IntervalAlarmPlan>> {
        return withContext(Dispatchers.IO) {
            runCatching {
                repositoryMutex.withLock {
                    readPlansLocked()
                }
            }
        }
    }

    override suspend fun upsertPlan(plan: IntervalAlarmPlan): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                repositoryMutex.withLock {
                    val existingPlans = readPlansLocked()
                    val updatedPlans = buildList {
                        addAll(existingPlans.filterNot { it.id == plan.id })
                        add(plan)
                    }.sortedBy { it.startTime.toMinutesOfDay() }
                    writePlansLocked(updatedPlans)
                }
            }
        }
    }

    override suspend fun deletePlan(planId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                repositoryMutex.withLock {
                    val existingPlans = readPlansLocked()
                    val updatedPlans = existingPlans.filterNot { it.id == planId }
                    writePlansLocked(updatedPlans)
                }
            }
        }
    }

    private fun storageKey(): String = "interval_plans"

    private fun readPlansLocked(): List<IntervalAlarmPlan> {
        val encodedPlans = preferences.getString(storageKey(), null) ?: return emptyList()
        return json.decodeFromString(ListSerializer(IntervalAlarmPlan.serializer()), encodedPlans)
    }

    private fun writePlansLocked(plans: List<IntervalAlarmPlan>) {
        val encodedPlans = json.encodeToString(ListSerializer(IntervalAlarmPlan.serializer()), plans)
        preferences.edit()
            .putString(storageKey(), encodedPlans)
            .apply()
    }
}
