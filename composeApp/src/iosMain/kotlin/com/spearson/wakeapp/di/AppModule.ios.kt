package com.spearson.wakeapp.di

import com.spearson.wakeapp.interval_alarm.data.PlatformIntervalAlarmPlanRepository
import com.spearson.wakeapp.interval_alarm.data.PlatformIntervalAlarmScheduler
import com.spearson.wakeapp.interval_alarm.data.PlatformAlarmRingtonePreviewPlayer
import com.spearson.wakeapp.interval_alarm.data.PlatformAlarmRingtoneProvider
import com.spearson.wakeapp.interval_alarm.data.PlatformAlarmHapticsPreviewPlayer
import com.spearson.wakeapp.interval_alarm.domain.AlarmHapticsPreviewPlayer
import com.spearson.wakeapp.interval_alarm.domain.AlarmRingtonePreviewPlayer
import com.spearson.wakeapp.interval_alarm.domain.AlarmRingtoneProvider
import com.spearson.wakeapp.interval_alarm.domain.IntervalAlarmPlanRepository
import com.spearson.wakeapp.interval_alarm.domain.IntervalAlarmScheduler
import com.spearson.wakeapp.timer.data.PlatformTimerCompletedHistoryRepository
import com.spearson.wakeapp.timer.domain.TimerCompletedHistoryRepository
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformAppModule: Module = module {
    singleOf(::PlatformIntervalAlarmPlanRepository) bind IntervalAlarmPlanRepository::class
    singleOf(::PlatformIntervalAlarmScheduler) bind IntervalAlarmScheduler::class
    singleOf(::PlatformAlarmRingtoneProvider) bind AlarmRingtoneProvider::class
    singleOf(::PlatformAlarmRingtonePreviewPlayer) bind AlarmRingtonePreviewPlayer::class
    singleOf(::PlatformAlarmHapticsPreviewPlayer) bind AlarmHapticsPreviewPlayer::class
    singleOf(::PlatformTimerCompletedHistoryRepository) bind TimerCompletedHistoryRepository::class
}
