package com.badal.medtrack

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dose_log")
data class DoseLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val medicineId: Long,
    val slotIndex: Int,
    val scheduledDateTime: Long,
    val status: String,
    val takenAt: Long? = null
)

object DoseStatus {
    const val PENDING = "PENDING"
    const val TAKEN = "TAKEN"
    const val MISSED = "MISSED"
    const val SKIPPED = "SKIPPED"
}
