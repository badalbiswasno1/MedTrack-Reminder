package com.badal.medtrack

import android.content.Context
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onResume() {
        super.onResume()
        val rootView = findViewById<android.view.View>(android.R.id.content)
        rootView?.let {
            TranslationHelper.applyToActivity(this, it)
        }
    }
}
