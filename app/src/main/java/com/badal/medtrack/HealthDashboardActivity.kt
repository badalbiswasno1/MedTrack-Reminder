package com.badal.medtrack

import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HealthDashboardActivity : BaseActivity() {

    private lateinit var repository: MedicineRepository
    private val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    private fun noDataLabel(): String =
        if (LocaleHelper.getLanguage(this) == "en") "No data yet" else "এখনো কোনো তথ্য নেই"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_health)

        repository = MedicineRepository(this)

        findViewById<TextView>(R.id.smokingAddButton).setOnClickListener { quickAdd(HealthType.SMOKING, it) }
        findViewById<TextView>(R.id.waterAddButton).setOnClickListener { quickAdd(HealthType.WATER, it) }

        findViewById<TextView>(R.id.saveHeartRateButton).setOnClickListener { saveHeartRate() }
        findViewById<TextView>(R.id.saveBpButton).setOnClickListener { saveBp() }
        findViewById<TextView>(R.id.saveSugarButton).setOnClickListener { saveSugar() }
        findViewById<TextView>(R.id.saveWeightButton).setOnClickListener { saveWeight() }
        findViewById<TextView>(R.id.saveLipidButton).setOnClickListener { saveLipid() }

        loadAll()
    }

    override fun onResume() {
        super.onResume()
        loadAll()
    }

    private fun playPulse(view: View) {
        val anim = AnimationUtils.loadAnimation(this, R.anim.pulse_scale)
        view.startAnimation(anim)
    }

    private fun animateIn(view: View) {
        val anim = AnimationUtils.loadAnimation(this, R.anim.fade_in_up)
        view.startAnimation(anim)
    }

    private fun quickAdd(type: String, triggerView: View) {
        playPulse(triggerView)
        lifecycleScope.launch {
            repository.insertHealthLog(HealthLog(type = type, value1 = 1f, recordedAt = System.currentTimeMillis()))
            loadCounters()
        }
    }

    private fun saveHeartRate() {
        val input = findViewById<EditText>(R.id.heartRateInput)
        val value = input.text.toString().toFloatOrNull()
        if (value == null) {
            Toast.makeText(this, "সঠিক মান দিন", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            repository.insertHealthLog(HealthLog(type = HealthType.HEART_RATE, value1 = value, recordedAt = System.currentTimeMillis()))
            input.text.clear()
            loadAll()
            Toast.makeText(this@HealthDashboardActivity, "সেভ হয়েছে", Toast.LENGTH_SHORT).show()
        }
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
            repository.insertHealthLog(HealthLog(type = HealthType.BP, value1 = sys, value2 = dia, recordedAt = System.currentTimeMillis()))
            sysInput.text.clear()
            diaInput.text.clear()
            loadAll()
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
            repository.insertHealthLog(HealthLog(type = HealthType.SUGAR, value1 = value, recordedAt = System.currentTimeMillis()))
            input.text.clear()
            loadAll()
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
            repository.insertHealthLog(HealthLog(type = HealthType.WEIGHT, value1 = value, recordedAt = System.currentTimeMillis()))
            input.text.clear()
            loadAll()
            Toast.makeText(this@HealthDashboardActivity, "সেভ হয়েছে", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveLipid() {
        val totalInput = findViewById<EditText>(R.id.lipidTotalInput)
        val ldlInput = findViewById<EditText>(R.id.lipidLdlInput)
        val total = totalInput.text.toString().toFloatOrNull()
        val ldl = ldlInput.text.toString().toFloatOrNull()
        if (total == null || ldl == null) {
            Toast.makeText(this, "সঠিক মান দিন", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            repository.insertHealthLog(HealthLog(type = HealthType.LIPID, value1 = total, value2 = ldl, recordedAt = System.currentTimeMillis()))
            totalInput.text.clear()
            ldlInput.text.clear()
            loadAll()
            Toast.makeText(this@HealthDashboardActivity, "সেভ হয়েছে", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadAll() {
        loadCounters()
        lifecycleScope.launch {
            val heartRate = repository.getLatestHealthLog(HealthType.HEART_RATE)
            val bp = repository.getLatestHealthLog(HealthType.BP)
            val sugar = repository.getLatestHealthLog(HealthType.SUGAR)
            val weight = repository.getLatestHealthLog(HealthType.WEIGHT)
            val lipid = repository.getLatestHealthLog(HealthType.LIPID)

            val heartRateText = findViewById<TextView>(R.id.latestHeartRateText)
            heartRateText.text = if (heartRate != null)
                "সর্বশেষ: ${heartRate.value1.toInt()} bpm (${dateFormat.format(Date(heartRate.recordedAt))})"
            else noDataLabel()
            animateIn(heartRateText)

            val bpText = findViewById<TextView>(R.id.latestBpText)
            bpText.text = if (bp != null)
                "সর্বশেষ: ${bp.value1.toInt()}/${bp.value2.toInt()} mmHg (${dateFormat.format(Date(bp.recordedAt))})"
            else noDataLabel()
            animateIn(bpText)

            val sugarText = findViewById<TextView>(R.id.latestSugarText)
            sugarText.text = if (sugar != null)
                "সর্বশেষ: ${sugar.value1} mg/dL (${dateFormat.format(Date(sugar.recordedAt))})"
            else noDataLabel()
            animateIn(sugarText)

            val weightText = findViewById<TextView>(R.id.latestWeightText)
            weightText.text = if (weight != null)
                "সর্বশেষ: ${weight.value1} কেজি (${dateFormat.format(Date(weight.recordedAt))})"
            else noDataLabel()
            animateIn(weightText)

            val lipidText = findViewById<TextView>(R.id.latestLipidText)
            lipidText.text = if (lipid != null)
                "সর্বশেষ: মোট ${lipid.value1.toInt()}, LDL ${lipid.value2.toInt()} mg/dL (${dateFormat.format(Date(lipid.recordedAt))})"
            else noDataLabel()
            animateIn(lipidText)
        }
    }

    private fun loadCounters() {
        lifecycleScope.launch {
            val smokingCount = repository.getTodayCount(HealthType.SMOKING)
            val waterCount = repository.getTodayCount(HealthType.WATER)
            findViewById<TextView>(R.id.smokingCountText).text = smokingCount.toString()
            findViewById<TextView>(R.id.waterCountText).text = waterCount.toString()
        }
    }
}
