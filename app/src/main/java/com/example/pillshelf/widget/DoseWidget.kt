package com.example.pillshelf.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.example.pillshelf.MainActivity
import com.example.pillshelf.R
import com.example.pillshelf.data.local.PillshelfDatabase
import com.example.pillshelf.domain.usecase.CalculateNextIntakeUseCase
import com.example.pillshelf.service.AlarmReceiver
import com.example.pillshelf.service.ReminderScheduler
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Віджет головного екрана: наступна доза + кнопка «Прийняти» без відкриття
 * застосунку.
 *
 * Оновлення: системний ACTION_APPWIDGET_UPDATE, внутрішній тік кожні 15 хв
 * (будильник перепланує себе), а також DoseWidget.updateAll() з AlarmReceiver
 * після кожної дії користувача з дозою.
 */
class DoseWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        refresh(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TICK_UPDATE) {
            refresh(context)
        }
    }

    companion object {
        const val ACTION_TICK_UPDATE = "com.example.pillshelf.widget.TICK_UPDATE"
        private const val TAKE_BUTTON_RC = 9_100_000
        private const val TICK_RC = 9_200_000
        private const val OPEN_APP_RC = 9_300_000
        private const val TICK_MINUTES = 15L

        /** Оновлює всі екземпляри віджета. Безпечний виклик звідусіль (швидкий локальний запит). */
        fun updateAll(context: Context) {
            refresh(context.applicationContext)
        }

        private fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, DoseWidget::class.java))
            if (ids.isEmpty()) return

            val db = PillshelfDatabase.getInstance(context)
            val useCase = CalculateNextIntakeUseCase()

            // Найближча за часом доза серед усіх препаратів (не AS_NEEDED, не нуль)
            val now = LocalDateTime.now()
            var bestMed: com.example.pillshelf.data.model.Medication? = null
            var bestTime: LocalDateTime? = null
            val meds = runBlocking { db.medicationDao().getAllMedicationsSync() }
            for (med in meds) {
                if (med.isOutOfStock()) continue
                val next = useCase.calculateNextIntake(med, now = now) ?: continue
                if (bestTime == null || next.isBefore(bestTime)) {
                    bestMed = med
                    bestTime = next
                }
            }

            val views = RemoteViews(context.packageName, R.layout.pillshelf_widget)
            val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

            if (bestMed == null || bestTime == null) {
                views.setTextViewText(R.id.text_next_med_time, "--:--")
                views.setTextViewText(
                    R.id.text_next_med_name,
                    if (meds.isEmpty()) "Аптечка порожня" else "Доз немає"
                )
                views.setTextViewText(R.id.text_widget_hint, "Додайте ліки з графіком")
                views.setViewVisibility(R.id.button_take_next, android.view.View.GONE)
            } else {
                val med = bestMed
                val next = bestTime
                views.setTextViewText(R.id.text_next_med_time, timeFmt.format(next))
                views.setTextViewText(R.id.text_next_med_name, med.name)
                views.setTextViewText(
                    R.id.text_widget_hint,
                    "${med.dosageForm} • залишок: ${med.remainingQuantity}"
                )
                views.setViewVisibility(R.id.button_take_next, android.view.View.VISIBLE)

                // «Прийняти» прямо зі стільниці: та сама обробка, що й кнопка
                // у сповіщенні (запис у журнал, наступна доза, оновлення віджета).
                val takePi = PendingIntent.getBroadcast(
                    context,
                    (TAKE_BUTTON_RC + med.id).toInt(),
                    Intent(context, AlarmReceiver::class.java).apply {
                        action = ReminderScheduler.ACTION_TAKE_DOSE
                        putExtra(ReminderScheduler.EXTRA_MEDICATION_ID, med.id)
                        putExtra(ReminderScheduler.EXTRA_SCHEDULED_AT, System.currentTimeMillis())
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.button_take_next, takePi)
            }

            // Тап по віджету відкриває застосунок
            val openPi = PendingIntent.getActivity(
                context,
                OPEN_APP_RC,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.text_next_med_name, openPi)

            manager.updateAppWidget(ids, views)
            scheduleNextUpdate(context)
        }

        /** Періодичне оновлення годинника: будильник на +15 хв, який сам себе перепланує. */
        private fun scheduleNextUpdate(context: Context) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pi = PendingIntent.getBroadcast(
                context,
                TICK_RC,
                Intent(context, DoseWidget::class.java).apply { action = ACTION_TICK_UPDATE },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val trigger = System.currentTimeMillis() + TICK_MINUTES * 60_000
            val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
            if (exact) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
            } else {
                am.set(AlarmManager.RTC_WAKEUP, trigger, pi)
            }
        }
    }
}
