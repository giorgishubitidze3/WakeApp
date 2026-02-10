package com.spearson.wakeapp.timer.domain

interface TimerCompletedHistoryRepository {
    suspend fun getCompletedDurationsMillis(): Result<List<Long>>
    suspend fun upsertCompletedDurationMillis(durationMillis: Long): Result<List<Long>>
}
