package com.badal.medtrack

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {
    private const val PREFS_NAME = "medtrack_settings"
    private const val KEY_LANGUAGE = "app_language"

    fun getLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, "bn") ?: "bn"
    }

    fun setLanguage(context: Context, languageCode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()
    }

    fun toggleLanguage(context: Context): String {
        val next = if (getLanguage(context) == "bn") "en" else "bn"
        setLanguage(context, next)
        return next
    }

    fun applyLocale(context: Context): Context {
        val languageCode = getLanguage(context)
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
