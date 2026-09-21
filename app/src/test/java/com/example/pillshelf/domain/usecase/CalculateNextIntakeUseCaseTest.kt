package com.example.pillshelf.domain.usecase

import com.example.pillshelf.data.model.Medication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class CalculateNextIntakeUseCaseTest {

    private val useCase = CalculateNextIntakeUseCase()

    private fun createMedication(
        scheduleType: String,
        timeOfDay: String = "MORNING",
        intervalHours: Int = 8,
        courseDurationDays: Int = 7,
        courseStartDate: String = LocalDate.now().toString(),
        remainingQuantity: Int = 10
    ): Medication {
        return Medication(
            id = 1,
            name = "ТестМед",
            activeSubstance = "Testin",
            dosageForm = "Таблетки 500 мг",
            category = "Аптечка",
            expiryDate = "2026-12-31",
            totalQuantity = 20,
            remainingQuantity = remainingQuantity,
            scheduleType = scheduleType,
            timeOfDay = timeOfDay,
            intervalHours = intervalHours,
            courseDurationDays = courseDurationDays,
            courseStartDate = courseStartDate,
            takeBeforeMeal = false,
            notes = "",
            trackPrices = false,
            targetPrice = 0.0,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    @Test
    fun testAsNeededReturnsNull() {
        val med = createMedication(scheduleType = "AS_NEEDED")
        val next = useCase.calculateNextIntake(med, LocalDateTime.now())
        assertNull("As-needed (PRN) medications should have no scheduled alarms", next)
    }

    @Test
    fun testOutOfStockReturnsNull() {
        val med = createMedication(scheduleType = "DAILY", remainingQuantity = 0)
        val next = useCase.calculateNextIntake(med, LocalDateTime.now())
        assertNull("Out-of-stock medication should not schedule intake", next)
    }

    @Test
    fun testDailyScheduleMorningBeforeEight() {
        val med = createMedication(scheduleType = "DAILY", timeOfDay = "MORNING")
        val fromTime = LocalDateTime.of(LocalDate.now(), LocalTime.of(7, 0))
        val next = useCase.calculateNextIntake(med, lastIntake = null, now = fromTime)

        assertNotNull(next)
        assertEquals(8, next!!.hour)
        assertEquals(0, next.minute)
        assertEquals(fromTime.toLocalDate(), next.toLocalDate())
    }

    @Test
    fun testDailyScheduleMorningAfterEightSchedulesNextDay() {
        val med = createMedication(scheduleType = "DAILY", timeOfDay = "MORNING")
        val fromTime = LocalDateTime.of(LocalDate.now(), LocalTime.of(9, 0))
        val next = useCase.calculateNextIntake(med, lastIntake = null, now = fromTime)

        assertNotNull(next)
        assertEquals(8, next!!.hour)
        assertEquals(fromTime.toLocalDate().plusDays(1), next.toLocalDate())
    }

    @Test
    fun testEveryNHoursCalculation() {
        val med = createMedication(scheduleType = "EVERY_N_HOURS", intervalHours = 6)
        val fromTime = LocalDateTime.of(2026, 6, 1, 10, 0)
        val next = useCase.calculateNextIntake(med, lastIntake = fromTime, now = fromTime)

        assertNotNull(next)
        assertEquals(LocalDateTime.of(2026, 6, 1, 16, 0), next)
    }

    @Test
    fun testCourseCompletedReturnsNull() {
        val pastStartDate = LocalDate.now().minusDays(10).toString()
        val med = createMedication(
            scheduleType = "COURSE",
            courseDurationDays = 5,
            courseStartDate = pastStartDate
        )
        val next = useCase.calculateNextIntake(med, lastIntake = null, now = LocalDateTime.now())
        assertNull("Completed course should not schedule further intakes", next)
    }
}
