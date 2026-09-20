package com.example.pillshelf.data.repository

import com.example.pillshelf.data.local.PriceHistoryDao
import com.example.pillshelf.data.model.PriceHistory
import com.example.pillshelf.data.remote.PriceAggregator
import com.example.pillshelf.domain.model.PriceInfo
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class PriceRepository(
    private val priceHistoryDao: PriceHistoryDao,
    private val priceAggregator: PriceAggregator = PriceAggregator()
) {
    fun getHistoryForMedication(medicationId: Long): Flow<List<PriceHistory>> =
        priceHistoryDao.getHistoryForMedication(medicationId)

    fun getAveragePrice(medicationId: Long, sinceEpochDays: Long): Flow<Double?> =
        priceHistoryDao.getAveragePrice(medicationId, sinceEpochDays)

    fun getOverallMinPrice(medicationId: Long): Flow<Double?> =
        priceHistoryDao.getOverallMinPrice(medicationId)

    suspend fun fetchAndStorePrices(medicationId: Long, drugName: String): List<PriceInfo> {
        val prices = priceAggregator.getAllPrices(drugName)
        val today = LocalDate.now()
        val historyEntries = prices.map { p ->
            PriceHistory(
                medicationId = medicationId,
                pharmacyName = p.pharmacyName,
                price = p.price,
                currency = p.currency,
                date = today.toString(),
                dateEpochDays = today.toEpochDay(),
                url = p.url,
                source = "Tabletki.ua"
            )
        }
        if (historyEntries.isNotEmpty()) {
            priceHistoryDao.insertAll(historyEntries)
        }
        return prices
    }

    suspend fun insertPrice(priceHistory: PriceHistory) =
        priceHistoryDao.insert(priceHistory)

    suspend fun deleteOlderThan(daysOld: Long = 90) {
        val cutoff = LocalDate.now().toEpochDay() - daysOld
        priceHistoryDao.deleteOlderThan(cutoff)
    }
}
