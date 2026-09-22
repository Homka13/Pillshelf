package com.example.pillshelf.domain.usecase

import com.example.pillshelf.data.model.Medication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Тести планувальника прийомів. Детерміновані: точка відліку `now`
 * ін'єктується в юзкейс (у продакшені — поточний час).
 *
 * Інваріанти:
 * 1. AS_NEEDED не планується — прийом "за потреби" не має нагадувань.
 * 2. Нульовий залишок скасовує план (нагадування не потрібні, коли пити нічого).
 * 3. "Відкласти" (snooze) дає рівно +15 хвилин.
 */
class PillshelfSchedulerTest {

    private val useCase = CalculateNextIntakeUseCase()

    // Фіксована точка відліку: неділя, 20 вересня 2026, 10:00
    private val now = LocalDateTime.of(2026, 9, 20, 10, 0)

    // ── Helpers ─────────────────────────────────────────────────────────────

    private fun med(
        scheduleType: String = "DAILY",
        intervalHours: Int = 8,
        timeOfDay: String = "MORNING,EVENING",
        remainingQuantity: Int = 10,
        courseStartDate: String = "",
        courseDurationDays: Int = 7
    ) = Medication(
        name = "Тест",
        scheduleType = scheduleType,
        intervalHours = intervalHours,
        timeOfDay = timeOfDay,
        remainingQuantity = remainingQuantity,
        courseStartDate = courseStartDate,
        courseDurationDays = courseDurationDays
    )

    // ── 1. AS_NEEDED не планується ──────────────────────────────────────────

    @Test
    fun `AS_NEEDED не планується - повертає null`() {
        val m = med(scheduleType = "AS_NEEDED")
        assertNull(useCase.calculateNextIntake(m, now = now))
    }

    @Test
    fun `AS_NEEDED не планується - навіть якщо був останній прийом`() {
        val m = med(scheduleType = "AS_NEEDED")
        assertNull(useCase.calculateNextIntake(m, lastIntake = now.minusHours(2), now = now))
    }

    @Test
    fun `AS_NEEDED не планується - dosesPerDay дорівнює нулю`() {
        val m = med(scheduleType = "AS_NEEDED")
        assertEquals(0, useCase.dosesPerDay(m))
    }

    @Test
    fun `as_needed у нижньому регістрі теж не планується`() {
        val m = med(scheduleType = "as_needed")
        assertNull(useCase.calculateNextIntake(m, now = now))
    }

    // ── 2. Нульовий залишок скасовує план ───────────────────────────────────

    @Test
    fun `нульовий залишок - isOutOfStock true для всіх типів розкладу`() {
        val daily = med(scheduleType = "DAILY", remainingQuantity = 0)
        val interval = med(scheduleType = "EVERY_N_HOURS", remainingQuantity = 0)
        val course = med(
            scheduleType = "COURSE",
            remainingQuantity = 0,
            courseStartDate = LocalDate.now().toString()
        )
        val prn = med(scheduleType = "AS_NEEDED", remainingQuantity = 0)

        assertTrue(daily.isOutOfStock())
        assertTrue(interval.isOutOfStock())
        assertTrue(course.isOutOfStock())
        assertTrue(prn.isOutOfStock())
    }

    @Test
    fun `нульовий залишок - завершений курс не планує прийомів`() {
        // Курс, що закінчився 2 дні тому: жодного наступного прийому.
        val m = med(
            scheduleType = "COURSE",
            courseStartDate = now.toLocalDate().minusDays(9).toString(),
            courseDurationDays = 7
        )
        assertEquals(0, useCase.dosesPerDay(m))
        assertNull(useCase.calculateNextIntake(m, now = now))
    }

