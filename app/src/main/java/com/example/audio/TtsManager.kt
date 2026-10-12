package com.example.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TtsManager(context: Context, private val onInitialized: (Boolean) -> Unit) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var isInitialized = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("ar", "SA"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w("TtsManager", "Arabic language not supported, defaulting to US English")
                tts?.setLanguage(Locale.US)
            }
            isInitialized = true
            onInitialized(true)
        } else {
            isInitialized = false
            onInitialized(false)
            Log.e("TtsManager", "TTS Initialization failed!")
        }
    }

    fun speak(text: String, pitch: Float, speed: Float, isArabic: Boolean = true) {
        if (!isInitialized) return
        tts?.language = if (isArabic) Locale("ar") else Locale.US
        tts?.setPitch(pitch)
        tts?.setSpeechRate(speed)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts_utterance_id")
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            // ignore
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            // ignore
        }
        tts = null
    }
}
