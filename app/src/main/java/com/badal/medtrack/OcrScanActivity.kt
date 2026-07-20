package com.badal.medtrack

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File

class OcrScanActivity : BaseActivity() {

    private var cameraImageUri: Uri? = null

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraImageUri != null) {
            processImage(cameraImageUri!!)
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            processImage(uri)
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera() else Toast.makeText(this, "ক্যামেরা অনুমতি প্রয়োজন", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ocr_scan)

        findViewById<TextView>(R.id.cameraButton).setOnClickListener {
            checkCameraPermissionAndLaunch()
        }

        findViewById<TextView>(R.id.galleryButton).setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
    }

    private fun checkCameraPermissionAndLaunch() {
        val permission = android.Manifest.permission.CAMERA
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, permission) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            cameraPermissionLauncher.launch(permission)
        }
    }

    private fun launchCamera() {
        val imagesDir = File(externalCacheDir, "ocr_images")
        if (!imagesDir.exists()) imagesDir.mkdirs()
        val imageFile = File(imagesDir, "scan_${System.currentTimeMillis()}.jpg")
        cameraImageUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", imageFile)
        takePictureLauncher.launch(cameraImageUri)
    }

    private fun processImage(uri: Uri) {
        findViewById<TextView>(R.id.statusText).text = "ছবি বিশ্লেষণ করা হচ্ছে..."
        findViewById<LinearLayout>(R.id.detectedLinesContainer).removeAllViews()

        val previewImage = findViewById<ImageView>(R.id.previewImage)
        previewImage.visibility = android.view.View.VISIBLE
        previewImage.setImageURI(uri)

        try {
            val image = InputImage.fromFilePath(this, uri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    displayDetectedLines(visionText.text)
                }
                .addOnFailureListener { e ->
                    findViewById<TextView>(R.id.statusText).text = "ব্যর্থ হয়েছে: ${e.message}"
                }
        } catch (e: Exception) {
            findViewById<TextView>(R.id.statusText).text = "ব্যর্থ হয়েছে: ${e.message}"
        }
    }

    private fun displayDetectedLines(fullText: String) {
        val lines = fullText.split("\n").map { it.trim() }.filter { it.isNotEmpty() && it.length > 1 }

        val container = findViewById<LinearLayout>(R.id.detectedLinesContainer)
        container.removeAllViews()

        if (lines.isEmpty()) {
            findViewById<TextView>(R.id.statusText).text = "কোনো লেখা সনাক্ত হয়নি, আবার চেষ্টা করুন"
            return
        }

        findViewById<TextView>(R.id.statusText).text = "${lines.size} টি লাইন পাওয়া গেছে, একটি বেছে নিন"

        for (line in lines) {
            val tv = TextView(this)
            tv.text = line
            tv.textSize = 14f
            tv.setPadding(dp(14), dp(12), dp(14), dp(12))
            tv.setBackgroundResource(R.drawable.bg_ocr_line)
            tv.setTextColor(getColor(R.color.on_surface))

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.bottomMargin = dp(8)
            tv.layoutParams = params

            tv.setOnClickListener {
                val intent = Intent(this, AddMedicineActivity::class.java)
                intent.putExtra("prefillName", line)
                startActivity(intent)
                finish()
            }

            container.addView(tv)
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
