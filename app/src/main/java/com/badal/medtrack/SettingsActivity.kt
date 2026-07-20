package com.badal.medtrack

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate

class SettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        findViewById<LinearLayout>(R.id.darkModeRow).setOnClickListener {
            val newMode = SettingsPrefs.cycleNightMode(this)
            recreate()
        }

        findViewById<LinearLayout>(R.id.voiceReminderRow).setOnClickListener {
            val current = SettingsPrefs.isVoiceReminderEnabled(this)
            SettingsPrefs.setVoiceReminderEnabled(this, !current)
            refreshValues()
        }

        findViewById<LinearLayout>(R.id.languageRow).setOnClickListener {
            LocaleHelper.toggleLanguage(this)
            recreate()
        }

        findViewById<LinearLayout>(R.id.backupRow).setOnClickListener {
            startActivity(Intent(this, BackupActivity::class.java))
        }

        refreshValues()
    }

    override fun onResume() {
        super.onResume()
        refreshValues()
    }

    private fun refreshValues() {
        val isEnglish = LocaleHelper.getLanguage(this) == "en"
        val nightMode = SettingsPrefs.getNightMode(this)

        val themeLabel = when (nightMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> if (isEnglish) "Light" else "লাইট"
            AppCompatDelegate.MODE_NIGHT_YES -> if (isEnglish) "Dark" else "ডার্ক"
            else -> if (isEnglish) "System Default" else "সিস্টেম অনুযায়ী"
        }
        findViewById<TextView>(R.id.darkModeValueText).text = themeLabel

        val voiceEnabled = SettingsPrefs.isVoiceReminderEnabled(this)
        findViewById<TextView>(R.id.voiceReminderValueText).text = when {
            voiceEnabled && isEnglish -> "On"
            voiceEnabled -> "চালু"
            isEnglish -> "Off"
            else -> "বন্ধ"
        }

        findViewById<TextView>(R.id.languageValueText).text = if (isEnglish) "English" else "বাংলা"
    }
}
