package com.example.pillshelf.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Entity(
    tableName = "intake_history",
    foreignKeys = [
        ForeignKey(
            entity = Medication::class,
            parentColumns = ["id"],
            childColumns = ["medication_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["medication_id"]),
        Index(value = ["intake_time"])
    ]
)
data class IntakeHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "medication_id")
    val medicationId: Long,

    @ColumnInfo(name = "medication_name")
    val medicationName: String = "",

    @ColumnInfo(name = "dosage_form")
    val dosageForm: String = "",

    @ColumnInfo(name = "intake_time")
    val intakeTime: Long = System.currentTimeMillis(),

    val taken: Boolean = true, // true = прийнято, false = пропущено

    @ColumnInfo(name = "actual_time")
    val actualTime: Long = System.currentTimeMillis(),

    val notes: String = ""
) {
    fun getFormattedDateTime(): String {
        return try {
            val instant = Instant.ofEpochMilli(if (actualTime > 0) actualTime else intakeTime)
            val zonedDateTime = instant.atZone(ZoneId.systemDefault())
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").format(zonedDateTime)
        } catch (e: Exception) {
            ""
        }
    }
}
