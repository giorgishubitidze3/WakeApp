package com.spearson.wakeapp.timer.data

import android.content.Context
import com.spearson.wakeapp.timer.domain.TimerCompletedHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PlatformTimerCompletedHistoryRepository : TimerCompletedHistoryRepository, KoinComponent {

    private val context: Context by inject()
    private val json = Json {
        ignoreUnknownKeys = true
    }
    private val storageMutex = Mutex()
    private val preferences by lazy {
        context.getSharedPreferences(TIMER_HISTORY_PREFS, Context.MODE_PRIVATE)
    }

    override suspend fun getCompletedDurationsMillis(): Result<List<Long>> {
        return withContext(Dispatchers.IO) {
            runCatching {
                storageMutex.withLock {
                    readDurationsLocked()
                }
            }
        }
    }

    override suspend fun upsertCompletedDurationMillis(durationMillis: Long): Result<List<Long>> {
        return withContext(Dispatchers.IO) {
            runCatching {
                storageMutex.withLock {
                    val resolvedDuration = durationMillis.coerceAtLeast(MIN_DURATION_MILLIS)
                    val existingDurations = readDurationsLocked()
                    val updatedDurations = buildList {
                        add(resolvedDuration)
                        addAll(existingDurations.filterNot { it == resolvedDuration })
                    }.take(MAX_STORED_DURATIONS)
                    writeDurationsLocked(updatedDurations)
                    updatedDurations
                }
            }
        }
    }

    private fun readDurationsLocked(): List<Long> {
        val encodedDurations = preferences.getString(STORAGE_KEY, null) ?: return emptyList()
        return json.decodeFromString(ListSerializer(Long.serializer()), encodedDurations)
            .filter { it >= MIN_DURATION_MILLIS }
            .distinct()
    }

    private fun writeDurationsLocked(durations: List<Long>) {
        val encodedDurations = json.encodeToString(ListSerializer(Long.serializer()), durations)
        preferences.edit()
            .putString(STORAGE_KEY, encodedDurations)
            .apply()
    }

    private companion object {
        const val TIMER_HISTORY_PREFS = "wake_timer_history_store"
        const val STORAGE_KEY = "completed_durations_ms"
        const val MIN_DURATION_MILLIS = 1_000L
        const val MAX_STORED_DURATIONS = 24
    }
}
