package com.badal.medtrack

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class LowStockActivity : AppCompatActivity() {

    private lateinit var repository: MedicineRepository
    private lateinit var adapter: LowStockAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_low_stock)

        repository = MedicineRepository(this)

        adapter = LowStockAdapter(emptyList()) { medicine ->
            val intent = Intent(this, MedicineDetailActivity::class.java)
            intent.putExtra("medicineId", medicine.id)
            startActivity(intent)
        }

        val recyclerView = findViewById<RecyclerView>(R.id.lowStockRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        loadLowStock()
    }

    override fun onResume() {
        super.onResume()
        loadLowStock()
    }

    private fun loadLowStock() {
        lifecycleScope.launch {
            val allMedicines = repository.getAllList()
            val lowStockMedicines = allMedicines
                .filter { it.quantity <= it.lowStockThreshold }
                .sortedBy { it.quantity }

            adapter.updateItems(lowStockMedicines)
            findViewById<TextView>(R.id.countText).text = "${lowStockMedicines.size} টি ওষুধ"

            val emptyText = findViewById<TextView>(R.id.emptyText)
            val recyclerView = findViewById<RecyclerView>(R.id.lowStockRecyclerView)
            if (lowStockMedicines.isEmpty()) {
                emptyText.visibility = android.view.View.VISIBLE
                recyclerView.visibility = android.view.View.GONE
            } else {
                emptyText.visibility = android.view.View.GONE
                recyclerView.visibility = android.view.View.VISIBLE
            }
        }
    }
}
