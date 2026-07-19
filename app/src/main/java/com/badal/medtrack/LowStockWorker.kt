package com.badal.medtrack

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.*
import java.util.concurrent.TimeUnit

class LowStockWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repo = MedicineRepository(applicationContext)
        val lowStock = repo.getLowStockMedicines()

        if (lowStock.isNotEmpty()) {
            val names = lowStock.joinToString(", ") { it.name }
            val notification = NotificationCompat.Builder(applicationContext, NotificationHelper.CHANNEL_STOCK)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("ওষুধ কম আছে")
                .setContentText("এই ওষুধগুলো কিনতে হবে: $names")
                .setStyle(NotificationCompat.BigTextStyle().bigText("এই ওষুধগুলো কিনতে হবে: $names"))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(9999, notification)
        }

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "low_stock_check"

        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<LowStockWorker>(8, TimeUnit.HOURS)
                .setInitialDelay(1, TimeUnit.HOURS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun triggerNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<LowStockWorker>().build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
