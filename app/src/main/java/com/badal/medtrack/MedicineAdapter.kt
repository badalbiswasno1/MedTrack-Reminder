package com.badal.medtrack

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MedicineAdapter(
    private var items: List<Medicine>,
    private val onEdit: (Medicine) -> Unit,
    private val onDelete: (Medicine) -> Unit,
    private val onTake: (Medicine) -> Unit = {}
) : RecyclerView.Adapter<MedicineAdapter.MedicineViewHolder>() {

    class MedicineViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.nameText)
        val doseText: TextView = view.findViewById(R.id.doseText)
        val timesText: TextView = view.findViewById(R.id.timesText)
        val quantityText: TextView = view.findViewById(R.id.quantityText)
        val lowStockBadge: TextView = view.findViewById(R.id.lowStockBadge)
        val editButton: TextView = view.findViewById(R.id.editButton)
        val deleteButton: TextView = view.findViewById(R.id.deleteButton)
        val takeButton: TextView = view.findViewById(R.id.takeButton)
        val statusDot: View = view.findViewById(R.id.statusDot)
        val progressBar: ProgressBar = view.findViewById(R.id.stockProgressBar)
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
        holder.quantityText.text = "স্টক: ${medicine.quantity} টি"

        val startingStock = maxOf(medicine.quantity, medicine.lowStockThreshold * 3, 1)
        val progressPercent = ((medicine.quantity.toFloat() / startingStock) * 100).toInt().coerceIn(0, 100)
        holder.progressBar.progress = progressPercent

        val isCritical = medicine.quantity <= (medicine.lowStockThreshold / 2)
        val isLow = medicine.quantity <= medicine.lowStockThreshold

        when {
            isCritical -> {
                holder.statusDot.setBackgroundResource(R.drawable.dot_danger)
                holder.progressBar.progressTintList = holder.itemView.context.getColorStateList(R.color.danger)
                holder.lowStockBadge.visibility = View.VISIBLE
                holder.lowStockBadge.text = "স্টক শেষের পথে"
            }
            isLow -> {
                holder.statusDot.setBackgroundResource(R.drawable.dot_warning)
                holder.progressBar.progressTintList = holder.itemView.context.getColorStateList(R.color.warning)
                holder.lowStockBadge.visibility = View.VISIBLE
                holder.lowStockBadge.text = "কম আছে"
            }
            else -> {
                holder.statusDot.setBackgroundResource(R.drawable.dot_success)
                holder.progressBar.progressTintList = holder.itemView.context.getColorStateList(R.color.success)
                holder.lowStockBadge.visibility = View.GONE
            }
        }

        holder.editButton.setOnClickListener { onEdit(medicine) }
        holder.deleteButton.setOnClickListener { onDelete(medicine) }
        holder.takeButton.setOnClickListener { onTake(medicine) }
    }

    override fun getItemCount(): Int = items.size

    fun getItemAt(position: Int): Medicine = items[position]

    fun updateItems(newItems: List<Medicine>) {
        items = newItems
        notifyDataSetChanged()
    }
}
