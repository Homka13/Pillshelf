package com.example.pillshelf.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.pillshelf.data.local.PillshelfDatabase
import com.example.pillshelf.data.model.IntakeHistory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class IntakeActionReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "IntakeActionReceiver"
        const val ACTION_TAKE_MEDICATION = "com.example.pillshelf.ACTION_TAKE_MEDICATION"
        const val ACTION_SNOOZE_MEDICATION = "com.example.pillshelf.ACTION_SNOOZE_MEDICATION"
        const val EXTRA_MEDICATION_ID = "extra_medication_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val medicationId = intent.getLongExtra(EXTRA_MEDICATION_ID, -1L)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, medicationId.toInt())
        if (medicationId == -1L) return

        when (intent.action) {
            ACTION_TAKE_MEDICATION -> {
                NotificationHelper.cancelNotification(context, notificationId)
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = PillshelfDatabase.getInstance(context)
                        val medication = db.medicationDao().getMedicationByIdSync(medicationId)
                        if (medication != null) {
                            val nowMillis = System.currentTimeMillis()
                            db.medicationDao().decrementRemainingQuantity(medicationId, nowMillis)
                            db.intakeHistoryDao().insert(
                                IntakeHistory(
                                    medicationId = medication.id,
                                    medicationName = medication.name,
                                    dosageForm = medication.dosageForm,
                                    intakeTime = nowMillis,
                                    actualTime = nowMillis,
                                    taken = true,
                                    notes = "Прийнято зі сповіщення"
                                )
                            )
                            // Reschedule next intake after this one was taken
                            ReminderScheduler.scheduleMedication(
                                context = context,
                                medication = medication,
                                lastIntake = LocalDateTime.now()
                            )
                            Log.d(TAG, "Medication ${medication.name} recorded as taken via notification action")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing TAKE action for medication $medicationId", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            ACTION_SNOOZE_MEDICATION -> {
                NotificationHelper.cancelNotification(context, notificationId)
                ReminderScheduler.snoozeMedication(context, medicationId, snoozeMinutes = 15)
                Log.d(TAG, "Medication $medicationId snoozed for 15 minutes")
            }
        }
    }
}
