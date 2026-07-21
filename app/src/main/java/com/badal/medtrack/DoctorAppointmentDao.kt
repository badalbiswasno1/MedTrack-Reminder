package com.badal.medtrack

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface DoctorAppointmentDao {
    @Insert
    suspend fun insert(appointment: DoctorAppointment): Long

    @Query("SELECT * FROM doctor_appointment ORDER BY dateTime ASC")
    suspend fun getAll(): List<DoctorAppointment>

    @Query("SELECT * FROM doctor_appointment WHERE dateTime >= :now ORDER BY dateTime ASC")
    suspend fun getUpcoming(now: Long): List<DoctorAppointment>

    @Query("DELETE FROM doctor_appointment WHERE id = :id")
    suspend fun deleteById(id: Long)
}
