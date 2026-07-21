package com.badal.medtrack

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DoseAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val medicineId = intent.getLongExtra("medicineId", -1)
        val slotIndex = intent.getIntExtra("slotIndex", 0)
        val isFollowUp = intent.getBooleanExtra("isFollowUp", false)
        val existingLogId = intent.getLongExtra("existingLogId", -1)

        if (medicineId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = MedicineRepository(context)
                val medicine = repo.getById(medicineId)
                if (medicine != null) {
                    val logId: Long
                    if (existingLogId != -1L) {
                        logId = existingLogId
                    } else if (!isFollowUp) {
                        logId = repo.createDoseLog(medicineId, slotIndex)
                    } else {
                        val pending = repo.getPendingLog(medicineId, slotIndex)
                        if (pending == null) {
                            pendingResult.finish()
                            return@launch
                        }
                        logId = pending.id
                    }

                    showDoseNotification(context, medicine, slotIndex, isFollowUp, logId)

                    if (!isFollowUp) {
                        AlarmScheduler.scheduleFollowUp(context, medicineId, slotIndex)
                        val times = medicine.timesList()
                        if (slotIndex < times.size) {
                            AlarmScheduler.scheduleDose(context, medicineId, times[slotIndex], slotIndex, medicine.repeatPattern, medicine.repeatDaysCsv)
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showDoseNotification(
        context: Context,
        medicine: Medicine,
        slotIndex: Int,
        isFollowUp: Boolean,
        logId: Long
    ) {
        val notifId = notificationId(medicine.id, slotIndex)

        val takenIntent = Intent(context, DoseActionReceiver::class.java).apply {
            action = DoseActionReceiver.ACTION_TAKEN
            putExtra("medicineId", medicine.id)
            putExtra("slotIndex", slotIndex)
            putExtra("logId", logId)
            putExtra("notificationId", notifId)
        }
        val takenPendingIntent = PendingIntent.getBroadcast(
            context, notifId + 1, takenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, DoseActionReceiver::class.java).apply {
            action = DoseActionReceiver.ACTION_SNOOZE
            putExtra("medicineId", medicine.id)
            putExtra("slotIndex", slotIndex)
            putExtra("logId", logId)
            putExtra("notificationId", notifId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context, notifId + 2, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val skipIntent = Intent(context, DoseActionReceiver::class.java).apply {
            action = DoseActionReceiver.ACTION_SKIP
            putExtra("medicineId", medicine.id)
            putExtra("slotIndex", slotIndex)
            putExtra("logId", logId)
            putExtra("notificationId", notifId)
        }
        val skipPendingIntent = PendingIntent.getBroadcast(
            context, notifId + 3, skipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (SettingsPrefs.isVoiceReminderEnabled(context)) {
            val voiceIntent = Intent(context, VoiceReminderService::class.java).apply {
                putExtra("medicineName", medicine.name)
                putExtra("medicineDose", medicine.dose)
            }
            context.startService(voiceIntent)
        }

        val title = if (isFollowUp) "মনে করিয়ে দিচ্ছি: ${medicine.name}" else "ওষুধ খাওয়ার সময় হয়েছে"
        val body = "${medicine.name} - ${medicine.dose}"

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_DOSE)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(android.R.drawable.checkbox_on_background, "✔ Taken", takenPendingIntent)
            .addAction(android.R.drawable.ic_menu_recent_history, "⏰ Snooze 10m", snoozePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "❌ Skip", skipPendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notifId, notification)
    }

    private fun notificationId(medicineId: Long, slotIndex: Int): Int =
        (medicineId.toString() + slotIndex).hashCode()
}
