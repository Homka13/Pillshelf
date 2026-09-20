package com.example.pillshelf.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DoseStatus {
    TAKEN,
    SKIPPED
}

@Entity(tableName = "dose_logs")
data class DoseLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val medicationId: Long,
    val medicationName: String,
    val dosageTaken: String = "1 dose",
    val scheduledTime: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val dateEpochDay: Long = 0L,
    val status: DoseStatus = DoseStatus.TAKEN,
    val notes: String = ""
)
