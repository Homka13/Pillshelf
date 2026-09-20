package com.example.pillshelf.service

import android.content.Context
import android.content.SharedPreferences
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Налаштування застосунку. SharedPreferences — достатньо для прапорців.
 */
object SettingsRepository {

    private const val PREFS = "pillshelf_settings"
    private const val KEY_PRICE_MONITORING = "price_monitoring_enabled"

    /**
     * Моніторинг цін. ЗА ЗАМОВЧУВАННЯМ ВИМКНЕНИЙ: мережеві запити з
     * назвами препаратів — це потенційний витік даних про здоров'я,
     * тож увімкнути може тільки користувач вручну.
     */
    fun isPriceMonitoringEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PRICE_MONITORING, false)

    fun setPriceMonitoringEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_PRICE_MONITORING, enabled).apply()
        if (enabled) {
            enqueuePriceChecks(context)
        } else {
            WorkManager.getInstance(context).cancelUniqueWork(PRICE_WORK_NAME)
        }
    }

    /** (Ре)реєстрація періодичної задачі перевірки цін. */
    fun enqueuePriceChecks(context: Context) {
        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<PriceCheckWorker>(12, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PRICE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private const val PRICE_WORK_NAME = "pillshelf_price_checks"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
