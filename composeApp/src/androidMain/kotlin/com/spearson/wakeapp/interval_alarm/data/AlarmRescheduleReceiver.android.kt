package com.spearson.wakeapp.interval_alarm.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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

        initKoinAndroid(context.applicationContext)
        val pendingResult = goAsync()
        Thread {
            runCatching {
                val koin = GlobalContext.get()
                val repository = koin.get<IntervalAlarmPlanRepository>()
                val scheduler = koin.get<IntervalAlarmScheduler>()
                val generateAlarmOccurrencesUseCase = koin.get<GenerateAlarmOccurrencesUseCase>()

                runBlocking {
                    val plan = repository.getPlan().getOrNull() ?: return@runBlocking
                    if (plan.isEnabled) {
                        val occurrences = generateAlarmOccurrencesUseCase(plan)
                        scheduler.schedulePlan(plan, occurrences)
                    } else {
                        scheduler.cancelPlan(plan.id)
                    }
                }
            }.also {
                pendingResult.finish()
            }
        }.start()
    }

    private companion object {
        val rescheduleActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
        )
    }
}
