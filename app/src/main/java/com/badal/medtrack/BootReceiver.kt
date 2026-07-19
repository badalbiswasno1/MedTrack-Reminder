package com.badal.medtrack

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = MedicineRepository(context)
                val medicines = repo.getAllList()
                for (medicine in medicines) {
                    val times = medicine.timesList()
                    times.forEachIndexed { index, timeStr ->
                        AlarmScheduler.scheduleDose(context, medicine.id, timeStr, index)
                    }
                }
                LowStockWorker.triggerNow(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
