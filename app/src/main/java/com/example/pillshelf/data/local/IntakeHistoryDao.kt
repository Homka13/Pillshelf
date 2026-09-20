package com.example.pillshelf.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pillshelf.data.model.IntakeHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface IntakeHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(intakeHistory: IntakeHistory): Long

    @Query("SELECT * FROM intake_history WHERE medication_id = :medicationId ORDER BY intake_time DESC")
    fun getHistoryForMedication(medicationId: Long): Flow<List<IntakeHistory>>

    @Query("SELECT * FROM intake_history ORDER BY intake_time DESC")
    fun getAllHistory(): Flow<List<IntakeHistory>>

    @Query("SELECT * FROM intake_history ORDER BY intake_time DESC")
    fun getAllHistorySync(): List<IntakeHistory>

    @Query("SELECT * FROM intake_history WHERE intake_time BETWEEN :startTime AND :endTime ORDER BY intake_time DESC")
    fun getHistoryBetweenDates(startTime: Long, endTime: Long): Flow<List<IntakeHistory>>

    @Query("SELECT * FROM intake_history WHERE medication_id = :medicationId AND intake_time BETWEEN :startTime AND :endTime ORDER BY intake_time DESC")
    suspend fun getIntakesBetweenSync(medicationId: Long, startTime: Long, endTime: Long): List<IntakeHistory>

    @Query("DELETE FROM intake_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM intake_history WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): IntakeHistory?

    @Query("SELECT COUNT(*) FROM intake_history WHERE medication_id = :medicationId AND taken = 1 AND intake_time >= :startOfDayTime")
    suspend fun getTodayIntakeCount(medicationId: Long, startOfDayTime: Long): Int

    @Query("DELETE FROM intake_history WHERE medication_id = :medicationId")
    suspend fun deleteForMedication(medicationId: Long)
}
