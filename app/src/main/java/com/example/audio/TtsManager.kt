package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Cloud TTS client for Soundmaic Voice API.
 * The ElevenLabs key never enters this app; the API key is held by the backend.
 */
class TtsManager(
    private val context: Context,
    private val onInitialized: (Boolean) -> Unit
) : TextToSpeech.OnInitListener {
    private var fallbackTts: TextToSpeech? = TextToSpeech(context, this)
    private var fallbackInitialized = false
    private var player: MediaPlayer? = null
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    override fun onInit(status: Int) {
        fallbackInitialized = status == TextToSpeech.SUCCESS
        if (fallbackInitialized) fallbackTts?.setLanguage(Locale.US)
        onInitialized(fallbackInitialized)
    }

    fun speak(
        text: String,
        pitch: Float,
        speed: Float,
        isArabic: Boolean = true,
        profileId: String = "young_woman"
    ) {
        if (text.isBlank()) return
        executor.execute {
            try {
                val url = "$API_BASE_URL/api/tts?profile=${Uri.encode(profileId)}"
                val payload = JSONObject()
                    .put("text", text)
                    .put("speed", speed.coerceIn(0.8f, 1.2f))
                    .put("language", if (isArabic) "ar" else "en")
                    .toString()
                val request = Request.Builder()
                    .url(url)
                    .header("X-Soundmaic-Client", "android")
                    .post(payload.toRequestBody(JSON_MEDIA_TYPE))
                    .build()
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IllegalStateException("Voice API HTTP ${response.code}")
                    }
                    val body = response.body ?: throw IllegalStateException("Voice API returned no audio")
                    val output = File(context.cacheDir, "cloud_tts_${System.currentTimeMillis()}.mp3")
                    body.byteStream().use { input -> output.outputStream().use { input.copyTo(it) } }
                    playGeneratedAudio(output)
                }
            } catch (error: Throwable) {
                Log.e(TAG, "Cloud TTS failed; using installed system TTS", error)
                speakWithSystemFallback(text, speed, isArabic)
            }
        }
    }

    private fun playGeneratedAudio(file: File) {
        player?.release()
        player = MediaPlayer().apply {
            setDataSource(context, Uri.fromFile(file))
            setOnCompletionListener { mp ->
                mp.release()
                if (file.exists()) file.delete()
            }
            setOnErrorListener { mp, _, _ ->
                mp.release()
                if (file.exists()) file.delete()
                true
            }
            prepare()
            start()
        }
    }

    private fun speakWithSystemFallback(text: String, speed: Float, isArabic: Boolean) {
        if (!fallbackInitialized) return
        fallbackTts?.language = if (isArabic) Locale("ar", "SA") else Locale.US
        fallbackTts?.setPitch(1.0f)
        fallbackTts?.setSpeechRate(speed.coerceIn(0.90f, 1.10f))
        fallbackTts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts_fallback")
    }

    fun stop() {
        try {
            player?.stop()
            player?.release()
            player = null
            fallbackTts?.stop()
        } catch (_: Exception) {
            // Ignore stop races while generated audio is prepared.
        }
    }

    fun shutdown() {
        stop()
        try {
            fallbackTts?.shutdown()
            httpClient.dispatcher.executorService.shutdown()
            executor.shutdownNow()
        } catch (_: Exception) {
            // Ignore shutdown races.
        }
        fallbackTts = null
    }

    companion object {
        private const val API_BASE_URL = "https://soundvoice-8dtidqem.manus.space"
        private const val TAG = "CloudTtsManager"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
