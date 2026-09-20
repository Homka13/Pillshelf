package com.example.pillshelf.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.pillshelf.data.local.PillshelfDatabase
import com.example.pillshelf.data.model.PriceHistory
import com.example.pillshelf.data.remote.PriceAggregator
import com.example.pillshelf.domain.model.PriceTrend
import com.example.pillshelf.domain.usecase.AnalyzePriceTrendUseCase
import java.time.LocalDate

/**
 * Періодична перевірка цін (12 год).
 *
 * Чесність даних: у БД зберігаються ТІЛЬКИ реально отримані ціни з API.
 * Порожня відповідь API не створює записів і не показує «тренди».
 *
 * Моніторинг виконується лише якщо користувач увімкнув його в налаштуваннях
 * (перемикач, за замовчуванням ВИМКНЕНИЙ) — див. SettingsRepository.
 */
class PriceCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Моніторинг цін — опція, вимкнена за замовчуванням.
            if (!SettingsRepository.isPriceMonitoringEnabled(applicationContext)) {
                return Result.success()
            }

            val database = PillshelfDatabase.getInstance(applicationContext)
            val priceAggregator = PriceAggregator()
            val medications = database.medicationDao().getMedicationsWithPriceTracking()
            val trendAnalyzer = AnalyzePriceTrendUseCase()
            val today = LocalDate.now()

            for (medication in medications) {
                try {
                    val currentPrices = priceAggregator.getAllPrices(medication.name)

                    // API недоступний / порожній результат — нічого не пишемо
                    // і нічого не сповіщаємо: немає даних = немає події.
                    if (currentPrices.isEmpty()) continue

                    val historyEntries = currentPrices.map { p ->
                        PriceHistory(
                            medicationId = medication.id,
                            pharmacyName = p.pharmacyName,
                            price = p.price,
                            currency = p.currency,
                            date = today.toString(),
                            dateEpochDays = today.toEpochDay(),
                            url = p.url,
                            source = "Tabletki.ua"
                        )
                    }
                    database.priceHistoryDao().insertAll(historyEntries)

                    // Analyze trend — лише на реальних записах
                    val fullHistory = database.priceHistoryDao().getHistoryForMedicationSync(medication.id)
                    val result = trendAnalyzer.analyze(fullHistory)

                    if (result.trend == PriceTrend.RISING) {
                        NotificationHelper.showNotification(
                            applicationContext,
                            "Ціна зросла",
                            "Ціна на ${medication.name} зросла (+${String.format("%.1f", result.percentageChange)}%). Рекомендуємо купити зараз.",
                            medication.id + 1000,
                            NotificationHelper.CHANNEL_PRICES
                        )
                    } else if (result.trend == PriceTrend.FALLING) {
                        val minPrice = currentPrices.minByOrNull { it.price }?.price ?: 0.0
                        NotificationHelper.showNotification(
                            applicationContext,
                            "Ціна знизилась!",
                            "Ціна на ${medication.name} впала (${String.format("%.1f", result.percentageChange)}%). Найкраща ціна: $minPrice ₴",
                            medication.id + 2000,
                            NotificationHelper.CHANNEL_PRICES
                        )
                    }

                    // Check if lower than target price
                    if (medication.targetPrice > 0) {
                        val bestPrice = currentPrices.minByOrNull { it.price }?.price ?: 0.0
                        if (bestPrice > 0 && bestPrice <= medication.targetPrice) {
                            NotificationHelper.showNotification(
                                applicationContext,
                                "Цільова ціна досягнута!",
                                "Ціна на ${medication.name} зараз $bestPrice ₴ (ваша мета: ${medication.targetPrice} ₴)",
                                medication.id + 3000,
                                NotificationHelper.CHANNEL_PRICES
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e("PriceCheckWorker", "Error checking prices for ${medication.name}", e)
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("PriceCheckWorker", "General error in PriceCheckWorker", e)
            Result.retry()
        }
    }
}
