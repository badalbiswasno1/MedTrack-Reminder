package com.badal.medtrack

import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import android.widget.ImageView
import java.io.File
import java.io.FileOutputStream

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
    private lateinit var medicineTypeInput: Spinner
    private val medicineTypeOptions = arrayOf("ট্যাবলেট", "ক্যাপসুল", "সিরাপ", "ইনজেকশন", "ড্রপস", "ইনহেলার")
    private lateinit var repeatPatternInput: Spinner
    private val repeatPatternOptions = arrayOf("প্রতিদিন", "একদিন পরপর", "নির্দিষ্ট দিন")
    private val repeatPatternValues = arrayOf("DAILY", "ALTERNATE", "SPECIFIC")
    private lateinit var colorSwatchContainer: LinearLayout
    private lateinit var specificDaysContainer: LinearLayout
    private val presetColors = listOf("#00695C", "#FF8A65", "#D32F2F", "#1976D2", "#7B1FA2", "#F57C00")
    private val weekDayNames = listOf("রবি", "সোম", "মঙ্গল", "বুধ", "বৃহঃ", "শুক্র", "শনি")
    private var selectedColor = "#00695C"
    private val selectedDays = mutableSetOf<Int>()
    private lateinit var photoPreview: ImageView
    private var currentPhotoPath: String? = null
    private var pendingCameraUri: Uri? = null

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) copyImageToInternalStorage(uri)
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && pendingCameraUri != null) copyImageToInternalStorage(pendingCameraUri!!)
    }

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera()
    }
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

        medicineTypeInput = findViewById(R.id.medicineTypeInput)
        val medicineTypeAdapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, medicineTypeOptions)
        medicineTypeInput.adapter = medicineTypeAdapter

        repeatPatternInput = findViewById(R.id.repeatPatternInput)
        val repeatAdapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, repeatPatternOptions)
        repeatPatternInput.adapter = repeatAdapter
        repeatPatternInput.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                specificDaysContainer.visibility = if (repeatPatternValues[position] == "SPECIFIC") View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        colorSwatchContainer = findViewById(R.id.colorSwatchContainer)
        specificDaysContainer = findViewById(R.id.specificDaysContainer)
        setupColorSwatches()
        setupDayCheckboxes()

        photoPreview = findViewById(R.id.photoPreview)
        findViewById<View>(R.id.pickPhotoCameraButton).setOnClickListener { checkCameraPermissionAndLaunch() }
        findViewById<View>(R.id.pickPhotoGalleryButton).setOnClickListener { galleryLauncher.launch("image/*") }
        findViewById<View>(R.id.removePhotoButton).setOnClickListener { clearPhoto() }
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

                val typeIndex = medicineTypeOptions.indexOf(medicine.medicineType)
                if (typeIndex >= 0) medicineTypeInput.setSelection(typeIndex)

                selectedColor = medicine.colorHex
                setupColorSwatches()

                val repeatIndex = repeatPatternValues.indexOf(medicine.repeatPattern)
                if (repeatIndex >= 0) repeatPatternInput.setSelection(repeatIndex)

                if (medicine.repeatDaysCsv.isNotBlank()) {
                    selectedDays.clear()
                    medicine.repeatDaysCsv.split(",").mapNotNull { it.toIntOrNull() }.forEach { selectedDays.add(it) }
                    setupDayCheckboxes()
                }

                if (medicine.photoPath.isNotBlank() && File(medicine.photoPath).exists()) {
                    currentPhotoPath = medicine.photoPath
                    showPhotoPreview(medicine.photoPath)
                }
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

    private fun setupColorSwatches() {
        colorSwatchContainer.removeAllViews()
        for (colorHex in presetColors) {
            val swatch = View(this)
            val size = (36 * resources.displayMetrics.density).toInt()
            val params = LinearLayout.LayoutParams(size, size)
            params.marginEnd = (10 * resources.displayMetrics.density).toInt()
            swatch.layoutParams = params

            val shape = android.graphics.drawable.GradientDrawable()
            shape.shape = android.graphics.drawable.GradientDrawable.OVAL
            shape.setColor(android.graphics.Color.parseColor(colorHex))
            swatch.background = shape

            swatch.setOnClickListener {
                selectedColor = colorHex
                setupColorSwatches()
            }

            if (colorHex == selectedColor) {
                val ring = android.widget.FrameLayout(this)
                ring.layoutParams = params
                ring.setBackgroundResource(R.drawable.bg_color_swatch_selected)
                val inner = View(this)
                val innerParams = android.widget.FrameLayout.LayoutParams(size - 10, size - 10)
                innerParams.gravity = android.view.Gravity.CENTER
                inner.layoutParams = innerParams
                inner.background = shape
                ring.addView(inner)
                colorSwatchContainer.addView(ring)
            } else {
                colorSwatchContainer.addView(swatch)
            }
        }
    }

    private fun setupDayCheckboxes() {
        specificDaysContainer.removeAllViews()
        weekDayNames.forEachIndexed { index, name ->
            val cb = android.widget.CheckBox(this)
            cb.text = name
            cb.textSize = 11f
            cb.isChecked = selectedDays.contains(index)
            cb.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedDays.add(index) else selectedDays.remove(index)
            }
            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            cb.layoutParams = params
            specificDaysContainer.addView(cb)
        }
    }

    private fun checkCameraPermissionAndLaunch() {
        val permission = android.Manifest.permission.CAMERA
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, permission) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            cameraPermissionLauncher.launch(permission)
        }
    }

    private fun launchCamera() {
        val dir = File(externalCacheDir, "temp_photos")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "temp_${System.currentTimeMillis()}.jpg")
        pendingCameraUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        cameraLauncher.launch(pendingCameraUri)
    }

    private fun copyImageToInternalStorage(uri: Uri) {
        try {
            val dir = File(filesDir, "medicine_photos")
            if (!dir.exists()) dir.mkdirs()
            val destFile = File(dir, "med_${System.currentTimeMillis()}.jpg")

            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            currentPhotoPath = destFile.absolutePath
            showPhotoPreview(destFile.absolutePath)
        } catch (e: Exception) {
            Toast.makeText(this, "ছবি সেভ করতে সমস্যা হয়েছে", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPhotoPreview(path: String) {
        photoPreview.visibility = View.VISIBLE
        photoPreview.setImageURI(Uri.fromFile(File(path)))
        findViewById<View>(R.id.removePhotoButton).visibility = View.VISIBLE
    }

    private fun clearPhoto() {
        currentPhotoPath = null
        photoPreview.visibility = View.GONE
        findViewById<View>(R.id.removePhotoButton).visibility = View.GONE
    }

    private fun saveMedicine() {
        val name = nameInput.text.toString().trim()
        val genericName = genericNameInput.text.toString().trim()
        val dose = doseInput.text.toString().trim()
        val quantityStr = quantityInput.text.toString().trim()
        val foodTiming = foodTimingInput.selectedItem?.toString() ?: ""
        val medicineType = medicineTypeInput.selectedItem?.toString() ?: ""
        val repeatIndex = repeatPatternInput.selectedItemPosition
        val repeatPattern = if (repeatIndex in repeatPatternValues.indices) repeatPatternValues[repeatIndex] else "DAILY"
        val repeatDaysCsv = if (repeatPattern == "SPECIFIC") selectedDays.sorted().joinToString(",") else ""
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
                    notes = notes,
                    medicineType = medicineType,
                    colorHex = selectedColor,
                    repeatPattern = repeatPattern,
                    repeatDaysCsv = repeatDaysCsv,
                    photoPath = currentPhotoPath ?: ""
                )
                repository.update(updated)
                timeSlots.forEachIndexed { index, time ->
                    AlarmScheduler.scheduleDose(this@AddMedicineActivity, updated.id, time, index, updated.repeatPattern, updated.repeatDaysCsv)
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
                    notes = notes,
                    medicineType = medicineType,
                    colorHex = selectedColor,
                    repeatPattern = repeatPattern,
                    repeatDaysCsv = repeatDaysCsv,
                    photoPath = currentPhotoPath ?: ""
                )
                val newId = repository.insert(medicine)
                timeSlots.forEachIndexed { index, time ->
                    AlarmScheduler.scheduleDose(this@AddMedicineActivity, newId, time, index, medicine.repeatPattern, medicine.repeatDaysCsv)
                }
            }

            Toast.makeText(this@AddMedicineActivity, "সেভ হয়েছে", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
