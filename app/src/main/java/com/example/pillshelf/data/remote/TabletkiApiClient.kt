package com.example.pillshelf.data.remote

import com.example.pillshelf.domain.model.PriceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class TabletkiApiClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        const val BASE_URL = "https://tabletki.ua"
        const val SEARCH_WEB_URL = "https://tabletki.ua/uk/search/?q="
    }

    suspend fun searchDrugPrices(drugName: String): List<PriceInfo> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(drugName.trim(), "UTF-8")
        val webUrl = "$SEARCH_WEB_URL$encoded"

        try {
            // Attempt remote query with mobile headers
            val request = Request.Builder()
                .url("$BASE_URL/api/search?q=$encoded")
                .header("User-Agent", "Pillshelf-Android/1.0 (Privacy-First)")
                .header("Accept", "application/json")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrBlank()) {
                    val json = JSONObject(body)
                    if (json.has("prices")) {
                        val pricesArray = json.getJSONArray("prices")
                        val list = mutableListOf<PriceInfo>()
                        for (i in 0 until pricesArray.length()) {
                            val item = pricesArray.getJSONObject(i)
                            list.add(
                                PriceInfo(
                                    pharmacyName = item.optString("pharmacy", "Аптека"),
                                    price = item.optDouble("price", 0.0),
                                    currency = item.optString("currency", "UAH"),
                                    url = item.optString("url", webUrl),
                                    inStock = item.optBoolean("in_stock", true)
                                )
                            )
                        }
                        if (list.isNotEmpty()) return@withContext list
                    }
                }
            }
        } catch (e: Exception) {
            // Log or handle network error
        }

        // Return empty list when remote prices are unavailable — no synthetic/made-up prices
        emptyList()
    }

    fun getSearchWebUrl(drugName: String): String {
        val encoded = try {
            URLEncoder.encode(drugName.trim(), "UTF-8")
        } catch (e: Exception) {
            drugName.trim()
        }
        return "$SEARCH_WEB_URL$encoded"
    }
}
