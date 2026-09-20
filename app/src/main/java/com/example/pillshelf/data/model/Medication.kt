package com.example.pillshelf.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Entity(
    tableName = "medications",
    indices = [
        Index(value = ["name"]),
        Index(value = ["category"])
    ]
)
data class Medication(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,                    // "Парацетамол"

    @ColumnInfo(name = "active_substance")
    val activeSubstance: String = "",    // "Paracetamol"

    @ColumnInfo(name = "dosage_form")
    val dosageForm: String = "Таблетки 500 мг", // "Таблетки 500 мг", "Капсули"

    val manufacturer: String = "",       // "Київмедпрепарат"

    val category: String = "Аптечка",    // "Аптечка", "Вітаміни", "Знеболювальні", "Рецептурні", "Травлення"

    @ColumnInfo(name = "total_quantity")
    val totalQuantity: Int = 30,         // 30 (всього в упаковці)

    @ColumnInfo(name = "remaining_quantity")
    val remainingQuantity: Int = 30,     // 12 (залишилось)

    @ColumnInfo(name = "expiry_date")
    val expiryDate: String = "",         // "2027-06-30"

    @ColumnInfo(name = "expiry_date_epoch_days")
    val expiryDateEpochDays: Long = 0L,

    @ColumnInfo(name = "schedule_type")
    val scheduleType: String = "DAILY",  // "DAILY", "EVERY_N_HOURS", "COURSE"

    @ColumnInfo(name = "interval_hours")
    val intervalHours: Int = 8,          // для EVERY_N_HOURS

    @ColumnInfo(name = "time_of_day")
    val timeOfDay: String = "MORNING",   // "MORNING,AFTERNOON,EVENING"

    @ColumnInfo(name = "course_start_date")
    val courseStartDate: String = "",    // "2026-09-20"

    @ColumnInfo(name = "course_duration_days")
    val courseDurationDays: Int = 7,     // для COURSE

    @ColumnInfo(name = "take_before_meal")
    val takeBeforeMeal: Boolean = false, // true = до їжі, false = після їжі

    val notes: String = "",              // "Запити водою", "Не розжовувати"

    @ColumnInfo(name = "profile_id")
    val profileId: String = "default",   // "default", "mom", "dad"

    @ColumnInfo(name = "track_prices")
    val trackPrices: Boolean = false,    // Чи відстежувати ціни

    @ColumnInfo(name = "target_price")
    val targetPrice: Double = 0.0,       // Бажана ціна (0 = не встановлено)

    @ColumnInfo(name = "color_hex")
    val colorHex: Long = 0xFF0D9488,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun isLowStock(): Boolean = remainingQuantity in 1..5

    fun isOutOfStock(): Boolean = remainingQuantity <= 0

    fun isExpired(currentEpochDay: Long = LocalDate.now().toEpochDay()): Boolean {
        val epochDay = getEffectiveExpiryEpochDay()
        return epochDay > 0 && epochDay < currentEpochDay
    }

    fun isExpiringSoon(currentEpochDay: Long = LocalDate.now().toEpochDay(), thresholdDays: Long = 30): Boolean {
        val epochDay = getEffectiveExpiryEpochDay()
        return epochDay > 0 && epochDay >= currentEpochDay && epochDay <= (currentEpochDay + thresholdDays)
    }

    fun daysUntilExpiry(currentEpochDay: Long = LocalDate.now().toEpochDay()): Long {
        val epochDay = getEffectiveExpiryEpochDay()
        return if (epochDay > 0) epochDay - currentEpochDay else Long.MAX_VALUE
    }

    fun getEffectiveExpiryEpochDay(): Long {
        if (expiryDateEpochDays > 0) return expiryDateEpochDays
        if (expiryDate.isNotBlank()) {
            return try {
                LocalDate.parse(expiryDate, DateTimeFormatter.ISO_LOCAL_DATE).toEpochDay()
            } catch (e: Exception) {
                0L
            }
        }
        return 0L
    }

    fun getTimeOfDayList(): List<String> {
        if (timeOfDay.isBlank()) return listOf("MORNING")
        return timeOfDay.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
}
