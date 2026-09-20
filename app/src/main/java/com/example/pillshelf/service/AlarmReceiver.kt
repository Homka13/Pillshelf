package com.example.pillshelf.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.pillshelf.data.local.PillshelfDatabase
import com.example.pillshelf.data.model.IntakeHistory
import com.example.pillshelf.domain.usecase.CalculateNextIntakeUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Приймач точних будильників доз.
 *
 * ACTION_DOSE_REMINDER: показує сповіщення «Час прийняти» і планує
 * фіналізацію через GRACE_MINUTES.
 *
 * ACTION_FINALIZE_MISSED: якщо прийом так і не був відмічений — додає до
 * журналу запис «прострочено» (доза не зникає мовчки), і планує наступну дозу.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val medicationId = intent.getLongExtra(ReminderScheduler.EXTRA_MEDICATION_ID, -1L)
        if (medicationId <= 0) return
        val scheduledAt = intent.getLongExtra(ReminderScheduler.EXTRA_SCHEDULED_AT, System.currentTimeMillis())
        val action = intent.action
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    ReminderScheduler.ACTION_DOSE_REMINDER -> handleDose(context, medicationId, scheduledAt)
                    ReminderScheduler.ACTION_FINALIZE_MISSED -> handleFinalize(context, medicationId, scheduledAt)
                }
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
        val mealText = if (med.takeBeforeMeal) " (до їжі)" else " (після їжі)"
        NotificationHelper.showNotification(
            context,
            "Час прийняти ліки",
            "${med.name} — ${med.dosageForm}$mealText",
            med.id,
            NotificationHelper.CHANNEL_REMINDERS
        )
        ReminderScheduler.scheduleFinalize(context, medicationId, scheduledAt)
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
    }

    private suspend fun scheduleNext(context: Context, med: com.example.pillshelf.data.model.Medication) {
        // Для EVERY_N_HOURS рахуємо інтервал від "зараз", для решти — наступний слот.
        val lastIntake = if (med.scheduleType == "EVERY_N_HOURS") {
            LocalDateTime.now()
        } else {
            null
        }
        ReminderScheduler.scheduleNextFor(context, med, lastIntake)
    }
}
