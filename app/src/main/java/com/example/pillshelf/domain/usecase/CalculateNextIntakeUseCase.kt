package com.example.pillshelf.domain.usecase

import com.example.pillshelf.data.model.Medication
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class CalculateNextIntakeUseCase {

    /**
     * Calculates the next planned intake date and time for a medication.
     * Returns null if course is completed or no intake required.
     */
    fun calculateNextIntake(medication: Medication, lastIntake: LocalDateTime? = null): LocalDateTime? {
        val now = LocalDateTime.now()
        val referenceTime = lastIntake ?: now

        return when (medication.scheduleType) {
            "DAILY" -> calculateDailyIntake(medication, lastIntake, now)
            "EVERY_N_HOURS" -> {
                val interval = if (medication.intervalHours > 0) medication.intervalHours else 8
                if (lastIntake != null) {
                    val candidate = lastIntake.plusHours(interval.toLong())
                    if (candidate.isBefore(now)) now else candidate
                } else {
                    now
                }
            }
            "COURSE" -> calculateCourseIntake(medication, lastIntake, now)
            else -> calculateDailyIntake(medication, lastIntake, now)
        }
    }

    private fun calculateDailyIntake(
        medication: Medication,
        lastIntake: LocalDateTime?,
        now: LocalDateTime
    ): LocalDateTime {
        val times = medication.getTimeOfDayList()
        val targetTimes = times.map { slotToTime(it) }.sorted()

        if (targetTimes.isEmpty()) {
            return now.plusDays(1).withHour(8).withMinute(0).withSecond(0)
        }

        // If today has upcoming times, return the next one today
        val todayUpcoming = targetTimes.firstOrNull { it.isAfter(now.toLocalTime()) }
        if (todayUpcoming != null) {
            return now.with(todayUpcoming).withSecond(0)
        }

        // Otherwise, first time tomorrow
        return now.plusDays(1).with(targetTimes.first()).withSecond(0)
    }

    private fun calculateCourseIntake(
        medication: Medication,
        lastIntake: LocalDateTime?,
        now: LocalDateTime
    ): LocalDateTime? {
        val startDate = if (medication.courseStartDate.isNotBlank()) {
            try {
                LocalDate.parse(medication.courseStartDate, DateTimeFormatter.ISO_LOCAL_DATE)
            } catch (e: Exception) {
                LocalDate.now()
            }
        } else {
            LocalDate.now()
        }

        val courseEnd = startDate.plusDays(medication.courseDurationDays.toLong())
        if (LocalDate.now().isAfter(courseEnd)) {
            return null // Course completed
        }

        return calculateDailyIntake(medication, lastIntake, now)
    }

    fun slotToTime(slot: String): LocalTime {
        return when (slot.uppercase()) {
            "MORNING" -> LocalTime.of(8, 0)
            "AFTERNOON" -> LocalTime.of(13, 0)
            "EVENING" -> LocalTime.of(20, 0)
            "BEDTIME" -> LocalTime.of(22, 0)
            else -> LocalTime.of(8, 0)
        }
    }
}
