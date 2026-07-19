package com.badal.medtrack

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DoseActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TAKEN = "com.badal.medtrack.ACTION_TAKEN"
        const val ACTION_SNOOZE = "com.badal.medtrack.ACTION_SNOOZE"
        const val ACTION_SKIP = "com.badal.medtrack.ACTION_SKIP"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val medicineId = intent.getLongExtra("medicineId", -1)
        val slotIndex = intent.getIntExtra("slotIndex", 0)
        val logId = intent.getLongExtra("logId", -1)
        val notificationId = intent.getIntExtra("notificationId", -1)

        if (medicineId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    ACTION_TAKEN -> handleTaken(context, medicineId, slotIndex, logId, notificationId)
                    ACTION_SKIP -> handleSkip(context, medicineId, slotIndex, logId, notificationId)
                    ACTION_SNOOZE -> handleSnooze(context, medicineId, slotIndex, logId, notificationId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleTaken(context: Context, medicineId: Long, slotIndex: Int, logId: Long, notificationId: Int) {
        val repo = MedicineRepository(context)
        repo.markDoseTaken(medicineId)
        if (logId != -1L) {
            repo.markLogTaken(logId)
        }
        AlarmScheduler.cancelFollowUp(context, medicineId, slotIndex)
        dismissNotification(context, notificationId)

        val medicine = repo.getById(medicineId)
        if (medicine != null && medicine.quantity <= medicine.lowStockThreshold) {
            LowStockWorker.triggerNow(context)
        }
    }

    private suspend fun handleSkip(context: Context, medicineId: Long, slotIndex: Int, logId: Long, notificationId: Int) {
        val repo = MedicineRepository(context)
        if (logId != -1L) {
            repo.markLogSkipped(logId)
        }
        AlarmScheduler.cancelFollowUp(context, medicineId, slotIndex)
        dismissNotification(context, notificationId)
    }

    private fun handleSnooze(context: Context, medicineId: Long, slotIndex: Int, logId: Long, notificationId: Int) {
        dismissNotification(context, notificationId)

        val intent = Intent(context, DoseAlarmReceiver::class.java).apply {
            putExtra("medicineId", medicineId)
            putExtra("slotIndex", slotIndex)
            putExtra("isFollowUp", true)
            putExtra("existingLogId", logId)
        }
        val requestCode = ("snooze" + medicineId.toString() + slotIndex).hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAt = System.currentTimeMillis() + (10 * 60 * 1000)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    private fun dismissNotification(context: Context, notificationId: Int) {
        if (notificationId != -1) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(notificationId)
        }
    }
}
