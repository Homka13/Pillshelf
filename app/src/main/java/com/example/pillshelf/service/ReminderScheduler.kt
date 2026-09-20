package com.example.pillshelf.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.pillshelf.data.local.PillshelfDatabase
import com.example.pillshelf.data.model.Medication
import com.example.pillshelf.domain.usecase.CalculateNextIntakeUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

object ReminderScheduler {
    private const val TAG = "ReminderScheduler"
    const val ACTION_REMINDER_ALARM = "com.example.pillshelf.ACTION_REMINDER_ALARM"
    const val EXTRA_MEDICATION_ID = "extra_medication_id"

    private val calculateNextIntakeUseCase = CalculateNextIntakeUseCase()

    fun scheduleMedication(
        context: Context,
        medication: Medication,
        lastIntake: LocalDateTime? = null
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // If out of stock or "AS_NEEDED", do not schedule alarms
        if (medication.isOutOfStock() || medication.scheduleType.equals("AS_NEEDED", ignoreCase = true)) {
            cancelAlarm(context, medication.id)
            return
        }

        val nextIntake = calculateNextIntakeUseCase.calculateNextIntake(medication, lastIntake)
        if (nextIntake == null) {
            cancelAlarm(context, medication.id)
            return
        }

        val now = LocalDateTime.now()
        // If next calculated time is in the past, schedule for right now or next slot
        val triggerTime = if (nextIntake.isBefore(now)) {
            System.currentTimeMillis() + 1000L // Trigger immediately if due/overdue
        } else {
            nextIntake.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_REMINDER_ALARM
            putExtra(EXTRA_MEDICATION_ID, medication.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            medication.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Use setExactAndAllowWhileIdle to guarantee exact alarm even in Doze mode
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled alarm for ${medication.name} (id=${medication.id}) at $triggerTime")
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException scheduling exact alarm, falling back to standard alarm", e)
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    fun snoozeMedication(context: Context, medicationId: Long, snoozeMinutes: Int = 15) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerTime = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_REMINDER_ALARM
            putExtra(EXTRA_MEDICATION_ID, medicationId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            medicationId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
            Log.d(TAG, "Snoozed alarm for med $medicationId for $snoozeMinutes minutes")
        } catch (e: Exception) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    fun cancelAlarm(context: Context, medicationId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_REMINDER_ALARM
            putExtra(EXTRA_MEDICATION_ID, medicationId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            medicationId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun rescheduleAll(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = PillshelfDatabase.getInstance(context)
                val medications = db.medicationDao().getAllMedicationsSync()
                for (med in medications) {
                    if (med.scheduleType.equals("AS_NEEDED", ignoreCase = true) || med.isOutOfStock()) {
                        cancelAlarm(context, med.id)
                        continue
                    }
                    val lastIntakeRecord = db.intakeHistoryDao().getLastTakenIntake(med.id)
                    val lastIntakeDateTime = lastIntakeRecord?.let {
                        Instant.ofEpochMilli(it.actualTime).atZone(ZoneId.systemDefault()).toLocalDateTime()
                    }
                    scheduleMedication(context, med, lastIntakeDateTime)
                }
                Log.d(TAG, "Successfully rescheduled all active medications")
            } catch (e: Exception) {
                Log.e(TAG, "Error rescheduling all medications", e)
            }
        }
    }
}
