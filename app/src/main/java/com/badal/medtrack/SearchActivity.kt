package com.badal.medtrack

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class SearchActivity : BaseActivity() {

    private lateinit var repository: MedicineRepository
    private lateinit var adapter: LowStockAdapter
    private var allMedicines: List<Medicine> = emptyList()
    private var showLowStockOnly = false
    private var currentQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        repository = MedicineRepository(this)

        adapter = LowStockAdapter(emptyList()) { medicine ->
            val intent = Intent(this, MedicineDetailActivity::class.java)
            intent.putExtra("medicineId", medicine.id)
            startActivity(intent)
        }

        val recyclerView = findViewById<RecyclerView>(R.id.searchRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        findViewById<EditText>(R.id.searchInput).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentQuery = s?.toString() ?: ""
                applyFilter()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        findViewById<TextView>(R.id.filterAll).setOnClickListener {
            showLowStockOnly = false
            updateFilterUI()
            applyFilter()
        }

        findViewById<TextView>(R.id.filterLowStock).setOnClickListener {
            showLowStockOnly = true
            updateFilterUI()
            applyFilter()
        }

        loadMedicines()
    }

    override fun onResume() {
        super.onResume()
        loadMedicines()
    }

    private fun loadMedicines() {
        lifecycleScope.launch {
            allMedicines = repository.getAllList()
            applyFilter()
        }
    }

    private fun applyFilter() {
        var results = allMedicines

        if (currentQuery.isNotBlank()) {
            results = results.filter { it.name.contains(currentQuery, ignoreCase = true) }
        }

        if (showLowStockOnly) {
            results = results.filter { it.quantity <= it.lowStockThreshold }
        }

        results = results.sortedBy { it.name }

        adapter.updateItems(results)

        val emptyText = findViewById<TextView>(R.id.searchEmptyText)
        val recyclerView = findViewById<RecyclerView>(R.id.searchRecyclerView)
        if (results.isEmpty()) {
            emptyText.visibility = android.view.View.VISIBLE
            recyclerView.visibility = android.view.View.GONE
        } else {
            emptyText.visibility = android.view.View.GONE
            recyclerView.visibility = android.view.View.VISIBLE
        }
    }

    private fun updateFilterUI() {
        val allTab = findViewById<TextView>(R.id.filterAll)
        val lowTab = findViewById<TextView>(R.id.filterLowStock)

        if (showLowStockOnly) {
            allTab.setBackgroundResource(R.drawable.bg_stat_card)
            allTab.setTextColor(getColor(R.color.primary))
            lowTab.setBackgroundResource(R.drawable.bg_day_upcoming)
            lowTab.setTextColor(getColor(R.color.white))
        } else {
            allTab.setBackgroundResource(R.drawable.bg_day_upcoming)
            allTab.setTextColor(getColor(R.color.white))
            lowTab.setBackgroundResource(R.drawable.bg_stat_card)
            lowTab.setTextColor(getColor(R.color.primary))
        }
    }
}
