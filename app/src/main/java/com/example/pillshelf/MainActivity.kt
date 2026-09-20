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
import androidx.work.WorkManager
import com.example.pillshelf.service.NotificationHelper
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
        initializeExactReminders()

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

    private fun initializeExactReminders() {
        // Cancel legacy background periodic workers to protect privacy and battery
        try {
            val workManager = WorkManager.getInstance(applicationContext)
            workManager.cancelUniqueWork("pillshelf_reminders")
            workManager.cancelUniqueWork("pillshelf_price_checks")
        } catch (e: Exception) {
            // Ignore if workmanager was not active
        }

        // Initialize exact alarm scheduling via AlarmManager
        com.example.pillshelf.service.ReminderScheduler.rescheduleAll(this)
    }
}
