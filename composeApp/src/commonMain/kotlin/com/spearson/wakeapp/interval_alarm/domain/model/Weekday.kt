package com.spearson.wakeapp.interval_alarm.domain.model

enum class Weekday(
    val shortLabel: String
) {
    Monday("Mon"),
    Tuesday("Tue"),
    Wednesday("Wed"),
    Thursday("Thu"),
    Friday("Fri"),
    Saturday("Sat"),
    Sunday("Sun");

    companion object {
        fun weekdays(): Set<Weekday> = setOf(
            Monday,
            Tuesday,
            Wednesday,
            Thursday,
            Friday,
        )
    }
}
