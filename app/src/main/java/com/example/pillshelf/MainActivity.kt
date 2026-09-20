package com.example.pillshelf

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.pillshelf.service.NotificationHelper
import com.example.pillshelf.service.ReminderScheduler
import com.example.pillshelf.service.SettingsRepository
import com.example.pillshelf.ui.PillshelfApp
import com.example.pillshelf.ui.theme.PillshelfTheme
import com.example.pillshelf.ui.viewmodel.PillshelfViewModel
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val viewModel: PillshelfViewModel by viewModels {
        PillshelfViewModel.provideFactory(application)
    }

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // Permission result handled gracefully
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        NotificationHelper.createNotificationChannels(this)
        requestNotificationPermission()
        scheduleBackgroundWorkers()

        setContent {
            PillshelfTheme {
                PillshelfApp(viewModel = viewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        observeMedicationsForAlarms()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    /**
     * Точні будильники доз: ставимо/оновлюємо при кожній зміні списку
     * препаратів. WorkManager лишається страховкою (15 хв), основний
     * механізм — setAlarmClock() через ReminderScheduler.
     */
    private fun observeMedicationsForAlarms() {
        lifecycleScope.launch {
            viewModel.allMedications.collect { meds ->
                for (med in meds) {
                    if (med.isOutOfStock()) {
                        ReminderScheduler.cancel(this@MainActivity, med.id)
                    } else {
                        ReminderScheduler.scheduleNextFor(this@MainActivity, med)
                    }
                }
            }
        }
    }

    private fun scheduleBackgroundWorkers() {
        val workManager = WorkManager.getInstance(applicationContext)

        // Reminder worker: страховочний періодичний перегляд розкладу
        val reminderRequest = PeriodicWorkRequestBuilder<com.example.pillshelf.service.ReminderWorker>(15, TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "pillshelf_reminders",
            ExistingPeriodicWorkPolicy.KEEP,
            reminderRequest
        )

        // Price check worker: ЛИШЕ якщо користувач увімкнув моніторинг
        // (за замовчуванням він вимкнений — див. SettingsRepository).
        if (SettingsRepository.isPriceMonitoringEnabled(this)) {
            SettingsRepository.enqueuePriceChecks(this)
        } else {
            workManager.cancelUniqueWork("pillshelf_price_checks")
        }
    }
}
