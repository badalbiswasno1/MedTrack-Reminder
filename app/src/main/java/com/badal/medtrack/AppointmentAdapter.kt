package com.badal.medtrack

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppointmentAdapter(
    private var items: List<DoctorAppointment>,
    private val onDelete: (DoctorAppointment) -> Unit
) : RecyclerView.Adapter<AppointmentAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val doctorText: TextView = view.findViewById(R.id.aptDoctorText)
        val dateTimeText: TextView = view.findViewById(R.id.aptDateTimeText)
        val locationText: TextView = view.findViewById(R.id.aptLocationText)
        val deleteButton: TextView = view.findViewById(R.id.aptDeleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_appointment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val apt = items[position]
        holder.doctorText.text = apt.doctorName
        holder.dateTimeText.text = dateFormat.format(Date(apt.dateTime))
        holder.locationText.text = apt.location.ifBlank { "" }
        holder.locationText.visibility = if (apt.location.isBlank()) View.GONE else View.VISIBLE
        holder.deleteButton.setOnClickListener { onDelete(apt) }
    }

    override fun getItemCount() = items.size

    fun updateItems(newItems: List<DoctorAppointment>) {
        items = newItems
        notifyDataSetChanged()
    }
}
