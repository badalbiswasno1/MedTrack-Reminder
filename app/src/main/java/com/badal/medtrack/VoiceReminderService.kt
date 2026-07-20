package com.badal.medtrack

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class VoiceReminderService : Service(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var pendingMessage: String? = null

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val name = intent?.getStringExtra("medicineName") ?: ""
        val dose = intent?.getStringExtra("medicineDose") ?: ""

        pendingMessage = if (dose.isNotBlank()) {
            "ওষুধ খাওয়ার সময় হয়েছে। $name, $dose"
        } else {
            "ওষুধ খাওয়ার সময় হয়েছে। $name"
        }

        return START_NOT_STICKY
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("bn", "IN"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.US)
            }

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    stopSelf()
                }
                override fun onError(utteranceId: String?) {
                    stopSelf()
                }
            })

            pendingMessage?.let { message ->
                tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "medtrack_voice_reminder")
            }
        } else {
            stopSelf()
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
