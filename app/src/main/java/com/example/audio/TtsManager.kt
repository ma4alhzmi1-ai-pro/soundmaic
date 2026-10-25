package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Cloud TTS client. The ElevenLabs API key remains on the backend. */
class TtsManager(
    private val context: Context,
    private val onAudioGenerated: (String, String) -> Unit = { _, _ -> },
    private val onError: (String) -> Unit = {}
) {
    private var player: MediaPlayer? = null
    private val audioStorage = AudioStorageManager(context)
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    fun speak(
        text: String,
        speed: Float,
        isArabic: Boolean = true,
        profileId: String = "young_woman"
    ) {
        if (text.isBlank()) return
        executor.execute {
            var output: File? = null
            var keepOutputOnFailure = false
            try {
                val url = "$API_BASE_URL/api/tts?profile=${Uri.encode(profileId)}"
                val payload = JSONObject()
                    .put("text", text)
                    .put("speed", speed.coerceIn(0.8f, 1.2f))
                    // The backend uses Eleven Multilingual v2, which detects the spoken language.
                    // Keep the field informational for backward-compatible service versions.
                    .put("language", if (isArabic) "ar" else "en")
                    .toString()
                val request = Request.Builder()
                    .url(url)
                    .header("X-Soundmaic-Client", CLIENT_HEADER)
                    .post(payload.toRequestBody(JSON_MEDIA_TYPE))
                    .build()
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IllegalStateException("تعذر الوصول إلى خدمة الصوت (HTTP ${response.code})")
                    }
                    val body = response.body ?: throw IllegalStateException("لم تُرجع خدمة ElevenLabs ملفًا صوتيًا")
                    if (!body.contentType().toString().startsWith("audio/")) {
                        throw IllegalStateException("أعادت خدمة ElevenLabs استجابة غير صوتية")
                    }
                    if (body.contentLength() > MAX_AUDIO_RESPONSE_BYTES) {
                        throw IllegalStateException("حجم الصوت الناتج أكبر من الحد المسموح")
                    }
                    output = File(context.cacheDir, "cloud_tts_${System.currentTimeMillis()}.mp3")
                    body.byteStream().use { input ->
                        output!!.outputStream().use { target ->
                            val buffer = ByteArray(8 * 1024)
                            var totalBytes = 0L
                            while (true) {
                                val count = input.read(buffer)
                                if (count < 0) break
                                totalBytes += count
                                if (totalBytes > MAX_AUDIO_RESPONSE_BYTES) {
                                    throw IllegalStateException("حجم الصوت الناتج أكبر من الحد المسموح")
                                }
                                target.write(buffer, 0, count)
                            }
                        }
                    }
                    if (output!!.length() == 0L) throw IllegalStateException("ملف الصوت الناتج فارغ")

                    val title = "REMOVOICE_TTS_${System.currentTimeMillis().toString().takeLast(6)}"
                    val savedReference = audioStorage.saveFile(output!!, title)
                    val storedReference = savedReference ?: output!!.absolutePath
                    val cacheFileIsOnlyCopy = savedReference == null
                    keepOutputOnFailure = cacheFileIsOnlyCopy
                    onAudioGenerated(storedReference, title)
                    playGeneratedAudio(output!!, deleteAfterPlayback = !cacheFileIsOnlyCopy)
                }
            } catch (error: Exception) {
                Log.e(TAG, "ElevenLabs text-to-speech failed", error)
                if (!keepOutputOnFailure) output?.delete()
                onError(error.message ?: "تعذر الاتصال بخدمة ElevenLabs؛ لم يتم استبدالها بصوت الجهاز")
            }
        }
    }

    private fun playGeneratedAudio(file: File, deleteAfterPlayback: Boolean) {
        player?.release()
        player = MediaPlayer().apply {
            setDataSource(context, Uri.fromFile(file))
            setOnCompletionListener { mp ->
                mp.release()
                if (deleteAfterPlayback && file.exists()) file.delete()
            }
            setOnErrorListener { mp, _, _ ->
                mp.release()
                if (deleteAfterPlayback && file.exists()) file.delete()
                true
            }
            prepare()
            start()
        }
    }

    fun stop() {
        try {
            player?.stop()
            player?.release()
            player = null
        } catch (_: Exception) {
            // Ignore stop races while generated audio is prepared.
        }
    }

    fun shutdown() {
        stop()
        try {
            httpClient.dispatcher.executorService.shutdown()
            executor.shutdownNow()
        } catch (_: Exception) {
            // Ignore shutdown races.
        }
    }

    companion object {
        private const val API_BASE_URL = "https://soundvoice-8dtidqem.manus.space"
        private const val CLIENT_HEADER = "android"
        private const val TAG = "CloudTtsManager"
        private const val MAX_AUDIO_RESPONSE_BYTES = 20L * 1024L * 1024L
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
