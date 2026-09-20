package com.example.pillshelf.data.repository

import com.example.pillshelf.data.local.CategoryDao
import com.example.pillshelf.data.local.MedicationDao
import com.example.pillshelf.data.model.Category
import com.example.pillshelf.data.model.Medication
import kotlinx.coroutines.flow.Flow

class MedicationRepository(
    private val medicationDao: MedicationDao,
    private val categoryDao: CategoryDao
) {
    val allMedications: Flow<List<Medication>> = medicationDao.getAllMedications()
    val runningLowMedications: Flow<List<Medication>> = medicationDao.getMedicationsRunningLow()
    val priceTrackedMedications: Flow<List<Medication>> = medicationDao.getMedicationsWithPriceTrackingFlow()
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()

    fun getMedicationById(id: Long): Flow<Medication?> = medicationDao.getMedicationById(id)

    suspend fun getMedicationByIdSync(id: Long): Medication? = medicationDao.getMedicationByIdSync(id)

    fun getMedicationsByCategory(category: String): Flow<List<Medication>> =
        medicationDao.getMedicationsByCategory(category)

    fun getMedicationsExpiringSoon(startDays: Long, endDays: Long): Flow<List<Medication>> =
        medicationDao.getMedicationsExpiringSoon(startDays, endDays)

    suspend fun insertMedication(medication: Medication): Long =
        medicationDao.insert(medication)

    suspend fun updateMedication(medication: Medication) =
        medicationDao.update(medication)

    suspend fun deleteMedication(medication: Medication) =
        medicationDao.delete(medication)

    suspend fun deleteById(id: Long) =
        medicationDao.deleteById(id)

    suspend fun decrementRemainingQuantity(medicationId: Long) =
        medicationDao.decrementRemainingQuantity(medicationId)

    suspend fun incrementRemainingQuantity(medicationId: Long, amount: Int) =
        medicationDao.incrementRemainingQuantity(medicationId, amount)

    suspend fun updateRemainingQuantity(medicationId: Long, quantity: Int) =
        medicationDao.updateRemainingQuantity(medicationId, quantity)
}
