package com.badal.medtrack

import androidx.room.*

@Dao
interface DoseLogDao {

    @Insert
    suspend fun insert(log: DoseLog): Long

    @Query("UPDATE dose_log SET status = :status, takenAt = :takenAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, takenAt: Long?)

    @Query("SELECT * FROM dose_log WHERE medicineId = :medicineId AND slotIndex = :slotIndex AND status = 'PENDING' AND scheduledDateTime >= :dayStart ORDER BY scheduledDateTime DESC LIMIT 1")
    suspend fun getPendingForSlot(medicineId: Long, slotIndex: Int, dayStart: Long): DoseLog?

    @Query("SELECT * FROM dose_log WHERE scheduledDateTime >= :dayStart AND scheduledDateTime < :dayEnd")
    suspend fun getLogsForDay(dayStart: Long, dayEnd: Long): List<DoseLog>

    @Query("SELECT * FROM dose_log WHERE status = 'PENDING' AND scheduledDateTime < :cutoff")
    suspend fun getStalePending(cutoff: Long): List<DoseLog>

    @Query("SELECT * FROM dose_log WHERE scheduledDateTime >= :start AND scheduledDateTime < :end ORDER BY scheduledDateTime ASC")
    suspend fun getLogsInRange(start: Long, end: Long): List<DoseLog>
}
