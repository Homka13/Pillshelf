package com.example.pillshelf.domain.model

data class PriceInfo(
    val pharmacyName: String,
    val price: Double,
    val currency: String = "UAH",
    val url: String = "",
    val inStock: Boolean = true,
    val address: String = ""
)

enum class PriceTrend(val titleUk: String) {
    RISING("Ціна зросла"),
    FALLING("Ціна знизилась"),
    STABLE("Ціна стабільна")
}

enum class ScheduleTypeEnum(val id: String, val titleUk: String) {
    DAILY("DAILY", "Щодня"),
    EVERY_N_HOURS("EVERY_N_HOURS", "Кожні N годин"),
    COURSE("COURSE", "Курс прийому")
}

data class InteractionWarning(
    val title: String,
    val description: String,
    val severity: Severity,
    val conflictingMeds: List<String>
) {
    enum class Severity {
        HIGH,
        MODERATE,
        INFO
    }
}
