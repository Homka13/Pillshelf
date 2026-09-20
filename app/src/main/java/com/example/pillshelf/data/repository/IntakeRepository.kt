package com.example.pillshelf.data.repository

import com.example.pillshelf.data.local.IntakeHistoryDao
import com.example.pillshelf.data.local.MedicationDao
import com.example.pillshelf.data.model.IntakeHistory
import com.example.pillshelf.data.model.Medication
import kotlinx.coroutines.flow.Flow

class IntakeRepository(
    private val intakeHistoryDao: IntakeHistoryDao,
    private val medicationDao: MedicationDao
) {
    val allHistory: Flow<List<IntakeHistory>> = intakeHistoryDao.getAllHistory()

    fun getHistoryForMedication(medicationId: Long): Flow<List<IntakeHistory>> =
        intakeHistoryDao.getHistoryForMedication(medicationId)

    fun getHistoryBetweenDates(startTime: Long, endTime: Long): Flow<List<IntakeHistory>> =
        intakeHistoryDao.getHistoryBetweenDates(startTime, endTime)

    suspend fun recordIntake(
        medication: Medication,
        taken: Boolean,
        notes: String = ""
    ): Long {
        if (taken) {
            // Decrement remaining quantity per AC-2
            medicationDao.decrementRemainingQuantity(medication.id)
        }

        val intake = IntakeHistory(
            medicationId = medication.id,
            medicationName = medication.name,
            dosageForm = medication.dosageForm,
            intakeTime = System.currentTimeMillis(),
            actualTime = System.currentTimeMillis(),
            taken = taken,
            notes = notes
        )
        return intakeHistoryDao.insert(intake)
    }

    /**
     * Відмітка прийому ЗАДНІМ ЧИСЛОМ: людина випила ліки о 7 ранку,
     * а кнопку натиснула опівдні. Залишок списується одразу,
     * intakeTime = реальний час прийому, actualTime = момент відмітки.
     */
    suspend fun recordIntakeAt(
        medication: Medication,
        taken: Boolean,
        intakeTimeMillis: Long,
        notes: String = ""
    ): Long {
        if (taken) {
            medicationDao.decrementRemainingQuantity(medication.id)
        }
        val intake = IntakeHistory(
            medicationId = medication.id,
            medicationName = medication.name,
            dosageForm = medication.dosageForm,
            intakeTime = intakeTimeMillis,
            actualTime = System.currentTimeMillis(),
            taken = taken,
            notes = notes
        )
        return intakeHistoryDao.insert(intake)
    }

    suspend fun undoIntake(intake: IntakeHistory) {
        if (intake.taken) {
            // Restore inventory when undid
            medicationDao.incrementRemainingQuantity(intake.medicationId, 1)
        }
        intakeHistoryDao.deleteById(intake.id)
    }

    suspend fun deleteIntake(id: Long) {
        intakeHistoryDao.deleteById(id)
    }

    suspend fun getTodayIntakeCount(medicationId: Long, startOfDayTime: Long): Int =
        intakeHistoryDao.getTodayIntakeCount(medicationId, startOfDayTime)
}
