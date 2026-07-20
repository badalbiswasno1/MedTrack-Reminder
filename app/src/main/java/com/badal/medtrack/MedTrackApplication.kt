package com.badal.medtrack

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class MedTrackApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val savedMode = SettingsPrefs.getNightMode(this)
        AppCompatDelegate.setDefaultNightMode(savedMode)
    }
}
