package com.spearson.wakeapp.interval_alarm.domain

import com.spearson.wakeapp.interval_alarm.domain.model.IntervalAlarmPlan
import com.spearson.wakeapp.interval_alarm.domain.model.TimeOfDay
import com.spearson.wakeapp.interval_alarm.domain.model.Weekday
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GenerateAlarmOccurrencesUseCaseTest {

    private val useCase = GenerateAlarmOccurrencesUseCase()

    @Test
    fun generateTimes_returnsExpectedTimesForFiveMinuteInterval() {
        val times = useCase.generateTimes(
            startTime = TimeOfDay(hour = 7, minute = 0),
            endTime = TimeOfDay(hour = 7, minute = 30),
            intervalMinutes = 5,
        )

        assertEquals(7, times.size)
        assertEquals(TimeOfDay(hour = 7, minute = 0), times.first())
        assertEquals(TimeOfDay(hour = 7, minute = 30), times.last())
    }

    @Test
    fun generateTimes_returnsEmptyListWhenRangeIsInvalid() {
        val times = useCase.generateTimes(
            startTime = TimeOfDay(hour = 8, minute = 0),
            endTime = TimeOfDay(hour = 7, minute = 0),
            intervalMinutes = 5,
        )

        assertTrue(times.isEmpty())
    }

    @Test
    fun invoke_expandsOccurrencesAcrossActiveDays() {
        val plan = IntervalAlarmPlan(
            startTime = TimeOfDay(hour = 7, minute = 0),
            endTime = TimeOfDay(hour = 7, minute = 10),
            intervalMinutes = 5,
            activeDays = setOf(Weekday.Monday, Weekday.Friday),
            isEnabled = true,
        )

        val occurrences = useCase(plan)

        assertEquals(6, occurrences.size)
        assertEquals(Weekday.Monday, occurrences.first().weekday)
        assertEquals(TimeOfDay(hour = 7, minute = 0), occurrences.first().time)
    }

    @Test
    fun invoke_returnsEmptyWhenPlanDisabled() {
        val plan = IntervalAlarmPlan(
            isEnabled = false,
            activeDays = setOf(Weekday.Monday),
        )

        val occurrences = useCase(plan)

        assertTrue(occurrences.isEmpty())
    }
}
