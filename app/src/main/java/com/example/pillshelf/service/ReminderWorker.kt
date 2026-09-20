package com.example.pillshelf.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.pillshelf.data.local.PillshelfDatabase
import com.example.pillshelf.data.model.Medication
import com.example.pillshelf.domain.usecase.CalculateNextIntakeUseCase
import java.time.LocalDateTime

class ReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val database = PillshelfDatabase.getInstance(applicationContext)
            val medications = database.medicationDao().getAllMedicationsSync()
            val calculateNextIntakeUseCase = CalculateNextIntakeUseCase()

            for (medication in medications) {
                if (shouldRemind(medication, calculateNextIntakeUseCase)) {
                    sendReminderNotification(medication)
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("ReminderWorker", "Error running reminder worker", e)
            Result.retry()
        }
    }

    private fun shouldRemind(
        medication: Medication,
        calculateNextIntakeUseCase: CalculateNextIntakeUseCase
    ): Boolean {
        if (medication.isOutOfStock()) return false
        val nextIntake = calculateNextIntakeUseCase.calculateNextIntake(medication) ?: return false
        val now = LocalDateTime.now()
        // Remind if within 15 minutes window
        val diffMinutes = java.time.Duration.between(now, nextIntake).toMinutes()
        return diffMinutes in -15..15
    }

    private fun sendReminderNotification(medication: Medication) {
        val mealText = if (medication.takeBeforeMeal) " (до їжі)" else " (після їжі)"
        NotificationHelper.showNotification(
            applicationContext,
            "Час прийняти ліки",
            "${medication.name} - ${medication.dosageForm}$mealText",
            medication.id,
            NotificationHelper.CHANNEL_REMINDERS
        )
    }
}
