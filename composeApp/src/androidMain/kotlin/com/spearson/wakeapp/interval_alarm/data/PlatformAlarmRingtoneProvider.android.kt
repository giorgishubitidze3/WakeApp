package com.spearson.wakeapp.interval_alarm.data

import android.content.Context
import android.media.RingtoneManager
import com.spearson.wakeapp.interval_alarm.domain.AlarmRingtoneProvider
import com.spearson.wakeapp.interval_alarm.domain.model.AlarmRingtoneOption
import com.spearson.wakeapp.interval_alarm.domain.model.IntervalAlarmPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlatformAlarmRingtoneProvider(
    private val context: Context,
) : AlarmRingtoneProvider {

    override suspend fun getAvailableRingtones(): Result<List<AlarmRingtoneOption>> {
        return withContext(Dispatchers.IO) {
            cachedRingtones?.let { cached ->
                return@withContext Result.success(cached)
            }
            runCatching {
                val systemDefault = AlarmRingtoneOption(
                    id = IntervalAlarmPlan.DEFAULT_RINGTONE_ID,
                    name = IntervalAlarmPlan.DEFAULT_RINGTONE_NAME,
                )
                val alarmTones = queryRingtonesForType(RingtoneManager.TYPE_ALARM)
                val fallbackTones = queryRingtonesForType(RingtoneManager.TYPE_RINGTONE)
                (listOf(systemDefault) + alarmTones + fallbackTones)
                    .distinctBy(AlarmRingtoneOption::id)
                    .take(MAX_RINGTONES)
            }.mapCatching { tones ->
                if (tones.isEmpty()) {
                    throw IllegalStateException("No device ringtones found.")
                }
                synchronized(cacheLock) {
                    if (cachedRingtones == null) {
                        cachedRingtones = tones
                    }
                }
                tones
            }
        }
    }

    private fun queryRingtonesForType(type: Int): List<AlarmRingtoneOption> {
        val ringtoneManager = RingtoneManager(context).apply {
            setType(type)
        }
        val cursor = ringtoneManager.cursor ?: return emptyList()
        val options = mutableListOf<AlarmRingtoneOption>()
        cursor.use {
            while (it.moveToNext()) {
                val position = it.position
                val uri = ringtoneManager.getRingtoneUri(position) ?: continue
                val title = RingtoneManager
                    .getRingtone(context, uri)
                    ?.getTitle(context)
                    .takeUnless { name -> name.isNullOrBlank() }
                    ?: "Alarm tone ${options.size + 1}"
                options += AlarmRingtoneOption(
                    id = uri.toString(),
                    name = title,
                )
            }
        }
        return options
    }

    private companion object {
        const val MAX_RINGTONES = 24
        private val cacheLock = Any()
        @Volatile
        private var cachedRingtones: List<AlarmRingtoneOption>? = null
    }
}
