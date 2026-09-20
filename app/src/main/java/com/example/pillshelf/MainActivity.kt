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
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.pillshelf.service.NotificationHelper
import com.example.pillshelf.service.PriceCheckWorker
import com.example.pillshelf.service.ReminderWorker
import com.example.pillshelf.ui.PillshelfApp
import com.example.pillshelf.ui.theme.PillshelfTheme
import com.example.pillshelf.ui.viewmodel.PillshelfViewModel
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

    private fun scheduleBackgroundWorkers() {
        val workManager = WorkManager.getInstance(applicationContext)

        // Reminder worker: runs periodically every 15 minutes
        val reminderRequest = PeriodicWorkRequestBuilder<ReminderWorker>(15, TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "pillshelf_reminders",
            ExistingPeriodicWorkPolicy.KEEP,
            reminderRequest
        )

        // Price check worker: runs every 12 hours when connected to unmetered or any network
        val priceConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val priceRequest = PeriodicWorkRequestBuilder<PriceCheckWorker>(12, TimeUnit.HOURS)
            .setConstraints(priceConstraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "pillshelf_price_checks",
            ExistingPeriodicWorkPolicy.KEEP,
            priceRequest
        )
    }
}
