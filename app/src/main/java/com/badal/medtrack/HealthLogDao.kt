package com.badal.medtrack

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface HealthLogDao {
    @Insert
    suspend fun insert(log: HealthLog): Long

    @Query("SELECT * FROM health_log WHERE type = :type ORDER BY recordedAt DESC")
    suspend fun getByType(type: String): List<HealthLog>

    @Query("SELECT * FROM health_log WHERE type = :type ORDER BY recordedAt DESC LIMIT 1")
    suspend fun getLatestByType(type: String): HealthLog?

    @Query("SELECT * FROM health_log ORDER BY recordedAt DESC")
    suspend fun getAll(): List<HealthLog>

    @Query("DELETE FROM health_log WHERE id = :id")
    suspend fun deleteById(id: Long)
}
