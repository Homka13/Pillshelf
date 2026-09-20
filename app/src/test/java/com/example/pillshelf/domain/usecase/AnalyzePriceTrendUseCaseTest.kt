package com.example.pillshelf.domain.usecase

import com.example.pillshelf.data.model.PriceHistory
import com.example.pillshelf.domain.model.PriceTrend
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class AnalyzePriceTrendUseCaseTest {

    private val useCase = AnalyzePriceTrendUseCase()

    @Test
    fun testEmptyHistoryReturnsStable() {
        val result = useCase.analyze(emptyList())
        assertEquals(PriceTrend.STABLE, result.trend)
        assertEquals(0.0, result.percentageChange, 0.001)
    }

    @Test
    fun testSinglePriceReturnsStable() {
        val today = LocalDate.now().toEpochDay()
        val history = listOf(
            PriceHistory(
                id = 1,
                medicationId = 1,
                pharmacyName = "АНЦ",
                price = 100.0,
                currency = "UAH",
                date = "2026-09-20",
                dateEpochDays = today
            )
        )
        val result = useCase.analyze(history)
        assertEquals(PriceTrend.STABLE, result.trend)
        assertEquals(100.0, result.currentAverage, 0.001)
    }

    @Test
    fun testRisingPriceDetected() {
        val today = LocalDate.now().toEpochDay()
        val history = listOf(
            PriceHistory(
                id = 1,
                medicationId = 1,
                pharmacyName = "АНЦ",
                price = 100.0,
                currency = "UAH",
                date = "2026-09-01",
                dateEpochDays = today - 20
            ),
            PriceHistory(
                id = 2,
                medicationId = 1,
                pharmacyName = "АНЦ",
                price = 130.0,
                currency = "UAH",
                date = "2026-09-18",
                dateEpochDays = today - 2
            )
        )
        val result = useCase.analyze(history)
        assertEquals(PriceTrend.RISING, result.trend)
    }

    @Test
    fun testFallingPriceDetected() {
        val today = LocalDate.now().toEpochDay()
        val history = listOf(
            PriceHistory(
                id = 1,
                medicationId = 1,
                pharmacyName = "АНЦ",
                price = 120.0,
                currency = "UAH",
                date = "2026-09-01",
                dateEpochDays = today - 20
            ),
            PriceHistory(
                id = 2,
                medicationId = 1,
                pharmacyName = "АНЦ",
                price = 80.0,
                currency = "UAH",
                date = "2026-09-19",
                dateEpochDays = today - 1
            )
        )
        val result = useCase.analyze(history)
        assertEquals(PriceTrend.FALLING, result.trend)
    }
}
