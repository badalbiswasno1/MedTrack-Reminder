package com.badal.medtrack

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Calendar

class AddMedicineActivity : AppCompatActivity() {

    private lateinit var repository: MedicineRepository
    private lateinit var nameInput: EditText
    private lateinit var doseInput: EditText
    private lateinit var quantityInput: EditText
    private lateinit var timesContainer: LinearLayout
    private lateinit var screenTitle: TextView

    private var editingMedicine: Medicine? = null
    private val timeSlots = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_medicine)

        repository = MedicineRepository(this)
        nameInput = findViewById(R.id.nameInput)
        doseInput = findViewById(R.id.doseInput)
        quantityInput = findViewById(R.id.quantityInput)
        timesContainer = findViewById(R.id.timesContainer)
        screenTitle = findViewById(R.id.screenTitle)

        findViewById<View>(R.id.addTimeButton).setOnClickListener { pickTime() }
        findViewById<View>(R.id.saveButton).setOnClickListener { saveMedicine() }

        val medicineId = intent.getLongExtra("medicineId", -1)
        if (medicineId != -1L) {
            screenTitle.text = "ওষুধ এডিট করো"
            loadExisting(medicineId)
        }
    }

    private fun loadExisting(id: Long) {
        lifecycleScope.launch {
            val medicine = repository.getById(id)
            if (medicine != null) {
                editingMedicine = medicine
                nameInput.setText(medicine.name)
                doseInput.setText(medicine.dose)
                quantityInput.setText(medicine.quantity.toString())
                timeSlots.clear()
                timeSlots.addAll(medicine.timesList())
                refreshTimeRows()
            }
        }
    }

    private fun pickTime() {
        val calendar = Calendar.getInstance()
        TimePickerDialog(
            this,
            { _, hour, minute ->
                val timeStr = String.format("%02d:%02d", hour, minute)
                timeSlots.add(timeStr)
                refreshTimeRows()
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun refreshTimeRows() {
        timesContainer.removeAllViews()
        timeSlots.forEachIndexed { index, time ->
            val row = LayoutInflater.from(this).inflate(R.layout.item_time_row, timesContainer, false)
            row.findViewById<TextView>(R.id.timeDisplay).text = time
            row.findViewById<TextView>(R.id.removeTimeButton).setOnClickListener {
                timeSlots.removeAt(index)
                refreshTimeRows()
            }
            timesContainer.addView(row)
        }
    }

    private fun saveMedicine() {
        val name = nameInput.text.toString().trim()
        val dose = doseInput.text.toString().trim()
        val quantityStr = quantityInput.text.toString().trim()

        if (name.isEmpty() || dose.isEmpty() || quantityStr.isEmpty()) {
            Toast.makeText(this, "সব ঘর পূরণ করো", Toast.LENGTH_SHORT).show()
            return
        }
        if (timeSlots.isEmpty()) {
            Toast.makeText(this, "অন্তত একটা সময় যোগ করো", Toast.LENGTH_SHORT).show()
            return
        }

        val quantity = quantityStr.toIntOrNull()
        if (quantity == null) {
            Toast.makeText(this, "সঠিক সংখ্যা দাও", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val existing = editingMedicine

            if (existing != null) {
                AlarmScheduler.cancelAllForMedicine(this@AddMedicineActivity, existing.id, existing.timesList().size)
                val updated = existing.copy(
                    name = name,
                    dose = dose,
                    timesCsv = timeSlots.toCsv(),
                    quantity = quantity
                )
                repository.update(updated)
                timeSlots.forEachIndexed { index, time ->
                    AlarmScheduler.scheduleDose(this@AddMedicineActivity, updated.id, time, index)
                }
            } else {
                val medicine = Medicine(
                    name = name,
                    dose = dose,
                    timesCsv = timeSlots.toCsv(),
                    quantity = quantity
                )
                val newId = repository.insert(medicine)
                timeSlots.forEachIndexed { index, time ->
                    AlarmScheduler.scheduleDose(this@AddMedicineActivity, newId, time, index)
                }
            }

            Toast.makeText(this@AddMedicineActivity, "সেভ হয়েছে", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
