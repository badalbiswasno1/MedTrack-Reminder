package com.badal.medtrack

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions

object TranslationHelper {

    private const val PREFS_NAME = "medtrack_translation_cache"
    private var translator: Translator? = null
    private var modelReady = false

    private const val TAG_ORIGINAL_TEXT = 0x7f100001

    fun ensureModelDownloaded(context: Context, onReady: (() -> Unit)? = null) {
        if (translator == null) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.BENGALI)
                .setTargetLanguage(TranslateLanguage.ENGLISH)
                .build()
            translator = Translation.getClient(options)
        }

        if (modelReady) {
            onReady?.invoke()
            return
        }

        val conditions = DownloadConditions.Builder().build()
        translator?.downloadModelIfNeeded(conditions)
            ?.addOnSuccessListener {
                modelReady = true
                onReady?.invoke()
            }
            ?.addOnFailureListener {
                // Model download failed (likely no network) - stays in Bengali, no crash
            }
    }

    fun applyToActivity(context: Context, root: View) {
        val language = LocaleHelper.getLanguage(context)

        if (language == "bn") {
            restoreOriginal(root)
            return
        }

        ensureModelDownloaded(context) {
            translateTree(context, root)
        }
    }

    private fun restoreOriginal(view: View) {
        if (view is TextView && view !is EditText) {
            val original = view.getTag(TAG_ORIGINAL_TEXT) as? String
            if (original != null) view.text = original
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                restoreOriginal(view.getChildAt(i))
            }
        }
    }

    private fun translateTree(context: Context, view: View) {
        if (view is TextView && view !is EditText) {
            val current = view.text?.toString() ?: ""
            if (current.isNotBlank() && containsBengali(current)) {
                if (view.getTag(TAG_ORIGINAL_TEXT) == null) {
                    view.setTag(TAG_ORIGINAL_TEXT, current)
                }
                val original = view.getTag(TAG_ORIGINAL_TEXT) as String

                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val cached = prefs.getString(original, null)

                if (cached != null) {
                    view.text = cached
                } else {
                    translator?.translate(original)
                        ?.addOnSuccessListener { translated ->
                            view.text = translated
                            prefs.edit().putString(original, translated).apply()
                        }
                }
            }
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                translateTree(context, view.getChildAt(i))
            }
        }
    }

    private fun containsBengali(text: String): Boolean {
        return text.any { it.code in 0x0980..0x09FF }
    }
}
