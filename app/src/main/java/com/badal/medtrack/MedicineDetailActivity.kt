package com.badal.medtrack

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MedicineDetailActivity : BaseActivity() {

    private lateinit var repository: MedicineRepository
    private var medicineId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medicine_detail)

        repository = MedicineRepository(this)
        medicineId = intent.getLongExtra("medicineId", -1)

        findViewById<View>(R.id.detailEditButton).setOnClickListener {
            val intent = Intent(this, AddMedicineActivity::class.java)
            intent.putExtra("medicineId", medicineId)
            startActivity(intent)
        }

        loadDetails()
    }

    override fun onResume() {
        super.onResume()
        loadDetails()
    }

    private fun loadDetails() {
        if (medicineId == -1L) return
        lifecycleScope.launch {
            val medicine = repository.getById(medicineId) ?: return@launch

            findViewById<TextView>(R.id.detailNameText).text = medicine.name
            findViewById<TextView>(R.id.detailGenericText).text =
                medicine.genericName.ifBlank { "জেনেরিক নাম দেওয়া নেই" }
            findViewById<TextView>(R.id.detailDoseText).text = medicine.dose
            findViewById<TextView>(R.id.detailTimesText).text = medicine.timesList().joinToString(", ")
            findViewById<TextView>(R.id.detailFoodTimingText).text =
                medicine.foodTiming.ifBlank { "উল্লেখ নেই" }
            findViewById<TextView>(R.id.detailDoctorText).text =
                medicine.doctorName.ifBlank { "উল্লেখ নেই" }
            findViewById<TextView>(R.id.detailExpiryText).text =
                medicine.expiryDate.ifBlank { "উল্লেখ নেই" }
            findViewById<TextView>(R.id.detailStockText).text = "${medicine.quantity} টি বাকি আছে"
            findViewById<TextView>(R.id.detailNotesText).text =
                medicine.notes.ifBlank { "কোনো নোট নেই" }
        }
    }
}
