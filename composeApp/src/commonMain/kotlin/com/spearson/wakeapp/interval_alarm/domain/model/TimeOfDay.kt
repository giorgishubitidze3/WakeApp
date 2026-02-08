package com.spearson.wakeapp.interval_alarm.domain.model

data class TimeOfDay(
    val hour: Int,
    val minute: Int,
) {
    init {
        require(hour in 0..23) { "Hour must be between 0 and 23." }
        require(minute in 0..59) { "Minute must be between 0 and 59." }
    }

    fun toMinutesOfDay(): Int = hour * MINUTES_PER_HOUR + minute

    companion object {
        private const val MINUTES_PER_HOUR = 60
        private const val MINUTES_PER_DAY = 1_440

        fun fromMinutesOfDay(minutesOfDay: Int): TimeOfDay {
            val normalizedMinutes = ((minutesOfDay % MINUTES_PER_DAY) + MINUTES_PER_DAY) % MINUTES_PER_DAY
            return TimeOfDay(
                hour = normalizedMinutes / MINUTES_PER_HOUR,
                minute = normalizedMinutes % MINUTES_PER_HOUR,
            )
        }
    }
}
