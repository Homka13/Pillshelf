package com.example.pillshelf.domain.usecase

import com.example.pillshelf.data.model.Medication
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class CalculateNextIntakeUseCase {

    companion object {
        const val AS_NEEDED = "AS_NEEDED"
    }

    /**
     * Calculates the next planned intake date and time for a medication.
     * Returns null if course is completed or no intake required.
     *
     * @param now точка відліку; параметр для детермінованих тестів,
     *            у продакшені використовується поточний час.
     */
    fun calculateNextIntake(
        medication: Medication,
        lastIntake: LocalDateTime? = null,
        now: LocalDateTime = LocalDateTime.now()
    ): LocalDateTime? {
        // AS_NEEDED (за потреби) — не планується: прийом фіксується вручну,
        // у журналі, без розкладу і без нагадувань.
        if (medication.scheduleType.equals(AS_NEEDED, ignoreCase = true)) return null

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

    /**
     * Скільки разів на день за розкладом має бути прийом (0 — не планується).
     * Використовується для позначення прострочених доз у графіку на сьогодні.
     */
    fun dosesPerDay(medication: Medication): Int {
        if (medication.scheduleType.equals(AS_NEEDED, ignoreCase = true)) return 0
        return when (medication.scheduleType) {
            "EVERY_N_HOURS" -> {
                val interval = if (medication.intervalHours > 0) medication.intervalHours else 8
                if (interval > 0) (24 / interval).coerceAtLeast(1) else 3
            }
            "COURSE" -> if (isCourseActive(medication, LocalDate.now())) medication.getTimeOfDayList().size else 0
            "DAILY" -> medication.getTimeOfDayList().size
            else -> medication.getTimeOfDayList().size
        }
    }

    fun isCourseActive(medication: Medication, today: LocalDate): Boolean {
        val startDate = parseCourseStart(medication)
        return !today.isAfter(startDate.plusDays(medication.courseDurationDays.toLong()))
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

        // Якщо останній прийом був пізніше за час найближчого слота сьогодні —
        // цей слот уже відпрацьований, наступний прийом завтра (для DAILY/COURSE).
        val upcomingToday = targetTimes.filter { it.isAfter(now.toLocalTime()) }
        if (lastIntake != null && lastIntake.toLocalDate() == now.toLocalDate()) {
            val consumedSlot = targetTimes.lastOrNull { !lastIntake.toLocalTime().isBefore(it) }
            if (consumedSlot != null) {
                val remaining = upcomingToday.filter { it.isAfter(consumedSlot) }
                if (remaining.isEmpty()) {
                    return now.plusDays(1).with(targetTimes.first()).withSecond(0)
                }
            }
        }

        // If today has upcoming times, return the next one today
        val todayUpcoming = upcomingToday.firstOrNull()
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
        val startDate = parseCourseStart(medication)
        val courseEnd = startDate.plusDays(medication.courseDurationDays.toLong())
        if (now.toLocalDate().isAfter(courseEnd)) {
            return null // Course completed
        }

        return calculateDailyIntake(medication, lastIntake, now)
    }

    private fun parseCourseStart(medication: Medication): LocalDate {
        return if (medication.courseStartDate.isNotBlank()) {
            try {
                LocalDate.parse(medication.courseStartDate, DateTimeFormatter.ISO_LOCAL_DATE)
            } catch (e: Exception) {
                LocalDate.now()
            }
        } else {
            LocalDate.now()
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
