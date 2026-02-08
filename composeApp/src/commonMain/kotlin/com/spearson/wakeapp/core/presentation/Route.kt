package com.spearson.wakeapp.core.presentation

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable
    data object IntervalAlarm: Route
}
