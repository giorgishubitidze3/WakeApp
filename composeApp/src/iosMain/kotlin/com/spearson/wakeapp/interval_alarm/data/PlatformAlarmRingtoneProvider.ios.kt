package com.spearson.wakeapp.interval_alarm.data

import com.spearson.wakeapp.interval_alarm.domain.AlarmRingtoneProvider
import com.spearson.wakeapp.interval_alarm.domain.model.AlarmRingtoneOption

class PlatformAlarmRingtoneProvider : AlarmRingtoneProvider {
    override suspend fun getAvailableRingtones(): Result<List<AlarmRingtoneOption>> {
        return Result.success(
            listOf(
                AlarmRingtoneOption(id = "system_default_alarm", name = "Morning Breeze"),
                AlarmRingtoneOption(id = "forest_birds", name = "Forest Birds"),
                AlarmRingtoneOption(id = "gentle_rain", name = "Gentle Rain"),
                AlarmRingtoneOption(id = "piano_dreams", name = "Piano Dreams"),
                AlarmRingtoneOption(id = "ocean_waves", name = "Ocean Waves"),
            ),
        )
    }
}
