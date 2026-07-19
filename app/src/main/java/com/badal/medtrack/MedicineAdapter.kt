package com.badal.medtrack

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MedicineAdapter(
    private var items: List<Medicine>,
    private val onEdit: (Medicine) -> Unit,
    private val onDelete: (Medicine) -> Unit
) : RecyclerView.Adapter<MedicineAdapter.MedicineViewHolder>() {

    class MedicineViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.nameText)
        val doseText: TextView = view.findViewById(R.id.doseText)
        val timesText: TextView = view.findViewById(R.id.timesText)
        val quantityText: TextView = view.findViewById(R.id.quantityText)
        val lowStockBadge: TextView = view.findViewById(R.id.lowStockBadge)
        val editButton: TextView = view.findViewById(R.id.editButton)
        val deleteButton: TextView = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicineViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_medicine, parent, false)
        return MedicineViewHolder(view)
    }

    override fun onBindViewHolder(holder: MedicineViewHolder, position: Int) {
        val medicine = items[position]
        holder.nameText.text = medicine.name
        holder.doseText.text = "ডোজ: ${medicine.dose}"
        holder.timesText.text = "সময়: ${medicine.timesList().joinToString(", ")}"
        holder.quantityText.text = "স্টক: ${medicine.quantity}"

        if (medicine.quantity <= medicine.lowStockThreshold) {
            holder.lowStockBadge.visibility = View.VISIBLE
        } else {
            holder.lowStockBadge.visibility = View.GONE
        }

        holder.editButton.setOnClickListener { onEdit(medicine) }
        holder.deleteButton.setOnClickListener { onDelete(medicine) }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<Medicine>) {
        items = newItems
        notifyDataSetChanged()
    }
}
