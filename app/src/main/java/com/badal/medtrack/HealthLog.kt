package com.badal.medtrack

import androidx.room.Entity
import androidx.room.PrimaryKey

object HealthType {
    const val BP = "BP"
    const val SUGAR = "SUGAR"
    const val WEIGHT = "WEIGHT"
    const val HEART_RATE = "HEART_RATE"
    const val SMOKING = "SMOKING"
    const val WATER = "WATER"
    const val LIPID = "LIPID"
}

@Entity(tableName = "health_log")
data class HealthLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val value1: Float,
    val value2: Float = 0f,
    val recordedAt: Long,
    val notes: String = ""
)
