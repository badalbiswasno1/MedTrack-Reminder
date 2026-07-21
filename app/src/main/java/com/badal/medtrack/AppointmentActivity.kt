package com.badal.medtrack

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AppointmentActivity : BaseActivity() {

    private lateinit var repository: MedicineRepository
    private lateinit var adapter: AppointmentAdapter
    private var selectedDateTime: Calendar? = null
    private val displayFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_appointment)

        repository = MedicineRepository(this)

        adapter = AppointmentAdapter(emptyList()) { apt ->
            lifecycleScope.launch {
                cancelAppointmentAlarm(apt.id)
                repository.deleteAppointment(apt.id)
                loadAppointments()
                Toast.makeText(this@AppointmentActivity, "মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
            }
        }

        val recyclerView = findViewById<RecyclerView>(R.id.aptRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        findViewById<TextView>(R.id.aptDateTimeButton).setOnClickListener { pickDateTime() }
        findViewById<TextView>(R.id.aptSaveButton).setOnClickListener { saveAppointment() }

        loadAppointments()
    }

    override fun onResume() {
        super.onResume()
        loadAppointments()
    }

    private fun pickDateTime() {
        val now = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            TimePickerDialog(this, { _, hour, minute ->
                val cal = Calendar.getInstance()
                cal.set(year, month, day, hour, minute, 0)
                selectedDateTime = cal
                findViewById<TextView>(R.id.aptDateTimeButton).text = displayFormat.format(cal.time)
            }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), false).show()
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun saveAppointment() {
        val doctorName = findViewById<EditText>(R.id.aptDoctorInput).text.toString().trim()
        val location = findViewById<EditText>(R.id.aptLocationInput).text.toString().trim()
        val dateTime = selectedDateTime

        if (doctorName.isEmpty()) {
            Toast.makeText(this, "ডাক্তারের নাম দাও", Toast.LENGTH_SHORT).show()
            return
        }
        if (dateTime == null) {
            Toast.makeText(this, "তারিখ ও সময় বেছে নাও", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val appointment = DoctorAppointment(
                doctorName = doctorName,
                dateTime = dateTime.timeInMillis,
                location = location
            )
            val id = repository.insertAppointment(appointment)
            scheduleAppointmentAlarm(id, doctorName, location, dateTime.timeInMillis)

            findViewById<EditText>(R.id.aptDoctorInput).text.clear()
            findViewById<EditText>(R.id.aptLocationInput).text.clear()
            findViewById<TextView>(R.id.aptDateTimeButton).text = "তারিখ ও সময় বেছে নিন"
            selectedDateTime = null

            loadAppointments()
            Toast.makeText(this@AppointmentActivity, "যোগ হয়েছে", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadAppointments() {
        lifecycleScope.launch {
            val appointments = repository.getAllAppointments()
            adapter.updateItems(appointments)

            val emptyText = findViewById<TextView>(R.id.aptEmptyText)
            val recyclerView = findViewById<RecyclerView>(R.id.aptRecyclerView)
            if (appointments.isEmpty()) {
                emptyText.visibility = android.view.View.VISIBLE
                recyclerView.visibility = android.view.View.GONE
            } else {
                emptyText.visibility = android.view.View.GONE
                recyclerView.visibility = android.view.View.VISIBLE
            }
        }
    }

    private fun scheduleAppointmentAlarm(id: Long, doctorName: String, location: String, triggerAt: Long) {
        val intent = Intent(this, AppointmentAlarmReceiver::class.java).apply {
            putExtra("appointmentId", id)
            putExtra("doctorName", doctorName)
            putExtra("location", location)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this, (100000 + id).toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    private fun cancelAppointmentAlarm(id: Long) {
        val intent = Intent(this, AppointmentAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, (100000 + id).toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }
}
