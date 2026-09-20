package com.example.pillshelf.domain.usecase

import com.example.pillshelf.data.model.PriceHistory
import com.example.pillshelf.domain.model.PriceTrend
import java.time.LocalDate

class AnalyzePriceTrendUseCase {

    data class TrendResult(
        val trend: PriceTrend,
        val percentageChange: Double,
        val currentAverage: Double,
        val previousAverage: Double
    ) {
        val description: String
            get() = when (trend) {
                PriceTrend.RISING -> "+${String.format(java.util.Locale.US, "%.1f", percentageChange)}%"
                PriceTrend.FALLING -> "${String.format(java.util.Locale.US, "%.1f", percentageChange)}%"
                PriceTrend.STABLE -> "Стабільна"
            }
    }

    fun analyze(history: List<PriceHistory>): TrendResult {
        if (history.size < 2) {
            val avg = if (history.isNotEmpty()) history.first().price else 0.0
            return TrendResult(PriceTrend.STABLE, 0.0, avg, avg)
        }

        val todayEpochDay = LocalDate.now().toEpochDay()
        val thirtyDaysAgo = todayEpochDay - 30

        val recentPrices = history.filter { it.dateEpochDays >= thirtyDaysAgo }
        if (recentPrices.size < 2) {
            val avg = history.map { it.price }.average().takeIf { !it.isNaN() } ?: 0.0
            return TrendResult(PriceTrend.STABLE, 0.0, avg, avg)
        }

        // Previous average: 20-30 days ago (or oldest half if limited dataset)
        val oldRangePrices = recentPrices.filter { it.dateEpochDays in (todayEpochDay - 30)..(todayEpochDay - 15) }
        val currentRangePrices = recentPrices.filter { it.dateEpochDays in (todayEpochDay - 14)..todayEpochDay }

        val oldAverage = if (oldRangePrices.isNotEmpty()) {
            oldRangePrices.map { it.price }.average()
        } else {
            // fallback: oldest 30%
            recentPrices.takeLast(recentPrices.size / 2).map { it.price }.average()
        }

        val currentAverage = if (currentRangePrices.isNotEmpty()) {
            currentRangePrices.map { it.price }.average()
        } else {
            recentPrices.take(recentPrices.size / 2).map { it.price }.average()
        }

        if (oldAverage == 0.0 || oldAverage.isNaN() || currentAverage.isNaN()) {
            return TrendResult(PriceTrend.STABLE, 0.0, currentAverage, oldAverage)
        }

        val changePercent = ((currentAverage - oldAverage) / oldAverage) * 100.0

        val trend = when {
            changePercent > 10.0 -> PriceTrend.RISING
            changePercent < -10.0 -> PriceTrend.FALLING
            else -> PriceTrend.STABLE
        }

        return TrendResult(
            trend = trend,
            percentageChange = changePercent,
            currentAverage = currentAverage,
            previousAverage = oldAverage
        )
    }
}
