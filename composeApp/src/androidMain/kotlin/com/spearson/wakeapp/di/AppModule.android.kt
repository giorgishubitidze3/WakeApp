package com.spearson.wakeapp.di

import com.spearson.wakeapp.interval_alarm.data.PlatformIntervalAlarmPlanRepository
import com.spearson.wakeapp.interval_alarm.data.PlatformIntervalAlarmScheduler
import com.spearson.wakeapp.interval_alarm.domain.IntervalAlarmPlanRepository
import com.spearson.wakeapp.interval_alarm.domain.IntervalAlarmScheduler
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformAppModule: Module = module {
    singleOf(::PlatformIntervalAlarmPlanRepository) bind IntervalAlarmPlanRepository::class
    singleOf(::PlatformIntervalAlarmScheduler) bind IntervalAlarmScheduler::class
}
