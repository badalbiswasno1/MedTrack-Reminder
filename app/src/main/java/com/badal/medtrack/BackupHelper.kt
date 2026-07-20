package com.badal.medtrack

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupHelper {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    fun exportCsv(context: Context, medicines: List<Medicine>, logs: List<DoseLog>): File {
        val backupDir = File(context.externalCacheDir, "backups")
        if (!backupDir.exists()) backupDir.mkdirs()

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(backupDir, "medtrack_backup_$timestamp.csv")

        FileWriter(file).use { writer ->
            writer.append("=== ওষুধের তালিকা ===\n")
            writer.append("নাম,জেনেরিক নাম,ডোজ,খাবারের সময়,ডাক্তারের নাম,স্টক,স্টক সীমা,মেয়াদ,নোট\n")

            for (m in medicines) {
                writer.append(csvEscape(m.name)).append(",")
                writer.append(csvEscape(m.genericName)).append(",")
                writer.append(csvEscape(m.dose)).append(",")
                writer.append(csvEscape(m.foodTiming)).append(",")
                writer.append(csvEscape(m.doctorName)).append(",")
                writer.append(m.quantity.toString()).append(",")
                writer.append(m.lowStockThreshold.toString()).append(",")
                writer.append(csvEscape(m.expiryDate)).append(",")
                writer.append(csvEscape(m.notes)).append("\n")
            }

            writer.append("\n=== খাওয়ার লগ ===\n")
            writer.append("ওষুধ আইডি,নির্ধারিত সময়,অবস্থা,খাওয়া হয়েছে কখন\n")

            for (log in logs) {
                writer.append(log.medicineId.toString()).append(",")
                writer.append(dateFormat.format(Date(log.scheduledDateTime))).append(",")
                writer.append(log.status).append(",")
                writer.append(if (log.takenAt != null) dateFormat.format(Date(log.takenAt!!)) else "").append("\n")
            }
        }

        return file
    }

    fun shareFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/csv"
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(intent, "ব্যাকআপ শেয়ার করুন"))
    }

    private fun csvEscape(value: String): String {
        val cleaned = value.replace("\"", "\"\"")
        return if (cleaned.contains(",") || cleaned.contains("\n")) "\"$cleaned\"" else cleaned
    }
}
