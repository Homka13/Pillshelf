package com.example.pillshelf.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.pillshelf.data.local.PillshelfDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Після перезавантаження пристрою всі будильники зникають —
 * відновлюємо точні будильники для всіх препаратів із розкладом.
 * WorkManager відновлює свою періодику сам, тому тут лише будильники.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = PillshelfDatabase.getInstance(context)
                val meds = db.medicationDao().getAllMedicationsSync()
                for (med in meds) {
                    ReminderScheduler.scheduleNextFor(context, med)
                }
                if (SettingsRepository.isPriceMonitoringEnabled(context)) {
                    SettingsRepository.enqueuePriceChecks(context)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
