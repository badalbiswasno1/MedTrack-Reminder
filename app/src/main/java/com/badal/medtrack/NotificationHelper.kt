package com.badal.medtrack

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationHelper {

    const val CHANNEL_DOSE = "dose_channel"
    const val CHANNEL_STOCK = "stock_channel"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java)

        val doseChannel = NotificationChannel(
            CHANNEL_DOSE,
            "Medicine Dose Reminder",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminds you when it's time to take medicine"
        }

        val stockChannel = NotificationChannel(
            CHANNEL_STOCK,
            "Low Stock Alert",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Alerts when medicine stock is low"
        }

        manager.createNotificationChannel(doseChannel)
        manager.createNotificationChannel(stockChannel)
    }
}