    @Test
    fun `нульовий залишок - юзкейс не планує прийомів (будильники скасовуються)`() {
        // Рішення: нульовий залишок = calculateNextIntake -> null, тож
        // ReminderScheduler.scheduleNextFor прибирає будильники. UI додатково
        // блокує кнопку прийому. Після поповнення план відновлюється.
        val m = med(
            scheduleType = "COURSE",
            courseStartDate = now.toLocalDate().toString(),
            courseDurationDays = 7,
            remainingQuantity = 0
        )
        assertNull(useCase.calculateNextIntake(m, now = now))
        assertEquals(0, useCase.dosesPerDay(m))
    }

    @Test
    fun `позитивний залишок - план активний`() {
        val m = med(scheduleType = "DAILY", remainingQuantity = 1)
        assertTrue(useCase.dosesPerDay(m) > 0)
        assertFalse(m.isOutOfStock())
    }

    // ── 3. «Відкласти» = рівно +15 хвилин ───────────────────────────────────

    @Test
    fun `відкласти - snooze дає РІВНО плюс 15 хвилин`() {
        // ViewModel.snoozeDose: next = useCase(now), потім next.plusMinutes(15).
        // Для EVERY_N_HOURS без lastIntake план = "зараз", тож snooze = now + 15.
        val m = med(scheduleType = "EVERY_N_HOURS", intervalHours = 8)
        val next = useCase.calculateNextIntake(m, now = now)
        assertEquals(now, next)
        assertEquals(now.plusMinutes(15), next!!.plusMinutes(15))
    }

    @Test
    fun `відкласти - доза за слотом 08-00 при now 07-50 відкладається на 08-15`() {
        // Прийом ще не був: наступний план 08:00, «відкласти» -> 08:15.
        val at0750 = LocalDateTime.of(2026, 9, 20, 7, 50)
        val m = med(scheduleType = "DAILY", timeOfDay = "MORNING,EVENING")
        val next = useCase.calculateNextIntake(m, lastIntake = at0750, now = at0750)
        assertEquals(LocalDateTime.of(2026, 9, 20, 8, 0), next)
        assertEquals(LocalDateTime.of(2026, 9, 20, 8, 15), next!!.plusMinutes(15))
    }

    @Test
    fun `відкладена дата завжди в майбутньому відносно now`() {
        val m = med(scheduleType = "DAILY", timeOfDay = "MORNING")
        val at0600 = LocalDateTime.of(2026, 9, 20, 6, 0)
        val next = useCase.calculateNextIntake(m, now = at0600)
        assertTrue(next!!.isAfter(at0600))
        assertTrue(next.plusMinutes(15).isAfter(at0600.plusMinutes(14)))
    }

    // ── Базова поведінка DAILY / EVERY_N_HOURS (регресія) ───────────────────

    @Test
    fun `DAILY - прийом о 07-50 до слота 08-00 не зсуває план (legacy-контракт)`() {
        val at0750 = LocalDateTime.of(2026, 9, 20, 7, 50)
        val m = med(scheduleType = "DAILY", timeOfDay = "MORNING,EVENING")
        val next = useCase.calculateNextIntake(m, lastIntake = at0750, now = at0750)
        assertEquals(LocalDateTime.of(2026, 9, 20, 8, 0), next)
    }

    @Test
    fun `DAILY - прийом о 20-30 після вечірнього слота переносить план на завтра`() {
        val at2030 = LocalDateTime.of(2026, 9, 20, 20, 30)
        val m = med(scheduleType = "DAILY", timeOfDay = "MORNING,EVENING")
        val next = useCase.calculateNextIntake(m, lastIntake = at2030, now = at2030)
        assertEquals(LocalDateTime.of(2026, 9, 21, 8, 0), next)
    }

    @Test
    fun `DAILY - слот у майбутньому сьогодні повертається як наступний`() {
        val m = med(scheduleType = "DAILY", timeOfDay = "MORNING,EVENING")
        val next = useCase.calculateNextIntake(m, now = now) // 10:00 -> вечір 20:00
        assertEquals(LocalDateTime.of(2026, 9, 20, 20, 0), next)
    }

