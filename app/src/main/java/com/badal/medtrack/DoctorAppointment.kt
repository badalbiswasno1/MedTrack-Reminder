package com.badal.medtrack

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "doctor_appointment")
data class DoctorAppointment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val doctorName: String,
    val dateTime: Long,
    val location: String = "",
    val notes: String = ""
)
