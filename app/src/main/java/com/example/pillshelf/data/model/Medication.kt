package com.example.pillshelf.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MedicationForm(val displayName: String, val defaultUnit: String) {
    TABLET("Tablet", "tablets"),
    CAPSULE("Capsule", "capsules"),
    LIQUID("Liquid / Syrup", "ml"),
    DROPS("Eye/Ear Drops", "drops"),
    INHALER("Inhaler", "puffs"),
    SPRAY("Nasal Spray", "sprays"),
    TOPICAL("Cream / Ointment", "applications"),
    INJECTION("Injection", "units"),
    OTHER("Other", "doses")
}

enum class MedicationCategory(val displayName: String) {
    ALL("All"),
    PAIN_RELIEF("Pain Relief"),
    DAILY_SUPPLEMENT("Supplements & Vitamins"),
    ALLERGY_SINUS("Allergy & Sinus"),
    PRESCRIPTION("Prescription"),
    COLD_FLU("Cold & Flu"),
    DIGESTIVE("Digestive Health"),
    FIRST_AID("First Aid"),
    OTHER("Other")
}

enum class ScheduleType(val displayName: String) {
    DAILY("Once Daily"),
    TWICE_DAILY("Twice Daily"),
    THREE_TIMES_DAILY("3 Times Daily"),
    AS_NEEDED("As Needed (PRN)"),
    CUSTOM("Custom Schedule")
}

@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val brandOrGeneric: String = "",
    val form: MedicationForm = MedicationForm.TABLET,
    val strength: String = "",
    val category: MedicationCategory = MedicationCategory.OTHER,
    val stockQuantity: Int = 0,
    val lowStockThreshold: Int = 5,
    val unit: String = "pills",
    val expiryDateEpochDays: Long = 0L,
    val storageLocation: String = "Medicine Cabinet",
    val instructions: String = "",
    val scheduleType: ScheduleType = ScheduleType.DAILY,
    val scheduledTimes: String = "08:00", // comma-delimited strings e.g. "08:00,20:00"
    val colorHex: Long = 0xFF0D9488,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun isLowStock(): Boolean = stockQuantity <= lowStockThreshold

    fun isExpired(currentEpochDay: Long): Boolean =
        expiryDateEpochDays > 0 && expiryDateEpochDays < currentEpochDay

    fun isExpiringSoon(currentEpochDay: Long, thresholdDays: Long = 30): Boolean =
        expiryDateEpochDays > 0 &&
                expiryDateEpochDays >= currentEpochDay &&
                expiryDateEpochDays <= (currentEpochDay + thresholdDays)

    fun daysUntilExpiry(currentEpochDay: Long): Long =
        if (expiryDateEpochDays > 0) expiryDateEpochDays - currentEpochDay else Long.MAX_VALUE

    fun getTimesList(): List<String> =
        if (scheduledTimes.isBlank()) emptyList()
        else scheduledTimes.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}
