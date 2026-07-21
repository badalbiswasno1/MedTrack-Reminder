package com.badal.medtrack

import android.widget.LinearLayout

import android.widget.Toast

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.Settings
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.util.Calendar

class MainActivity : BaseActivity() {

    private lateinit var repository: MedicineRepository
    private lateinit var adapter: MedicineAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var greetingText: TextView
    private lateinit var totalCountText: TextView
    private lateinit var takenCountText: TextView
    private lateinit var missedCountText: TextView
    private lateinit var nextDoseCard: View
    private lateinit var nextDoseName: TextView
    private lateinit var nextDoseCountdown: TextView

    private var countdownTimer: CountDownTimer? = null

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        NotificationHelper.createChannels(this)
        repository = MedicineRepository(this)

        recyclerView = findViewById(R.id.medicineRecyclerView)
        emptyText = findViewById(R.id.emptyText)
        greetingText = findViewById(R.id.greetingText)
        totalCountText = findViewById(R.id.totalCountText)
        takenCountText = findViewById(R.id.takenCountText)
        missedCountText = findViewById(R.id.missedCountText)
        nextDoseCard = findViewById(R.id.nextDoseCard)
        nextDoseName = findViewById(R.id.nextDoseName)
        nextDoseCountdown = findViewById(R.id.nextDoseCountdown)

        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = MedicineAdapter(
            items = emptyList(),
            onEdit = { medicine ->
                val intent = Intent(this, AddMedicineActivity::class.java)
                intent.putExtra("medicineId", medicine.id)
                startActivity(intent)
            },
            onDelete = { medicine -> confirmDelete(medicine) },
            onTake = { medicine -> markMedicineTaken(medicine) },
            onCardClick = { medicine ->
                val intent = Intent(this, MedicineDetailActivity::class.java)
                intent.putExtra("medicineId", medicine.id)
                startActivity(intent)
            }
        )
        recyclerView.adapter = adapter
        attachSwipeGestures()

        setupHomeCarousel()

        findViewById<TextView>(R.id.motivationalQuoteText).text = MotivationalQuotes.getRandomQuote(this)

        


