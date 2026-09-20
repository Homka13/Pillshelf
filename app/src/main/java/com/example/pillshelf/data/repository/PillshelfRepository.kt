package com.example.pillshelf.data.repository

import com.example.pillshelf.data.local.DoseLogDao
import com.example.pillshelf.data.local.MedicationDao
import com.example.pillshelf.data.model.DoseLog
import com.example.pillshelf.data.model.DoseStatus
import com.example.pillshelf.data.model.Medication
import kotlinx.coroutines.flow.Flow

class PillshelfRepository(
    private val medicationDao: MedicationDao,
    private val doseLogDao: DoseLogDao
) {
    val allMedications: Flow<List<Medication>> = medicationDao.getAllMedications()

    fun getMedication(id: Long): Flow<Medication?> = medicationDao.getMedicationById(id)

    suspend fun insertMedication(medication: Medication): Long =
        medicationDao.insertMedication(medication)

    suspend fun updateMedication(medication: Medication) =
        medicationDao.updateMedication(medication)

    suspend fun deleteMedication(medication: Medication) {
        doseLogDao.deleteLogsForMedication(medication.id)
        medicationDao.deleteMedication(medication)
    }

    suspend fun restock(id: Long, addedAmount: Int) {
        medicationDao.incrementStock(id, addedAmount)
    }

    suspend fun updateStock(id: Long, newStock: Int) {
        medicationDao.updateStock(id, newStock)
    }

    suspend fun logDose(
        medication: Medication,
        scheduledTime: String,
        dateEpochDay: Long,
        status: DoseStatus,
        notes: String = ""
    ): Long {
        if (status == DoseStatus.TAKEN) {
            medicationDao.decrementStock(medication.id, 1)
        }
        val log = DoseLog(
            medicationId = medication.id,
            medicationName = medication.name,
            dosageTaken = if (medication.strength.isNotBlank()) "1 ${medication.unit} (${medication.strength})" else "1 ${medication.unit}",
            scheduledTime = scheduledTime,
            timestamp = System.currentTimeMillis(),
            dateEpochDay = dateEpochDay,
            status = status,
            notes = notes
        )
        return doseLogDao.insertLog(log)
    }

    suspend fun undoLog(log: DoseLog) {
        if (log.status == DoseStatus.TAKEN) {
            medicationDao.incrementStock(log.medicationId, 1)
        }
        doseLogDao.deleteLog(log)
    }

    fun getLogsForDay(dateEpochDay: Long): Flow<List<DoseLog>> =
        doseLogDao.getLogsForDay(dateEpochDay)

    val recentLogs: Flow<List<DoseLog>> = doseLogDao.getRecentLogs(100)

    val allLogs: Flow<List<DoseLog>> = doseLogDao.getAllLogs()
}
