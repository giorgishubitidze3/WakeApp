package com.spearson.wakeapp.core.presentation

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable
    data object Alarms: Route

    @Serializable
    data class CreateAlarm(
        val planId: String? = null,
    ): Route

    @Serializable
    data object Timer: Route

    @Serializable
    data object Stopwatch: Route
}
