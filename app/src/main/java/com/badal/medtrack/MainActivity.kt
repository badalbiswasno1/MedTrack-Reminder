package com.badal.medtrack

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Calendar

class MainActivity : AppCompatActivity() {

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
            onDelete = { medicine -> confirmDelete(medicine) }
        )
        recyclerView.adapter = adapter

        findViewById<View>(R.id.addFab).setOnClickListener {
            startActivity(Intent(this, AddMedicineActivity::class.java))
        }

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
}
