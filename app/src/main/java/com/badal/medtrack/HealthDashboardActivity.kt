package com.badal.medtrack

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HealthDashboardActivity : AppCompatActivity() {

    private lateinit var repository: MedicineRepository
    private val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_health)

        repository = MedicineRepository(this)

        findViewById<TextView>(R.id.saveBpButton).setOnClickListener { saveBp() }
        findViewById<TextView>(R.id.saveSugarButton).setOnClickListener { saveSugar() }
        findViewById<TextView>(R.id.saveWeightButton).setOnClickListener { saveWeight() }

        loadLatest()
    }

    override fun onResume() {
        super.onResume()
        loadLatest()
    }

    private fun saveBp() {
        val sysInput = findViewById<EditText>(R.id.bpSystolicInput)
        val diaInput = findViewById<EditText>(R.id.bpDiastolicInput)
        val sys = sysInput.text.toString().toFloatOrNull()
        val dia = diaInput.text.toString().toFloatOrNull()

        if (sys == null || dia == null) {
            Toast.makeText(this, "সঠিক মান দিন", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            repository.insertHealthLog(
                HealthLog(type = HealthType.BP, value1 = sys, value2 = dia, recordedAt = System.currentTimeMillis())
            )
            sysInput.text.clear()
            diaInput.text.clear()
            loadLatest()
            Toast.makeText(this@HealthDashboardActivity, "সেভ হয়েছে", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveSugar() {
        val input = findViewById<EditText>(R.id.sugarInput)
        val value = input.text.toString().toFloatOrNull()

        if (value == null) {
            Toast.makeText(this, "সঠিক মান দিন", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            repository.insertHealthLog(
                HealthLog(type = HealthType.SUGAR, value1 = value, recordedAt = System.currentTimeMillis())
            )
            input.text.clear()
            loadLatest()
            Toast.makeText(this@HealthDashboardActivity, "সেভ হয়েছে", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveWeight() {
        val input = findViewById<EditText>(R.id.weightInput)
        val value = input.text.toString().toFloatOrNull()

        if (value == null) {
            Toast.makeText(this, "সঠিক মান দিন", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            repository.insertHealthLog(
                HealthLog(type = HealthType.WEIGHT, value1 = value, recordedAt = System.currentTimeMillis())
            )
            input.text.clear()
            loadLatest()
            Toast.makeText(this@HealthDashboardActivity, "সেভ হয়েছে", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadLatest() {
        lifecycleScope.launch {
            val bp = repository.getLatestHealthLog(HealthType.BP)
            val sugar = repository.getLatestHealthLog(HealthType.SUGAR)
            val weight = repository.getLatestHealthLog(HealthType.WEIGHT)

            findViewById<TextView>(R.id.latestBpText).text = if (bp != null)
                "সর্বশেষ: ${bp.value1.toInt()}/${bp.value2.toInt()} mmHg (${dateFormat.format(Date(bp.recordedAt))})"
            else "এখনো কোনো তথ্য নেই"

            findViewById<TextView>(R.id.latestSugarText).text = if (sugar != null)
                "সর্বশেষ: ${sugar.value1} mg/dL (${dateFormat.format(Date(sugar.recordedAt))})"
            else "এখনো কোনো তথ্য নেই"

            findViewById<TextView>(R.id.latestWeightText).text = if (weight != null)
                "সর্বশেষ: ${weight.value1} কেজি (${dateFormat.format(Date(weight.recordedAt))})"
            else "এখনো কোনো তথ্য নেই"
        }
    }
}
