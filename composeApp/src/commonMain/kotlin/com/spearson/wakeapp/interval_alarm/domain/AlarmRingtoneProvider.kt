package com.spearson.wakeapp.interval_alarm.domain

import com.spearson.wakeapp.interval_alarm.domain.model.AlarmRingtoneOption

interface AlarmRingtoneProvider {
    suspend fun getAvailableRingtones(): Result<List<AlarmRingtoneOption>>
}
