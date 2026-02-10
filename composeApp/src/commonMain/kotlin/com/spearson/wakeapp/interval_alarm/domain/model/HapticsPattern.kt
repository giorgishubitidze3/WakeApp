package com.spearson.wakeapp.interval_alarm.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class HapticsPattern {
    NoneOff,
    GentlePulse,
    Standard,
    StrongBuzz,
    Heartbeat,
    RapidFire,
}
