package com.example.pillshelf.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.pillshelf.data.local.PillshelfDatabase
import com.example.pillshelf.data.model.IntakeHistory
import com.example.pillshelf.data.model.Medication
import com.example.pillshelf.data.repository.IntakeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/**
 * Приймач точних будильників і кнопок у сповіщенні.
 *
 * ACTION_DOSE_REMINDER: показує сповіщення «Час прийняти» з кнопками
 * «Прийняти» / «Відкласти» і планує фіналізацію через GRACE_MINUTES.
 *
 * ACTION_TAKE_DOSE (кнопка у сповіщенні): записує прийом у журнал
 * (intakeTime = час дози), прибирає сповіщення, планує наступну дозу.
 *
 * ACTION_SNOOZE_DOSE (кнопка у сповіщенні): переносить дозу на
 * +15 хвилин точним будильником; фіналізатор переплановується разом
 * з дозою (FLAG_UPDATE_CURRENT), тож «прострочено» не запишеться.
 *
 * ACTION_FINALIZE_MISSED: якщо прийом так і не був відмічений — додає до
 * журналу запис «прострочено» (доза не зникає мовчки), і планує наступну дозу.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val medicationId = intent.getLongExtra(ReminderScheduler.EXTRA_MEDICATION_ID, -1L)
        if (medicationId <= 0) return
        val scheduledAt = intent.getLongExtra(
            ReminderScheduler.EXTRA_SCHEDULED_AT,
            System.currentTimeMillis()
        )
        when (intent.action) {
            ReminderScheduler.ACTION_DOSE_REMINDER -> goAsyncWork(context) { ctx ->
                handleDose(ctx, medicationId, scheduledAt)
            }
            ReminderScheduler.ACTION_TAKE_DOSE -> goAsyncWork(context) { ctx ->
                handleTake(ctx, medicationId, scheduledAt)
            }
            ReminderScheduler.ACTION_SNOOZE_DOSE -> goAsyncWork(context) { ctx ->
                handleSnooze(ctx, medicationId)
            }
            ReminderScheduler.ACTION_FINALIZE_MISSED -> goAsyncWork(context) { ctx ->
                handleFinalize(ctx, medicationId, scheduledAt)
            }
        }
    }

    private inline fun goAsyncWork(context: Context, crossinline block: suspend (Context) -> Unit) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                block(context)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun handleDose(context: Context, medicationId: Long, scheduledAt: Long) {
        val db = PillshelfDatabase.getInstance(context)
        val med = db.medicationDao().getMedicationByIdSync(medicationId) ?: return
        if (med.isOutOfStock()) {
            // Пити нічого — будильники більше не потрібні.
            ReminderScheduler.cancel(context, medicationId)
            return
        }
        NotificationHelper.showDoseNotification(context, med, scheduledAt)
        ReminderScheduler.scheduleFinalize(context, medicationId, scheduledAt)
    }

    private suspend fun handleTake(context: Context, medicationId: Long, scheduledAt: Long) {
        val db = PillshelfDatabase.getInstance(context)
        val med = db.medicationDao().getMedicationByIdSync(medicationId) ?: return

        // Запис «прийнято» з реальним часом дози (людина могла натиснути пізніше).
        IntakeRepository(db.intakeHistoryDao(), db.medicationDao()).recordIntakeAt(
            medication = med,
            taken = true,
            intakeTimeMillis = scheduledAt,
            notes = "Прийнято (зі сповіщення)"
        )

        cancelNotification(context, medicationId)
        scheduleNext(context, med)
        com.example.pillshelf.widget.DoseWidget.updateAll(context)
    }

    private suspend fun handleSnooze(context: Context, medicationId: Long) {
        val db = PillshelfDatabase.getInstance(context)
        val med = db.medicationDao().getMedicationByIdSync(medicationId) ?: return
        if (med.isOutOfStock()) {
            ReminderScheduler.cancel(context, medicationId)
            return
        }
        // Рівно +15 хвилин від зараз; schedule() також перепланує фіналізатор.
        ReminderScheduler.schedule(context, medicationId, System.currentTimeMillis() + 15 * 60_000)
        cancelNotification(context, medicationId)
        com.example.pillshelf.widget.DoseWidget.updateAll(context)
    }

    private suspend fun handleFinalize(context: Context, medicationId: Long, scheduledAt: Long) {
        val db = PillshelfDatabase.getInstance(context)
        val med = db.medicationDao().getMedicationByIdSync(medicationId) ?: return

        // Відмічений прийом (вчасно або з запасом на годину раніше) — не прострочення.
        val from = scheduledAt - 60 * 60_000
        val to = System.currentTimeMillis() + 60_000
        val marked = db.intakeHistoryDao().getIntakesBetweenSync(medicationId, from, to)
        if (marked.isNotEmpty()) {
            scheduleNext(context, med)
            return
        }

        // Доза не була відмічена — фіксуємо прострочення в журналі.
        db.intakeHistoryDao().insert(
            IntakeHistory(
                medicationId = medicationId,
                medicationName = med.name,
                dosageForm = med.dosageForm,
                intakeTime = scheduledAt,
                actualTime = System.currentTimeMillis(),
                taken = false,
                notes = "Прострочено: не відмічено протягом ${ReminderScheduler.GRACE_MINUTES} хв"
            )
        )
        scheduleNext(context, med)
        com.example.pillshelf.widget.DoseWidget.updateAll(context)
    }

    private suspend fun scheduleNext(context: Context, med: Medication) {
        // Для EVERY_N_HOURS інтервал рахується від "зараз", для решти — наступний слот.
        val lastIntake = if (med.scheduleType == "EVERY_N_HOURS") LocalDateTime.now() else null
        ReminderScheduler.scheduleNextFor(context, med, lastIntake)
    }

    private fun cancelNotification(context: Context, medicationId: Long) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.cancel(medicationId.toInt())
    }
}
