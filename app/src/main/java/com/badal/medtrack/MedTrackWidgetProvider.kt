package com.badal.medtrack

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class MedTrackWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (id in appWidgetIds) {
                    performUpdate(context, appWidgetManager, id)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        suspend fun performUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val repository = MedicineRepository(context)

            val dayStart = Calendar.getInstance()
            dayStart.set(Calendar.HOUR_OF_DAY, 0)
            dayStart.set(Calendar.MINUTE, 0)
            dayStart.set(Calendar.SECOND, 0)
            dayStart.set(Calendar.MILLISECOND, 0)
            val dayEnd = dayStart.timeInMillis + 24 * 60 * 60 * 1000

            val logs = repository.getLogsInRange(dayStart.timeInMillis, dayEnd)
            val taken = logs.count { it.status == DoseStatus.TAKEN }
            val missed = logs.count { it.status == DoseStatus.MISSED }
            val total = logs.size
            val pending = logs.count { it.status == DoseStatus.PENDING }

            val views = RemoteViews(context.packageName, R.layout.widget_medtrack)
            views.setTextViewText(R.id.widgetTotalText, total.toString())
            views.setTextViewText(R.id.widgetTakenText, taken.toString())
            views.setTextViewText(R.id.widgetMissedText, missed.toString())
            views.setTextViewText(
                R.id.widgetNextDoseText,
                if (pending > 0) "$pending টি ওষুধ বাকি আছে" else "আজকের সব ওষুধ সম্পন্ন"
            )

            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetRoot, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            CoroutineScope(Dispatchers.IO).launch {
                performUpdate(context, appWidgetManager, appWidgetId)
            }
        }
    }
}
