package com.badal.medtrack

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView

class HelpCenterActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)

        findViewById<TextView>(R.id.contactButton).setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:badalbiswas0045@gmail.com")
                putExtra(Intent.EXTRA_SUBJECT, "MedTrack App - Feedback")
            }
            startActivity(Intent.createChooser(intent, "ইমেইল পাঠান"))
        }

        findViewById<TextView>(R.id.shareButton).setOnClickListener {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "MedTrack - ওষুধ খাওয়ার রিমাইন্ডার অ্যাপ। ডাউনলোড করুন এবং কখনো ওষুধ খেতে ভুলবেন না!")
            }
            startActivity(Intent.createChooser(intent, "শেয়ার করুন"))
        }
    }
}
