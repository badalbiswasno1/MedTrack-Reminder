package com.badal.medtrack

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medicine")
data class Medicine(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val dose: String,
    val timesCsv: String,
    val quantity: Int,
    val lowStockThreshold: Int = 10,
    val genericName: String = "",
    val doctorName: String = "",
    val foodTiming: String = "",
    val notes: String = "",
    val expiryDate: String = ""
)

fun Medicine.timesList(): List<String> =
    if (timesCsv.isBlank()) emptyList() else timesCsv.split(",")

fun List<String>.toCsv(): String = this.joinToString(",")
