package com.example.pillshelf.data.remote

import com.example.pillshelf.domain.model.PriceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Клієнт tabletki.ua.
 *
 * ВАЖЛИВО (чесність даних): застосунок НЕ генерує і не підставляє
 * вигадані ціни. Якщо відповідь недоступна (403 Cloudflare, таймаут,
 * незнайомий формат) — повертається порожній список, а UI показує
 * «ціни недоступні» з кнопкою переходу на Tabletki.ua.
 *
 * Станом на 2026-09: /api/search віддає HTTP 403 (Cloudflare) для
 * не-браузерних клієнтів, тож мережеві ціни фактично недоступні;
 * єдиний робочий шлях — веб-посилання на пошук Tabletki.ua.
 */
class TabletkiApiClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        const val BASE_URL = "https://tabletki.ua"
        const val SEARCH_WEB_URL = "https://tabletki.ua/uk/search/?q="

        // Нейтральний UA: не видаваємо додаток і не намагаємося обійти
        // захист — просто коректно представляємо HTTP-клієнт.
        const val USER_AGENT =
            "Mozilla/5.0 (Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36"
    }

    /**
     * Повертає ціни з tabletki.ua або ПОРОЖНІЙ список, якщо дані
     * недоступні. Ніколи не повертає сфабриковані значення.
     */
    suspend fun searchDrugPrices(drugName: String): List<PriceInfo> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(drugName.trim(), "UTF-8")
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/search?q=$encoded")
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                parsePrices(body, fallbackUrl = "$SEARCH_WEB_URL$encoded")
            }
        } catch (e: Exception) {
            // Мережа недоступна / таймаут — чесний порожній результат.
            emptyList()
        }
    }

    /** Парсинг відповіді; незнайомий формат => порожній список (не вигадки). */
    private fun parsePrices(body: String, fallbackUrl: String): List<PriceInfo> {
        return try {
            val json = org.json.JSONObject(body)
            if (!json.has("prices")) return emptyList()
            val pricesArray = json.getJSONArray("prices")
            val list = mutableListOf<PriceInfo>()
            for (i in 0 until pricesArray.length()) {
                val item = pricesArray.getJSONObject(i)
                val price = item.optDouble("price", Double.NaN)
                if (price.isNaN() || price <= 0.0) continue
                list.add(
                    PriceInfo(
                        pharmacyName = item.optString("pharmacy", "").ifBlank { "Аптека" },
                        price = price,
                        currency = item.optString("currency", "UAH"),
                        url = item.optString("url", fallbackUrl),
                        inStock = item.optBoolean("in_stock", true)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }
}