        setGreeting()
        requestPermissionsIfNeeded()
        observeMedicines()
        LowStockWorker.schedulePeriodic(this)
        MissedDoseWorker.schedulePeriodic(this)
    }

    override fun onResume() {
        super.onResume()
        loadDashboardStats()
    }

    override fun onDestroy() {
        super.onDestroy()
        countdownTimer?.cancel()
    }

    private fun setGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        greetingText.text = when {
            hour < 12 -> "শুভ সকাল"
            hour < 17 -> "শুভ বিকাল"
            else -> "শুভ সন্ধ্যা"
        }
    }

    private fun observeMedicines() {
        lifecycleScope.launch {
            repository.getAll().collect { list ->
                adapter.updateItems(list)
                emptyText.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                recyclerView.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
                loadDashboardStats()
            }
        }
    }

    private fun refreshWidgets() {
        val manager = android.appwidget.AppWidgetManager.getInstance(this)
        val ids = manager.getAppWidgetIds(android.content.ComponentName(this, MedTrackWidgetProvider::class.java))
        for (id in ids) {
            MedTrackWidgetProvider.updateWidget(this, manager, id)
        }
    }

    private fun loadDashboardStats() {
        lifecycleScope.launch {
            val medicines = repository.getAllList()
            val todayLogs = repository.getTodayLogs()

            val totalSlots = medicines.sumOf { it.timesList().size }
            val taken = todayLogs.count { it.status == DoseStatus.TAKEN }
            val missed = todayLogs.count { it.status == DoseStatus.MISSED }

            totalCountText.text = totalSlots.toString()
            takenCountText.text = taken.toString()
            missedCountText.text = missed.toString()

            showNextDose(medicines)
        }
    }

    private fun showNextDose(medicines: List<Medicine>) {
        countdownTimer?.cancel()

        var soonestTimeMillis: Long = Long.MAX_VALUE
        var soonestName = ""

        val now = Calendar.getInstance()

        for (medicine in medicines) {
            for (timeStr in medicine.timesList()) {
                val parts = timeStr.split(":")
                if (parts.size != 2) continue
                val hour = parts[0].toIntOrNull() ?: continue
                val minute = parts[1].toIntOrNull() ?: continue

                val candidate = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
                }

                if (candidate.timeInMillis < soonestTimeMillis) {
                    soonestTimeMillis = candidate.timeInMillis
                    soonestName = medicine.name
                }
            }
        }

        if (soonestTimeMillis == Long.MAX_VALUE) {
            nextDoseCard.visibility = View.GONE
            return
        }

        nextDoseCard.visibility = View.VISIBLE
        nextDoseName.text = soonestName

        val remaining = soonestTimeMillis - System.currentTimeMillis()
        if (remaining <= 0) {
            nextDoseCountdown.text = "এখনই"
            return
        }

        countdownTimer = object : CountDownTimer(remaining, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val h = millisUntilFinished / 3600000
                val m = (millisUntilFinished % 3600000) / 60000
                val s = (millisUntilFinished % 60000) / 1000
                nextDoseCountdown.text = String.format("%02d:%02d:%02d", h, m, s)
            }

            override fun onFinish() {
                nextDoseCountdown.text = "এখনই"
                loadDashboardStats()
            }
        }.start()
    }

    private fun markMedicineTaken(medicine: Medicine) {
        lifecycleScope.launch {
            if (medicine.quantity <= 0) return@launch
            repository.markDoseTaken(medicine.id)
            val slotIndex = 0
            val pending = repository.getPendingLog(medicine.id, slotIndex)
            if (pending != null) {
                repository.markLogTaken(pending.id)
            }
            loadDashboardStats()
        }
    }

    private fun attachSwipeGestures() {
        val callback = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val medicine = adapter.getItemAt(position)
                if (direction == ItemTouchHelper.RIGHT) {
                    markMedicineTaken(medicine)
                } else {
                    val intent = Intent(this@MainActivity, AddMedicineActivity::class.java)
                    intent.putExtra("medicineId", medicine.id)
                    startActivity(intent)
                }
                adapter.notifyItemChanged(position)
            }

            override fun onChildDraw(
                c: Canvas, rv: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val paint = android.graphics.Paint()
                if (dX > 0) paint.color = Color.parseColor("#2E7D32") else paint.color = Color.parseColor("#00695C")
                c.drawRoundRect(
                    itemView.left.toFloat(), itemView.top.toFloat(),
                    itemView.right.toFloat(), itemView.bottom.toFloat(),
                    20f, 20f, paint
                )
                super.onChildDraw(c, rv, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(recyclerView)
    }

    private fun confirmDelete(medicine: Medicine) {
        AlertDialog.Builder(this)
            .setTitle("ডিলিট করবে?")
            .setMessage("${medicine.name} ডিলিট করতে চাও?")
            .setPositiveButton("হ্যাঁ") { _, _ ->
                lifecycleScope.launch {
                    AlarmScheduler.cancelAllForMedicine(this@MainActivity, medicine.id, medicine.timesList().size)
                    repository.delete(medicine)
                }
            }
            .setNegativeButton("না", null)
            .show()
    }

    private fun requestPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                AlertDialog.Builder(this)
                    .setTitle("পারমিশন দরকার")
                    .setMessage("সঠিক সময়ে notification পেতে Exact Alarm পারমিশন দাও।")
                    .setPositiveButton("সেটিংসে যাও") { _, _ ->
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        intent.data = Uri.parse("package:$packageName")
                        startActivity(intent)
                    }
                    .setNegativeButton("বাদ দাও", null)
                    .show()
            }
        }
    }


    private fun setupHomeCarousel() {
        val actions = listOf(
            HomeAction("📊 বিস্তারিত পরিসংখ্যান দেখো", R.color.primary) {
                startActivity(Intent(this, StatisticsActivity::class.java))
            },
            HomeAction("📅 ক্যালেন্ডার দেখো", R.color.primary) {
                startActivity(Intent(this, CalendarActivity::class.java))
            },
            HomeAction("⚠️ কম স্টক থাকা ওষুধ", R.color.danger) {
                startActivity(Intent(this, LowStockActivity::class.java))
            },
            HomeAction("🔍 ওষুধ খুঁজুন", R.color.secondary) {
                startActivity(Intent(this, SearchActivity::class.java))
            },
            HomeAction("❤️ স্বাস্থ্য ড্যাশবোর্ড", R.color.primary) {
                startActivity(Intent(this, HealthDashboardActivity::class.java))
            },
            HomeAction("📷 প্রেসক্রিপশন স্ক্যান করুন", R.color.secondary) {
                startActivity(Intent(this, OcrScanActivity::class.java))
            },
            HomeAction("⚙️ সেটিংস", R.color.accent) {
                startActivity(Intent(this, SettingsActivity::class.java))
            },
            HomeAction("❓ সাহায্য কেন্দ্র", R.color.primary) {
                startActivity(Intent(this, HelpCenterActivity::class.java))
            },
            HomeAction("🩺 ডাক্তারের অ্যাপয়েন্টমেন্ট", R.color.secondary) {
                startActivity(Intent(this, AppointmentActivity::class.java))
            }
        )

        val adapter = HomeCarouselAdapter(actions)
        val pager = findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.homeCarousel)
        pager.adapter = adapter

        val dotsContainer = findViewById<LinearLayout>(R.id.carouselDots)
        val pageCount = adapter.getRealPageCount()
        val dots = mutableListOf<View>()

        if (pageCount > 1) {
            for (i in 0 until pageCount) {
                val dot = View(this)
                val size = (8 * resources.displayMetrics.density).toInt()
                val params = LinearLayout.LayoutParams(size, size)
                params.marginStart = (4 * resources.displayMetrics.density).toInt()
                params.marginEnd = (4 * resources.displayMetrics.density).toInt()
                dot.layoutParams = params
                dot.setBackgroundResource(if (i == 0) R.drawable.dot_active else R.drawable.dot_inactive)
                dotsContainer.addView(dot)
                dots.add(dot)
            }

            pager.setCurrentItem(adapter.getStartPosition(), false)

            pager.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    val realPage = position % pageCount
                    dots.forEachIndexed { index, dot ->
                        dot.setBackgroundResource(if (index == realPage) R.drawable.dot_active else R.drawable.dot_inactive)
                    }
                }
            })
        }
    }
}