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
}
