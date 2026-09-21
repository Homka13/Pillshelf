package com.example.pillshelf.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.pillshelf.MainActivity
import com.example.pillshelf.R
import com.example.pillshelf.data.model.Medication
import com.example.pillshelf.service.AlarmReceiver

object NotificationHelper {
    const val CHANNEL_REMINDERS = "pillshelf_reminders"
    const val CHANNEL_PRICES = "pillshelf_prices"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Нагадування про прийом ліків",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Сповіщення про необхідність прийняти ліки за розкладом"
                enableVibration(true)
            }

            val priceChannel = NotificationChannel(
                CHANNEL_PRICES,
                "Моніторинг цін на ліки",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Сповіщення про зміну цін у аптеках на відстежувані препарати"
            }

            notificationManager.createNotificationChannel(reminderChannel)
            notificationManager.createNotificationChannel(priceChannel)
        }
    }

    fun showMedicationReminderNotification(
        context: Context,
        medicationId: Long,
        medicationName: String,
        dosageForm: String,
        mealNote: String
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            medicationId.toInt(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Прийняв (Take)
        val takeIntent = Intent(context, IntakeActionReceiver::class.java).apply {
            action = IntakeActionReceiver.ACTION_TAKE_MEDICATION
            putExtra(IntakeActionReceiver.EXTRA_MEDICATION_ID, medicationId)
            putExtra(IntakeActionReceiver.EXTRA_NOTIFICATION_ID, medicationId.toInt())
        }
        val takePendingIntent = PendingIntent.getBroadcast(
            context,
            (medicationId * 10 + 1).toInt(),
            takeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Відкласти на 15 хв (Snooze)
        val snoozeIntent = Intent(context, IntakeActionReceiver::class.java).apply {
            action = IntakeActionReceiver.ACTION_SNOOZE_MEDICATION
            putExtra(IntakeActionReceiver.EXTRA_MEDICATION_ID, medicationId)
            putExtra(IntakeActionReceiver.EXTRA_NOTIFICATION_ID, medicationId.toInt())
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (medicationId * 10 + 2).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "Час прийняти ліки"
        val message = "$medicationName ($dosageForm)$mealNote"

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_menu_agenda, "Прийняв", takePendingIntent)
            .addAction(android.R.drawable.ic_menu_recent_history, "Відкласти (15 хв)", snoozePendingIntent)
            .build()

        notificationManager.notify(medicationId.toInt(), notification)
    }

    fun cancelNotification(context: Context, notificationId: Int) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)
    }

    fun showNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Long,
        channelId: String = CHANNEL_REMINDERS
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(
                if (channelId == CHANNEL_REMINDERS) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId.toInt(), notification)
    }

    /**
     * Сповіщення про дозу з кнопками дій: «Прийняти» і «Відкласти» —
     * без відкриття застосунку. Обробка в AlarmReceiver.
     */
    fun showDoseNotification(context: Context, med: Medication, scheduledAt: Long) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val openApp = PendingIntent.getActivity(
            context,
            med.id.toInt(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val takeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ReminderScheduler.ACTION_TAKE_DOSE
            putExtra(ReminderScheduler.EXTRA_MEDICATION_ID, med.id)
            putExtra(ReminderScheduler.EXTRA_SCHEDULED_AT, scheduledAt)
        }
        val takePi = PendingIntent.getBroadcast(
            context,
            (med.id + 2_000_000).toInt(),
            takeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ReminderScheduler.ACTION_SNOOZE_DOSE
            putExtra(ReminderScheduler.EXTRA_MEDICATION_ID, med.id)
        }
        val snoozePi = PendingIntent.getBroadcast(
            context,
            (med.id + 3_000_000).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val mealText = if (med.takeBeforeMeal) " (до їжі)" else " (після їжі)"
        val message = "${med.name} — ${med.dosageForm}$mealText"

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Час прийняти ліки")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openApp)
            .addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_input_add, "Прийняти", takePi
                )
            )
            .addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_menu_close_clear_cancel, "Відкласти", snoozePi
                )
            )
            .setAutoCancel(true)
            .build()

        notificationManager.notify(med.id.toInt(), notification)
    }
}
