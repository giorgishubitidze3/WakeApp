package com.spearson.wakeapp.interval_alarm.domain

import com.spearson.wakeapp.interval_alarm.domain.model.AlarmOccurrence
import com.spearson.wakeapp.interval_alarm.domain.model.IntervalAlarmPlan
import com.spearson.wakeapp.interval_alarm.domain.model.TimeOfDay
import com.spearson.wakeapp.interval_alarm.domain.model.Weekday

class GenerateAlarmOccurrencesUseCase {

    operator fun invoke(plan: IntervalAlarmPlan): List<AlarmOccurrence> {
        if (!plan.isEnabled || plan.intervalMinutes <= 0 || plan.activeDays.isEmpty()) {
            return emptyList()
        }

        val times = generateTimes(
            startTime = plan.startTime,
            endTime = plan.endTime,
            intervalMinutes = plan.intervalMinutes,
        )

        return plan.activeDays
            .sortedBy(Weekday::ordinal)
            .flatMap { weekday ->
                times.map { time ->
                    AlarmOccurrence(
                        weekday = weekday,
                        time = time,
                    )
                }
            }
    }

    fun generateTimes(
        startTime: TimeOfDay,
        endTime: TimeOfDay,
        intervalMinutes: Int,
    ): List<TimeOfDay> {
        if (intervalMinutes <= 0) return emptyList()

        val startMinutes = startTime.toMinutesOfDay()
        val endMinutes = endTime.toMinutesOfDay()
        if (startMinutes > endMinutes) return emptyList()

        return buildList {
            var current = startMinutes
            while (current <= endMinutes) {
                add(TimeOfDay.fromMinutesOfDay(current))
                current += intervalMinutes
            }
        }
    }
}
