package com.badal.medtrack

import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class StatisticsActivity : BaseActivity() {

    private lateinit var repository: MedicineRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        repository = MedicineRepository(this)
        loadStats()
    }

    private fun loadStats() {
        lifecycleScope.launch {
            val allLogs = repository.getAllLogsAsc()
            val totalTaken = repository.getTotalTakenCount()
            val totalMissed = repository.getTotalMissedCount()

            findViewById<TextView>(R.id.adherenceText).text =
                "${StatsCalculator.adherencePercent(totalTaken, totalMissed)}%"
            findViewById<TextView>(R.id.totalTakenText).text = totalTaken.toString()
            findViewById<TextView>(R.id.totalMissedText).text = totalMissed.toString()
            findViewById<TextView>(R.id.currentStreakText).text =
                StatsCalculator.currentStreak(allLogs).toString()
            findViewById<TextView>(R.id.longestStreakText).text =
                StatsCalculator.longestStreak(allLogs).toString()

            buildWeeklyChart(StatsCalculator.weeklyBreakdown(allLogs))
        }
    }

    private fun buildWeeklyChart(days: List<DayStat>) {
        val container = findViewById<LinearLayout>(R.id.weeklyChartContainer)
        container.removeAllViews()

        val maxCount = days.maxOfOrNull { it.taken + it.missed }?.coerceAtLeast(1) ?: 1

        for (day in days) {
            val column = LinearLayout(this)
            column.orientation = LinearLayout.VERTICAL
            column.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            val columnParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            columnParams.marginEnd = 6
            columnParams.marginStart = 6
            column.layoutParams = columnParams

            if (day.taken > 0) {
                val takenBar = android.view.View(this)
                val heightPx = ((day.taken.toFloat() / maxCount) * 300).toInt().coerceAtLeast(6)
                takenBar.layoutParams = LinearLayout.LayoutParams(40, heightPx)
                takenBar.setBackgroundResource(R.drawable.bar_taken)
                column.addView(takenBar)
            }

            if (day.missed > 0) {
                val missedBar = android.view.View(this)
                val heightPx = ((day.missed.toFloat() / maxCount) * 100).toInt().coerceAtLeast(6)
                val params = LinearLayout.LayoutParams(40, heightPx)
                params.topMargin = 4
                missedBar.layoutParams = params
                missedBar.setBackgroundResource(R.drawable.bar_missed)
                column.addView(missedBar)
            }

            val label = TextView(this)
            label.text = day.dateLabel
            label.textSize = 11f
            label.setTextColor(getColor(R.color.on_surface_variant))
            label.gravity = Gravity.CENTER
            val labelParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            labelParams.topMargin = 8
            label.layoutParams = labelParams
            column.addView(label)

            container.addView(column)
        }
    }
}
