package com.example.pillshelf.data.remote

import com.example.pillshelf.domain.model.PriceInfo

class PriceAggregator(
    private val apiClient: TabletkiApiClient = TabletkiApiClient()
) {
    suspend fun getAllPrices(drugName: String): List<PriceInfo> {
        return apiClient.searchDrugPrices(drugName)
    }

    suspend fun getBestPrice(drugName: String): PriceInfo? {
        val prices = getAllPrices(drugName)
        return prices.minByOrNull { it.price }
    }
}
