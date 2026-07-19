package com.badal.medtrack

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class MissedDoseWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repo = MedicineRepository(applicationContext)
        repo.markStaleLogsMissed()
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "missed_dose_check"

        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<MissedDoseWorker>(6, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
