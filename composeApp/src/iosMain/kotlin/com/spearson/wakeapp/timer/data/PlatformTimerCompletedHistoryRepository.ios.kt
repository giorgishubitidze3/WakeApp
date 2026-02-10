package com.spearson.wakeapp.timer.data

import com.spearson.wakeapp.timer.domain.TimerCompletedHistoryRepository
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import platform.Foundation.NSUserDefaults

class PlatformTimerCompletedHistoryRepository : TimerCompletedHistoryRepository {

    private val userDefaults = NSUserDefaults.standardUserDefaults
    private val json = Json {
        ignoreUnknownKeys = true
    }

    override suspend fun getCompletedDurationsMillis(): Result<List<Long>> {
        return runCatching {
            val encodedDurations = userDefaults.stringForKey(STORAGE_KEY) ?: return@runCatching emptyList()
            json.decodeFromString(ListSerializer(Long.serializer()), encodedDurations)
                .filter { it >= MIN_DURATION_MILLIS }
                .distinct()
        }
    }

    override suspend fun upsertCompletedDurationMillis(durationMillis: Long): Result<List<Long>> {
        return runCatching {
            val resolvedDuration = durationMillis.coerceAtLeast(MIN_DURATION_MILLIS)
            val existingDurations = getCompletedDurationsMillis().getOrElse { emptyList() }
            val updatedDurations = buildList {
                add(resolvedDuration)
                addAll(existingDurations.filterNot { it == resolvedDuration })
            }.take(MAX_STORED_DURATIONS)
            val encodedDurations = json.encodeToString(
                serializer = ListSerializer(Long.serializer()),
                value = updatedDurations,
            )
            userDefaults.setObject(encodedDurations, forKey = STORAGE_KEY)
            updatedDurations
        }
    }

    private companion object {
        const val STORAGE_KEY = "completed_durations_ms"
        const val MIN_DURATION_MILLIS = 1_000L
        const val MAX_STORED_DURATIONS = 24
    }
}
