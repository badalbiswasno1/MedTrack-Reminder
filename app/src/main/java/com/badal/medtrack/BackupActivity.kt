package com.badal.medtrack

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.io.File

class BackupActivity : AppCompatActivity() {

    private lateinit var repository: MedicineRepository
    private var lastExportedFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_backup)

        repository = MedicineRepository(this)

        findViewById<TextView>(R.id.exportButton).setOnClickListener {
            exportBackup()
        }

        findViewById<TextView>(R.id.shareButton).setOnClickListener {
            lastExportedFile?.let { file ->
                BackupHelper.shareFile(this, file)
            }
        }
    }

    private fun exportBackup() {
        lifecycleScope.launch {
            val medicines = repository.getAllList()
            val logs = repository.getAllLogsAsc()

            val file = BackupHelper.exportCsv(this@BackupActivity, medicines, logs)
            lastExportedFile = file

            findViewById<TextView>(R.id.statusText).text =
                "ব্যাকআপ তৈরি হয়েছে: ${file.name}\n${medicines.size} টি ওষুধ, ${logs.size} টি লগ"
            findViewById<TextView>(R.id.shareButton).visibility = android.view.View.VISIBLE

            Toast.makeText(this@BackupActivity, "এক্সপোর্ট সম্পন্ন হয়েছে", Toast.LENGTH_SHORT).show()
        }
    }
}
