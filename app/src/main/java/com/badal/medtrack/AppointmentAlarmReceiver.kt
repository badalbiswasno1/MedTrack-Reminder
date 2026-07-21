package com.badal.medtrack

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

class AppointmentAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val doctorName = intent.getStringExtra("doctorName") ?: ""
        val location = intent.getStringExtra("location") ?: ""
        val appointmentId = intent.getLongExtra("appointmentId", 0)

        val title = "ডাক্তারের অ্যাপয়েন্টমেন্ট"
        val body = if (location.isNotBlank()) "$doctorName - $location" else doctorName

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_DOSE)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify((100000 + appointmentId).toInt(), notification)
    }
}
