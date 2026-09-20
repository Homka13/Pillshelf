package com.example.pillshelf.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "price_history",
    foreignKeys = [
        ForeignKey(
            entity = Medication::class,
            parentColumns = ["id"],
            childColumns = ["medication_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["medication_id", "date"])
    ]
)
data class PriceHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "medication_id")
    val medicationId: Long,

    @ColumnInfo(name = "pharmacy_name")
    val pharmacyName: String,         // "Аптека Бажає Здоров'я", "АНЦ", "Подорожник"

    val price: Double,                // 85.50

    val currency: String = "UAH",     // "UAH"

    val date: String,                 // "2026-09-20"

    @ColumnInfo(name = "date_epoch_days")
    val dateEpochDays: Long = LocalDate.now().toEpochDay(),

    val url: String = "",             // "https://tabletki.ua/..."

    val source: String = "Tabletki.ua"
)
