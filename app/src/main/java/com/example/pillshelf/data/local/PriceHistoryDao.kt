package com.example.pillshelf.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pillshelf.data.model.PriceHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface PriceHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(prices: List<PriceHistory>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(price: PriceHistory): Long

    @Query("SELECT * FROM price_history WHERE medication_id = :medicationId ORDER BY date_epoch_days DESC, price ASC")
    fun getHistoryForMedication(medicationId: Long): Flow<List<PriceHistory>>

    @Query("SELECT * FROM price_history WHERE medication_id = :medicationId ORDER BY date_epoch_days DESC, price ASC")
    fun getHistoryForMedicationSync(medicationId: Long): List<PriceHistory>

    @Query("SELECT AVG(price) FROM price_history WHERE medication_id = :medicationId AND date_epoch_days >= :sinceEpochDays")
    fun getAveragePrice(medicationId: Long, sinceEpochDays: Long): Flow<Double?>

    @Query("SELECT MIN(price) FROM price_history WHERE medication_id = :medicationId AND date = :date")
    fun getMinPriceOnDate(medicationId: Long, date: String): Flow<Double?>

    @Query("SELECT MIN(price) FROM price_history WHERE medication_id = :medicationId")
    fun getOverallMinPrice(medicationId: Long): Flow<Double?>

    @Query("DELETE FROM price_history WHERE date_epoch_days < :beforeEpochDays")
    suspend fun deleteOlderThan(beforeEpochDays: Long)

    @Query("SELECT * FROM price_history ORDER BY date_epoch_days DESC")
    fun getAllPrices(): Flow<List<PriceHistory>>
}
