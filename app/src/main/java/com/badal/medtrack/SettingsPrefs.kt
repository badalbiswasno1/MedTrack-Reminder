package com.badal.medtrack

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object SettingsPrefs {
    private const val PREFS_NAME = "medtrack_settings"
    private const val KEY_NIGHT_MODE = "night_mode"

    fun getNightMode(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }

    fun setNightMode(context: Context, mode: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_NIGHT_MODE, mode).apply()
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun cycleNightMode(context: Context): Int {
        val current = getNightMode(context)
        val next = when (current) {
            AppCompatDelegate.MODE_NIGHT_NO -> AppCompatDelegate.MODE_NIGHT_YES
            AppCompatDelegate.MODE_NIGHT_YES -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            else -> AppCompatDelegate.MODE_NIGHT_NO
        }
        setNightMode(context, next)
        return next
    }

    fun modeLabel(mode: Int): String {
        return when (mode) {
            AppCompatDelegate.MODE_NIGHT_NO -> "লাইট মোড"
            AppCompatDelegate.MODE_NIGHT_YES -> "ডার্ক মোড"
            else -> "সিস্টেম অনুযায়ী"
        }
    }
}
