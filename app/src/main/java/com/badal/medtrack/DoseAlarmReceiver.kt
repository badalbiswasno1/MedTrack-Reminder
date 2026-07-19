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

        if (medicineId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = MedicineRepository(context)
                val medicine = repo.getById(medicineId)
                if (medicine != null) {
                    showDoseNotification(context, medicine, slotIndex, isFollowUp)
                    if (!isFollowUp) {
                        AlarmScheduler.scheduleFollowUp(context, medicineId, slotIndex)
                    }
                    // reschedule next day's dose for this slot
                    if (!isFollowUp) {
                        val times = medicine.timesList()
                        if (slotIndex < times.size) {
                            AlarmScheduler.scheduleDose(context, medicineId, times[slotIndex], slotIndex)
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
        isFollowUp: Boolean
    ) {
        val takenIntent = Intent(context, DoseActionReceiver::class.java).apply {
            action = DoseActionReceiver.ACTION_TAKEN
            putExtra("medicineId", medicine.id)
            putExtra("slotIndex", slotIndex)
            putExtra("notificationId", notificationId(medicine.id, slotIndex))
        }
        val takenPendingIntent = PendingIntent.getBroadcast(
            context, notificationId(medicine.id, slotIndex) + 5000, takenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isFollowUp) "মনে করিয়ে দিচ্ছি: ${medicine.name}" else "ওষুধ খাওয়ার সময় হয়েছে"
        val body = "${medicine.name} - ${medicine.dose}"

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_DOSE)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(android.R.drawable.checkbox_on_background, "Taken", takenPendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId(medicine.id, slotIndex), notification)
    }

    private fun notificationId(medicineId: Long, slotIndex: Int): Int =
        (medicineId.toString() + slotIndex).hashCode()
}
