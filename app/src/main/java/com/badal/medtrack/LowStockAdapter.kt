package com.badal.medtrack

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LowStockAdapter(
    private var items: List<Medicine>,
    private val onClick: (Medicine) -> Unit
) : RecyclerView.Adapter<LowStockAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val statusDot: View = view.findViewById(R.id.statusDot)
        val nameText: TextView = view.findViewById(R.id.nameText)
        val quantityText: TextView = view.findViewById(R.id.quantityText)
        val badgeText: TextView = view.findViewById(R.id.badgeText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_low_stock, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val medicine = items[position]
        holder.nameText.text = medicine.name
        holder.quantityText.text = "বর্তমান স্টক: ${medicine.quantity} (সীমা: ${medicine.lowStockThreshold})"

        val isCritical = medicine.quantity <= (medicine.lowStockThreshold / 2)

        if (isCritical) {
            holder.statusDot.setBackgroundResource(R.drawable.dot_danger)
            holder.badgeText.text = "স্টক শেষের পথে"
            holder.badgeText.setBackgroundResource(R.drawable.bg_day_missed)
        } else {
            holder.statusDot.setBackgroundResource(R.drawable.dot_warning)
            holder.badgeText.text = "কম আছে"
            holder.badgeText.setBackgroundResource(R.drawable.bg_day_partial)
        }

        holder.itemView.setOnClickListener { onClick(medicine) }
    }

    override fun getItemCount() = items.size

    fun updateItems(newItems: List<Medicine>) {
        items = newItems
        notifyDataSetChanged()
    }
}
