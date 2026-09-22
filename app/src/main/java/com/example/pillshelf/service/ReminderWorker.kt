package com.example.pillshelf.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            ReminderScheduler.rescheduleAll(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Log.e("ReminderWorker", "Error running reminder worker", e)
            Result.retry()
        }
    }
}
