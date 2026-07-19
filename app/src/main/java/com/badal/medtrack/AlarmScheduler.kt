package com.badal.medtrack

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object AlarmScheduler {

    fun scheduleDose(context: Context, medicineId: Long, timeStr: String, slotIndex: Int) {
        val parts = timeStr.split(":")
        if (parts.size != 2) return
        val hour = parts[0].toIntOrNull() ?: return
        val minute = parts[1].toIntOrNull() ?: return

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val requestCode = doseRequestCode(medicineId, slotIndex)

        val intent = Intent(context, DoseAlarmReceiver::class.java).apply {
            putExtra("medicineId", medicineId)
            putExtra("slotIndex", slotIndex)
            putExtra("isFollowUp", false)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            // Permission not granted, fall back to inexact
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            return
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }

    fun scheduleFollowUp(context: Context, medicineId: Long, slotIndex: Int) {
        val requestCode = followUpRequestCode(medicineId, slotIndex)

        val intent = Intent(context, DoseAlarmReceiver::class.java).apply {
            putExtra("medicineId", medicineId)
            putExtra("slotIndex", slotIndex)
            putExtra("isFollowUp", true)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAt = System.currentTimeMillis() + (30 * 60 * 1000)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            return
        }

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }

    fun cancelFollowUp(context: Context, medicineId: Long, slotIndex: Int) {
        val requestCode = followUpRequestCode(medicineId, slotIndex)
        val intent = Intent(context, DoseAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }

    fun cancelAllForMedicine(context: Context, medicineId: Long, slotCount: Int) {
        for (slot in 0 until slotCount) {
            val doseIntent = Intent(context, DoseAlarmReceiver::class.java)
            val dosePending = PendingIntent.getBroadcast(
                context, doseRequestCode(medicineId, slot), doseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(dosePending)
            cancelFollowUp(context, medicineId, slot)
        }
    }

    private fun doseRequestCode(medicineId: Long, slotIndex: Int): Int =
        (medicineId.toString() + "0" + slotIndex).hashCode()

    private fun followUpRequestCode(medicineId: Long, slotIndex: Int): Int =
        (medicineId.toString() + "1" + slotIndex).hashCode()
}
