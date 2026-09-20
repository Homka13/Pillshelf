package com.example.pillshelf.domain.usecase

import com.example.pillshelf.data.model.Medication
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class CalculateNextIntakeUseCase {

    /**
     * Calculates the next planned intake date and time for a medication.
     * Returns null if course is completed, as-needed, or no intake required.
     */
    fun calculateNextIntake(
        medication: Medication,
        lastIntake: LocalDateTime? = null,
        currentTime: LocalDateTime = LocalDateTime.now()
    ): LocalDateTime? {
        if (medication.remainingQuantity <= 0) {
            return null // No reminder if medication is out of stock
        }

        return when (medication.scheduleType.uppercase()) {
            "AS_NEEDED" -> null // As needed (PRN) has no scheduled reminder
            "DAILY" -> calculateDailyIntake(medication, lastIntake, currentTime)
            "EVERY_N_HOURS" -> calculateIntervalIntake(medication, lastIntake, currentTime)
            "COURSE" -> calculateCourseIntake(medication, lastIntake, currentTime)
            else -> calculateDailyIntake(medication, lastIntake, currentTime)
        }
    }

    private fun calculateIntervalIntake(
        medication: Medication,
        lastIntake: LocalDateTime?,
        now: LocalDateTime
    ): LocalDateTime {
        val interval = if (medication.intervalHours > 0) medication.intervalHours else 8
        return if (lastIntake != null) {
            val candidate = lastIntake.plusHours(interval.toLong())
            // If the candidate time has already passed, return candidate so caller knows it's due/overdue
            candidate
        } else {
            // Default first intake: current time + interval, or 08:00 today if in future
            val todayMorning = now.withHour(8).withMinute(0).withSecond(0).withNano(0)
            if (now.isBefore(todayMorning)) {
                todayMorning
            } else {
                now.plusHours(interval.toLong())
            }
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
            return now.plusDays(1).withHour(8).withMinute(0).withSecond(0).withNano(0)
        }

        // If taken today, only consider slots after lastIntake
        val minTimeToday = if (lastIntake != null && lastIntake.toLocalDate() == now.toLocalDate()) {
            if (lastIntake.toLocalTime().isAfter(now.toLocalTime())) lastIntake.toLocalTime() else now.toLocalTime()
        } else {
            now.toLocalTime()
        }

        // If today has upcoming times, return the next one today
        val todayUpcoming = targetTimes.firstOrNull { it.isAfter(minTimeToday) }
        if (todayUpcoming != null) {
            return now.with(todayUpcoming).withSecond(0).withNano(0)
        }

        // Otherwise, first scheduled time tomorrow
        return now.plusDays(1).with(targetTimes.first()).withSecond(0).withNano(0)
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
        if (now.toLocalDate().isAfter(courseEnd)) {
            return null // Course completed
        }

        return if (medication.intervalHours > 0) {
            calculateIntervalIntake(medication, lastIntake, now)
        } else {
            calculateDailyIntake(medication, lastIntake, now)
        }
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
