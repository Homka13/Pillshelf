package com.example.pillshelf.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.pillshelf.data.local.PillshelfDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "AlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val medicationId = intent.getLongExtra(ReminderScheduler.EXTRA_MEDICATION_ID, -1L)
        if (medicationId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = PillshelfDatabase.getInstance(context)
                val medication = db.medicationDao().getMedicationByIdSync(medicationId)

                if (medication != null && !medication.isOutOfStock()) {
                    val mealNote = if (medication.takeBeforeMeal) " • До їжі" else " • Після їжі"
                    NotificationHelper.showMedicationReminderNotification(
                        context = context,
                        medicationId = medication.id,
                        medicationName = medication.name,
                        dosageForm = medication.dosageForm,
                        mealNote = mealNote
                    )

                    // Schedule the next dose in advance
                    ReminderScheduler.scheduleMedication(
                        context = context,
                        medication = medication,
                        lastIntake = LocalDateTime.now()
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling reminder alarm for medication $medicationId", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
