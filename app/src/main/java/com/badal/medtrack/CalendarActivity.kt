package com.badal.medtrack

import android.os.Bundle
import android.view.Gravity
import android.widget.GridLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class CalendarActivity : AppCompatActivity() {

    private lateinit var repository: MedicineRepository
    private val displayCal = Calendar.getInstance()
    private val monthNamesBn = listOf(
        "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
        "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
    )
    private val weekDaysBn = listOf("রবি", "সোম", "মঙ্গল", "বুধ", "বৃহঃ", "শুক্র", "শনি")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        repository = MedicineRepository(this)

        setupWeekDayHeader()

        findViewById<android.widget.ImageButton>(R.id.prevMonthBtn).setOnClickListener {
            displayCal.add(Calendar.MONTH, -1)
            loadMonth()
        }

        findViewById<android.widget.ImageButton>(R.id.nextMonthBtn).setOnClickListener {
            displayCal.add(Calendar.MONTH, 1)
            loadMonth()
        }

        loadMonth()
    }

    private fun setupWeekDayHeader() {
        val header = findViewById<GridLayout>(R.id.weekDayHeader)
        header.removeAllViews()
        for (day in weekDaysBn) {
            val tv = TextView(this)
            tv.text = day
            tv.textSize = 11f
            tv.setTextColor(getColor(R.color.on_surface_variant))
            tv.gravity = Gravity.CENTER
            val params = GridLayout.LayoutParams()
            params.width = 0
            params.height = GridLayout.LayoutParams.WRAP_CONTENT
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            tv.layoutParams = params
            header.addView(tv)
        }
    }

    private fun loadMonth() {
        findViewById<TextView>(R.id.monthYearText).text =
            "${monthNamesBn[displayCal.get(Calendar.MONTH)]} ${displayCal.get(Calendar.YEAR)}"

        lifecycleScope.launch {
            val monthStart = Calendar.getInstance()
            monthStart.timeInMillis = displayCal.timeInMillis
            monthStart.set(Calendar.DAY_OF_MONTH, 1)
            monthStart.set(Calendar.HOUR_OF_DAY, 0)
            monthStart.set(Calendar.MINUTE, 0)
            monthStart.set(Calendar.SECOND, 0)
            monthStart.set(Calendar.MILLISECOND, 0)

            val monthEnd = Calendar.getInstance()
            monthEnd.timeInMillis = monthStart.timeInMillis
            monthEnd.add(Calendar.MONTH, 1)

            val logs = repository.getLogsInRange(monthStart.timeInMillis, monthEnd.timeInMillis)
            val logsByDay = logs.groupBy { log ->
                val c = Calendar.getInstance()
                c.timeInMillis = log.scheduledDateTime
                c.get(Calendar.DAY_OF_MONTH)
            }

            buildGrid(monthStart, logsByDay)
        }
    }

    private fun buildGrid(monthStart: Calendar, logsByDay: Map<Int, List<DoseLog>>) {
        val grid = findViewById<GridLayout>(R.id.calendarGrid)
        grid.removeAllViews()

        val firstDayOfWeek = monthStart.get(Calendar.DAY_OF_WEEK) - 1
        val daysInMonth = monthStart.getActualMaximum(Calendar.DAY_OF_MONTH)

        val today = Calendar.getInstance()
        val isCurrentMonth = today.get(Calendar.MONTH) == monthStart.get(Calendar.MONTH) &&
                today.get(Calendar.YEAR) == monthStart.get(Calendar.YEAR)
        val todayDate = today.get(Calendar.DAY_OF_MONTH)

        for (i in 0 until firstDayOfWeek) {
            val empty = TextView(this)
            val params = GridLayout.LayoutParams()
            params.width = 0
            params.height = dp(44)
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            empty.layoutParams = params
            grid.addView(empty)
        }

        for (day in 1..daysInMonth) {
            val dayLogs = logsByDay[day] ?: emptyList()
            val taken = dayLogs.count { it.status == DoseStatus.TAKEN }
            val missed = dayLogs.count { it.status == DoseStatus.MISSED }
            val pending = dayLogs.count { it.status == DoseStatus.PENDING }

            val isFuture = isCurrentMonth && day > todayDate
            val isToday = isCurrentMonth && day == todayDate

            val bgRes = when {
                dayLogs.isEmpty() -> R.drawable.bg_day_empty
                isFuture || (isToday && pending > 0 && missed == 0 && taken == 0) -> R.drawable.bg_day_upcoming
                missed > 0 && taken > 0 -> R.drawable.bg_day_partial
                missed > 0 -> R.drawable.bg_day_missed
                taken > 0 -> R.drawable.bg_day_taken
                else -> R.drawable.bg_day_empty
            }

            val cell = TextView(this)
            cell.text = day.toString()
            cell.gravity = Gravity.CENTER
            cell.textSize = 13f
            cell.setTextColor(if (bgRes == R.drawable.bg_day_empty) getColor(R.color.on_surface) else getColor(R.color.white))
            cell.setBackgroundResource(bgRes)

            val params = GridLayout.LayoutParams()
            params.width = 0
            params.height = dp(44)
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            params.setMargins(dp(3), dp(3), dp(3), dp(3))
            cell.layoutParams = params

            if (isToday) {
                cell.setTypeface(null, android.graphics.Typeface.BOLD)
            }

            grid.addView(cell)
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
