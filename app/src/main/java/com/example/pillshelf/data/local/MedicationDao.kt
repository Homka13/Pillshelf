package com.example.pillshelf.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.pillshelf.data.model.Medication
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(medication: Medication): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(medications: List<Medication>)

    @Update
    suspend fun update(medication: Medication)

    @Delete
    suspend fun delete(medication: Medication)

    @Query("SELECT * FROM medications ORDER BY name ASC")
    fun getAllMedications(): Flow<List<Medication>>

    @Query("SELECT * FROM medications ORDER BY name ASC")
    fun getAllMedicationsSync(): List<Medication>

    @Query("SELECT * FROM medications WHERE id = :id LIMIT 1")
    fun getMedicationById(id: Long): Flow<Medication?>

    @Query("SELECT * FROM medications WHERE id = :id LIMIT 1")
    suspend fun getMedicationByIdSync(id: Long): Medication?

    @Query("SELECT * FROM medications WHERE category = :category ORDER BY name ASC")
    fun getMedicationsByCategory(category: String): Flow<List<Medication>>

    @Query("SELECT * FROM medications WHERE remaining_quantity <= 5 AND remaining_quantity > 0")
    fun getMedicationsRunningLow(): Flow<List<Medication>>

    @Query("SELECT * FROM medications WHERE expiry_date_epoch_days BETWEEN :startDays AND :endDays")
    fun getMedicationsExpiringSoon(startDays: Long, endDays: Long): Flow<List<Medication>>

    @Query("SELECT * FROM medications WHERE track_prices = 1")
    fun getMedicationsWithPriceTracking(): List<Medication>

    @Query("SELECT * FROM medications WHERE track_prices = 1")
    fun getMedicationsWithPriceTrackingFlow(): Flow<List<Medication>>

    @Query("UPDATE medications SET remaining_quantity = remaining_quantity - 1, updated_at = :updatedAt WHERE id = :medicationId AND remaining_quantity > 0")
    suspend fun decrementRemainingQuantity(medicationId: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE medications SET remaining_quantity = remaining_quantity + :amount, updated_at = :updatedAt WHERE id = :medicationId")
    suspend fun incrementRemainingQuantity(medicationId: Long, amount: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE medications SET remaining_quantity = :quantity, updated_at = :updatedAt WHERE id = :medicationId")
    suspend fun updateRemainingQuantity(medicationId: Long, quantity: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM medications WHERE id = :id")
    suspend fun deleteById(id: Long)
}
