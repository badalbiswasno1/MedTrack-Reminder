package com.badal.medtrack

import android.widget.Spinner

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

class AddMedicineActivity : BaseActivity() {

    private lateinit var repository: MedicineRepository
    private lateinit var nameInput: EditText
    private lateinit var genericNameInput: EditText
    private lateinit var doseInput: EditText
    private lateinit var quantityInput: EditText
    private lateinit var foodTimingInput: Spinner
    private val foodTimingOptions = arrayOf("খাওয়ার আগে", "খাওয়ার পরে", "খাওয়ার সাথে", "যেকোনো সময়")
    private lateinit var doctorNameInput: EditText
    private lateinit var expiryDateInput: EditText
    private lateinit var notesInput: EditText
    private lateinit var timesContainer: LinearLayout
    private lateinit var screenTitle: TextView

    private var editingMedicine: Medicine? = null
    private val timeSlots = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_medicine)

        repository = MedicineRepository(this)
        nameInput = findViewById(R.id.nameInput)

        intent.getStringExtra("prefillName")?.let { prefill ->
            nameInput.setText(prefill)
        }
        genericNameInput = findViewById(R.id.genericNameInput)
        doseInput = findViewById(R.id.doseInput)
        quantityInput = findViewById(R.id.quantityInput)
        foodTimingInput = findViewById(R.id.foodTimingInput)
        val foodTimingAdapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, foodTimingOptions)
        foodTimingInput.adapter = foodTimingAdapter
        doctorNameInput = findViewById(R.id.doctorNameInput)
        expiryDateInput = findViewById(R.id.expiryDateInput)
        notesInput = findViewById(R.id.notesInput)
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
                genericNameInput.setText(medicine.genericName)
                doseInput.setText(medicine.dose)
                quantityInput.setText(medicine.quantity.toString())
                val foodTimingIndex = foodTimingOptions.indexOf(medicine.foodTiming)
                if (foodTimingIndex >= 0) foodTimingInput.setSelection(foodTimingIndex)
                doctorNameInput.setText(medicine.doctorName)
                expiryDateInput.setText(medicine.expiryDate)
                notesInput.setText(medicine.notes)
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
        val genericName = genericNameInput.text.toString().trim()
        val dose = doseInput.text.toString().trim()
        val quantityStr = quantityInput.text.toString().trim()
        val foodTiming = foodTimingInput.selectedItem?.toString() ?: ""
        val doctorName = doctorNameInput.text.toString().trim()
        val expiryDate = expiryDateInput.text.toString().trim()
        val notes = notesInput.text.toString().trim()

        if (name.isEmpty() || dose.isEmpty() || quantityStr.isEmpty()) {
            Toast.makeText(this, "নাম, ডোজ ও পরিমাণ পূরণ করো", Toast.LENGTH_SHORT).show()
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
                    genericName = genericName,
                    dose = dose,
                    timesCsv = timeSlots.toCsv(),
                    quantity = quantity,
                    foodTiming = foodTiming,
                    doctorName = doctorName,
                    expiryDate = expiryDate,
                    notes = notes
                )
                repository.update(updated)
                timeSlots.forEachIndexed { index, time ->
                    AlarmScheduler.scheduleDose(this@AddMedicineActivity, updated.id, time, index)
                }
            } else {
                val medicine = Medicine(
                    name = name,
                    genericName = genericName,
                    dose = dose,
                    timesCsv = timeSlots.toCsv(),
                    quantity = quantity,
                    foodTiming = foodTiming,
                    doctorName = doctorName,
                    expiryDate = expiryDate,
                    notes = notes
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