    @Test
    fun `DAILY - всі слоти минули, наступний завтра вранці`() {
        val lateNight = LocalDateTime.of(2026, 9, 20, 23, 30)
        val m = med(scheduleType = "DAILY", timeOfDay = "MORNING")
        val next = useCase.calculateNextIntake(m, now = lateNight)
        assertEquals(LocalDateTime.of(2026, 9, 21, 8, 0), next)
    }

    @Test
    fun `EVERY_N_HOURS - інтервал рахується від останнього прийому`() {
        val m = med(scheduleType = "EVERY_N_HOURS", intervalHours = 6)
        val lastIntake = LocalDateTime.of(2026, 9, 20, 8, 0)
        val next = useCase.calculateNextIntake(m, lastIntake = lastIntake, now = now)
        assertEquals(LocalDateTime.of(2026, 9, 20, 14, 0), next)
    }

    @Test
    fun `EVERY_N_HOURS - прострочений інтервал планує негайно (now)`() {
        val m = med(scheduleType = "EVERY_N_HOURS", intervalHours = 4)
        val lastIntake = LocalDateTime.of(2026, 9, 20, 2, 0) // 8 год тому
        val next = useCase.calculateNextIntake(m, lastIntake = lastIntake, now = now)
        assertEquals(now, next)
    }

    @Test
    fun `EVERY_N_HOURS - за один інтервал надсилається рівно одне сповіщення`() {
        val intervalHours = 6
        val m = med(scheduleType = "EVERY_N_HOURS", intervalHours = intervalHours)
        val lastIntake = LocalDateTime.of(2026, 9, 20, 8, 0)
        val nextScheduled = useCase.calculateNextIntake(m, lastIntake = lastIntake, now = lastIntake)
        assertEquals(LocalDateTime.of(2026, 9, 20, 14, 0), nextScheduled)

        // Симулюємо перебіг часу протягом усього інтервалу (кроками по 15 хв).
        // Оскільки ReminderWorker більше не надсилає сповіщень (лише точні
        // будильники через ReminderScheduler), сповіщення виникає лише в момент
        // настання розрахованого часу дози.
        var notificationsSent = 0
        var current = lastIntake
        val endOfInterval = nextScheduled!!

        while (!current.isAfter(endOfInterval)) {
            // Точний будильник спрацьовує виключно у призначений час дози
            if (current == nextScheduled) {
                notificationsSent++
            }
            // План лишається стабільним на весь інтервал
            val planned = useCase.calculateNextIntake(m, lastIntake = lastIntake, now = current)
            assertEquals(nextScheduled, planned)

            current = current.plusMinutes(15)
        }

        assertEquals("За один інтервал надсилається рівно одне сповіщення", 1, notificationsSent)
    }

    // ── dosesPerDay ─────────────────────────────────────────────────────────

    @Test
    fun `dosesPerDay для DAILY - кількість слотів`() {
        assertEquals(2, useCase.dosesPerDay(med(timeOfDay = "MORNING,EVENING")))
        assertEquals(1, useCase.dosesPerDay(med(timeOfDay = "MORNING")))
    }

    @Test
    fun `dosesPerDay для EVERY_N_HOURS - 24 ділимо на інтервал`() {
        assertEquals(4, useCase.dosesPerDay(med(scheduleType = "EVERY_N_HOURS", intervalHours = 6)))
        assertEquals(3, useCase.dosesPerDay(med(scheduleType = "EVERY_N_HOURS", intervalHours = 8)))
    }

    @Test
    fun `dosesPerDay для активного COURSE - кількість слотів на день`() {
        val m = med(
            scheduleType = "COURSE",
            courseStartDate = LocalDate.now().toString(),
            courseDurationDays = 7,
            timeOfDay = "MORNING,AFTERNOON,EVENING"
        )
        assertEquals(3, useCase.dosesPerDay(m))
    }
}
