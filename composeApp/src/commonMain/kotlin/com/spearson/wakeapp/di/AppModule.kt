package com.spearson.wakeapp.di

import com.spearson.wakeapp.alarm_home.presentation.AlarmHomeViewModel
import com.spearson.wakeapp.interval_alarm.domain.GenerateAlarmOccurrencesUseCase
import com.spearson.wakeapp.interval_alarm.presentation.IntervalAlarmViewModel
import com.spearson.wakeapp.stopwatch.presentation.StopwatchViewModel
import com.spearson.wakeapp.timer.presentation.TimerViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

expect val platformAppModule: Module

val appModule = module {
    singleOf(::GenerateAlarmOccurrencesUseCase)
    viewModelOf(::AlarmHomeViewModel)
    viewModelOf(::IntervalAlarmViewModel)
    viewModelOf(::TimerViewModel)
    viewModelOf(::StopwatchViewModel)
    includes(platformAppModule)
}
