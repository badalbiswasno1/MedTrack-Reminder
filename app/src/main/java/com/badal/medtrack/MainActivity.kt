package com.badal.medtrack

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
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

class MainActivity : AppCompatActivity() {

    private lateinit var repository: MedicineRepository
    private lateinit var adapter: MedicineAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        NotificationHelper.createChannels(this)
        repository = MedicineRepository(this)

        recyclerView = findViewById(R.id.medicineRecyclerView)
        emptyText = findViewById(R.id.emptyText)
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

        requestPermissionsIfNeeded()
        observeMedicines()
        LowStockWorker.schedulePeriodic(this)
    }

    private fun observeMedicines() {
        lifecycleScope.launch {
            repository.getAll().collect { list ->
                adapter.updateItems(list)
                emptyText.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                recyclerView.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
            }
        }
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
