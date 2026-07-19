package com.badal.medtrack

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DoseActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TAKEN = "com.badal.medtrack.ACTION_TAKEN"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TAKEN) return

        val medicineId = intent.getLongExtra("medicineId", -1)
        val slotIndex = intent.getIntExtra("slotIndex", 0)
        val logId = intent.getLongExtra("logId", -1)
        val notificationId = intent.getIntExtra("notificationId", -1)

        if (medicineId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = MedicineRepository(context)
                repo.markDoseTaken(medicineId)
                if (logId != -1L) {
                    repo.markLogTaken(logId)
                }
                AlarmScheduler.cancelFollowUp(context, medicineId, slotIndex)

                if (notificationId != -1) {
                    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    manager.cancel(notificationId)
                }

                val medicine = repo.getById(medicineId)
                if (medicine != null && medicine.quantity <= medicine.lowStockThreshold) {
                    LowStockWorker.triggerNow(context)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
