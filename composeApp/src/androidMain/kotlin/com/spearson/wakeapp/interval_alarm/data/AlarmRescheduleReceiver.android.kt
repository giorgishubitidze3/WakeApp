package com.spearson.wakeapp.interval_alarm.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.spearson.wakeapp.di.initKoinAndroid
import com.spearson.wakeapp.interval_alarm.domain.GenerateAlarmOccurrencesUseCase
import com.spearson.wakeapp.interval_alarm.domain.IntervalAlarmPlanRepository
import com.spearson.wakeapp.interval_alarm.domain.IntervalAlarmScheduler
import kotlinx.coroutines.runBlocking
import org.koin.core.context.GlobalContext

class AlarmRescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in rescheduleActions) return
        Log.d(TAG, "Received reschedule action=$action")

        initKoinAndroid(context.applicationContext)
        val pendingResult = goAsync()
        Thread {
            runCatching {
                val koin = GlobalContext.get()
                val repository = koin.get<IntervalAlarmPlanRepository>()
                val scheduler = koin.get<IntervalAlarmScheduler>()
                val generateAlarmOccurrencesUseCase = koin.get<GenerateAlarmOccurrencesUseCase>()

                runBlocking {
                    val plans = repository.getPlans().getOrNull().orEmpty()
                    Log.d(TAG, "Rescheduling ${plans.size} plans after action=$action")
                    plans.forEach { plan ->
                        if (plan.isEnabled) {
                            val occurrences = generateAlarmOccurrencesUseCase(plan)
                            scheduler.schedulePlan(plan, occurrences)
                        } else {
                            scheduler.cancelPlan(plan.id)
                        }
                    }
                }
            }.onFailure { throwable ->
                Log.e(TAG, "Failed to reschedule alarms for action=$action", throwable)
            }.also {
                pendingResult.finish()
            }
        }.start()
    }

    private companion object {
        const val TAG = "WakeAppReschedule"
        val rescheduleActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
        )
    }
}
