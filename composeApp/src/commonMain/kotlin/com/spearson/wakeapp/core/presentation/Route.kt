package com.spearson.wakeapp.core.presentation

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable
    data object Alarms: Route

    @Serializable
    data object CreateAlarm: Route

    @Serializable
    data object Stats: Route

    @Serializable
    data object Settings: Route
}
