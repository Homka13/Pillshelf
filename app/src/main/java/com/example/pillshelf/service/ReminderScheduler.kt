package com.example.pillshelf.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.pillshelf.data.model.Medication
import com.example.pillshelf.domain.usecase.CalculateNextIntakeUseCase
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Точні будильники для доз ліків.
 *
 * Рішення за замовчуванням — setAlarmClock(): не підпадає під обмеження
 * Doze і показує системну іконку будильника (користувач бачить, що доза
 * запланована). Fallback без дозволу SCHEDULE_EXACT_ALARM —
 * setAndAllowWhileIdle() (неточний, може спрацювати пізніше): стан дозволу
 * завжди видно в графіку прийомів, і звідти ж веде запит дозволу.
 *
 * На кожну дозу ставиться два будильники:
 *  1. ACTION_DOSE_REMINDER — у час дози: сповіщення «Час прийняти».
 *  2. ACTION_FINALIZE_MISSED — через GRACE_MINUTES після дози: якщо користувач
 *     так і не відмітив прийом, у журнал з'являється запис «прострочено»,
 *     а не мовчазна зникаюча картка.
 */
object ReminderScheduler {

    const val ACTION_DOSE_REMINDER = "com.example.pillshelf.ACTION_DOSE_REMINDER"
    const val ACTION_FINALIZE_MISSED = "com.example.pillshelf.ACTION_FINALIZE_MISSED"
    const val ACTION_TAKE_DOSE = "com.example.pillshelf.ACTION_TAKE_DOSE"
    const val ACTION_SNOOZE_DOSE = "com.example.pillshelf.ACTION_SNOOZE_DOSE"
    const val EXTRA_MEDICATION_ID = "extra_medication_id"
    const val EXTRA_SCHEDULED_AT = "extra_scheduled_at"

    /** Скільки хвилин після часу дози даємо на відмітку, перш ніж записати «прострочено». */
    const val GRACE_MINUTES = 30L

    private val useCase = CalculateNextIntakeUseCase()

    fun canScheduleExact(context: Context): Boolean {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            am.canScheduleExactAlarms()
        } else {
            true // До Android 12 окремий дозвіл не потрібен
        }
    }

    /**
     * Інтент на системний екран запиту точних будильників
     * (ACTION_REQUEST_SCHEDULE_EXACT_ALARM). null — дозвіл не потрібен.
     */
    fun exactAlarmSettingsIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        return Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = android.net.Uri.parse("package:${context.packageName}")
        }
    }

    /**
     * Планує наступну дозу для препарату (сповіщення + фіналізація пропуску).
     * Для AS_NEEDED / завершеного курсу — прибирає будильники.
     */
    fun scheduleNextFor(context: Context, medication: Medication, lastIntake: LocalDateTime? = null) {
        val next = useCase.calculateNextIntake(medication, lastIntake)
        if (next == null) {
            cancel(context, medication.id)
            return
        }
        val triggerAt = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val nowMs = System.currentTimeMillis()
        if (triggerAt <= nowMs + 60_000) {
            // Розрахований час уже фактично настав — будильник "зараз" лише
            // згенерує цикл. Ставимо мінімальний відступ.
            schedule(context, medication.id, nowMs + 15 * 60_000)
        } else {
            schedule(context, medication.id, triggerAt)
        }
    }

    /** Сумісність: кнопки сповіщення (IntakeActionReceiver). Те саме, що scheduleNextFor. */
    fun scheduleMedication(context: Context, medication: Medication, lastIntake: LocalDateTime?) =
        scheduleNextFor(context, medication, lastIntake)

    /** Сумісність: «відкласти» на N хвилин (IntakeActionReceiver). Точний будильник від зараз. */
    fun snoozeMedication(context: Context, medicationId: Long, snoozeMinutes: Long = 15L) =
        schedule(context, medicationId, System.currentTimeMillis() + snoozeMinutes * 60_000)

    /** Планує остаточну фіналізацію дози (запис «прострочено», якщо не відмічено). */
    fun scheduleFinalize(context: Context, medicationId: Long, scheduledAtMillis: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = scheduledAtMillis + GRACE_MINUTES * 60_000
        val pi = PendingIntent.getBroadcast(
            context,
            finalizeRequestCode(medicationId),
            Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_FINALIZE_MISSED
                putExtra(EXTRA_MEDICATION_ID, medicationId)
                putExtra(EXTRA_SCHEDULED_AT, scheduledAtMillis)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setAlarm(am, triggerAt, pi)
    }

    fun cancel(context: Context, medicationId: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(PendingIntent.getBroadcast(
            context,
            medicationId.toInt(),
            Intent(context, AlarmReceiver::class.java).apply { action = ACTION_DOSE_REMINDER },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        ))
        am.cancel(PendingIntent.getBroadcast(
            context,
            finalizeRequestCode(medicationId),
            Intent(context, AlarmReceiver::class.java).apply { action = ACTION_FINALIZE_MISSED },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        ))
    }

    /**
     * Планує дозу на конкретний час (використовується для «відкласти»).
     */
    fun schedule(context: Context, medicationId: Long, triggerAt: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val openApp = PendingIntent.getActivity(
            context,
            medicationId.toInt(),
            Intent(context, com.example.pillshelf.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val pi = PendingIntent.getBroadcast(
            context,
            medicationId.toInt(),
            Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_DOSE_REMINDER
                putExtra(EXTRA_MEDICATION_ID, medicationId)
                putExtra(EXTRA_SCHEDULED_AT, triggerAt)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // setAlarmClock: імунітет до Doze + системна іконка будильника.
        if (canScheduleExact(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, openApp), pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
        scheduleFinalize(context, medicationId, triggerAt)
    }

    private fun setAlarm(am: AlarmManager, triggerAt: Long, pi: PendingIntent) {
        if (triggerAt <= System.currentTimeMillis()) return
        if (canScheduleExact(am)) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    private fun canScheduleExact(am: AlarmManager): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()

    private fun finalizeRequestCode(medicationId: Long): Int =
        1_000_000 + (medicationId and 0xFFFFF).toInt()
}
