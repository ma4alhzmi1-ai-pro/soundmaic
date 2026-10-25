package com.example.audio

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/** Sends a recording to the Soundmaic API, which keeps the ElevenLabs key server-side. */
class VoiceConversionManager(private val context: Context) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        .build()

    suspend fun convertRecording(recordingReference: String, profileId: String): File = withContext(Dispatchers.IO) {
        require(profileId in SUPPORTED_PROFILES) { "الشخصية الصوتية المختارة غير مدعومة" }
        val upload = resolveUpload(recordingReference)
        try {
            require(upload.file.exists() && upload.file.length() > 0L) { "ملف التسجيل فارغ أو غير موجود" }
            require(upload.file.length() <= MAX_AUDIO_BYTES) { "حجم التسجيل أكبر من الحد المسموح لتحويل الصوت" }

            val form = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("model_id", STS_MODEL_ID)
                .addFormDataPart(
                    "audio",
                    upload.file.name,
                    upload.file.asRequestBody(upload.mimeType.toMediaType())
                )
                .build()
            val request = Request.Builder()
                .url("$API_BASE_URL/api/voice-convert?profile=$profileId")
                .header("X-Soundmaic-Client", CLIENT_HEADER)
                .post(form)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val responseText = response.body?.string().orEmpty()
                    val message = runCatching { JSONObject(responseText).optString("error") }
                        .getOrNull()
                        ?.takeIf { it.isNotBlank() }
                        ?: when (response.code) {
                            401 -> "تعذر التحقق من التطبيق لدى خدمة الصوت"
                            413 -> "حجم التسجيل أكبر من الحد المسموح"
                            429 -> "تجاوزت الخدمة حد الطلبات، حاول بعد قليل"
                            503 -> "خدمة ElevenLabs غير مهيأة حاليًا"
                            else -> "فشل تحويل الصوت (HTTP ${response.code})"
                        }
                    throw IllegalStateException(message)
                }

                val body = response.body ?: throw IllegalStateException("لم تُرجع الخدمة ملفًا صوتيًا")
                val mediaType = body.contentType()?.toString().orEmpty()
                if (!mediaType.startsWith("audio/")) {
                    throw IllegalStateException("أعادت الخدمة استجابة غير صوتية")
                }
                if (body.contentLength() > MAX_RESPONSE_BYTES) {
                    throw IllegalStateException("حجم الصوت الناتج أكبر من الحد المسموح")
                }
                val output = File(context.cacheDir, "elevenlabs_${System.currentTimeMillis()}.mp3")
                try {
                    body.byteStream().use { input ->
                        output.outputStream().use { target ->
                            val buffer = ByteArray(8 * 1024)
                            var totalBytes = 0L
                            while (true) {
                                val count = input.read(buffer)
                                if (count < 0) break
                                totalBytes += count
                                if (totalBytes > MAX_RESPONSE_BYTES) {
                                    throw IllegalStateException("حجم الصوت الناتج أكبر من الحد المسموح")
                                }
                                target.write(buffer, 0, count)
                            }
                        }
                    }
                    if (output.length() == 0L) throw IllegalStateException("ملف الصوت المحوّل فارغ")
                    output
                } catch (error: Throwable) {
                    output.delete()
                    throw error
                }
            }
        } finally {
            if (upload.temporary) upload.file.delete()
        }
    }

    private fun resolveUpload(reference: String): UploadAudio {
        if (!reference.startsWith("content://")) {
            val file = File(reference)
            if (!file.isFile) throw IllegalStateException("ملف التسجيل غير موجود")
            return UploadAudio(file, mimeTypeFor(file.extension), temporary = false)
        }

        val uri = Uri.parse(reference)
        val resolver = context.contentResolver
        val displayName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)) else null
            }
        val extension = displayName?.substringAfterLast('.', "")?.lowercase()?.takeIf { it.matches(Regex("[a-z0-9]{1,8}")) }
            ?: extensionForMime(resolver.getType(uri))
        val target = File(context.cacheDir, "voice_upload_${System.currentTimeMillis()}.$extension")
        try {
            resolver.openInputStream(uri)?.use { input -> target.outputStream().use { input.copyTo(it) } }
                ?: throw IllegalStateException("تعذر قراءة التسجيل المحدد")
            return UploadAudio(target, resolver.getType(uri) ?: mimeTypeFor(extension), temporary = true)
        } catch (error: Throwable) {
            target.delete()
            throw error
        }
    }

    private fun mimeTypeFor(extension: String): String = when (extension.lowercase()) {
        "m4a", "mp4", "aac" -> "audio/mp4"
        "ogg", "oga" -> "audio/ogg"
        "mp3" -> "audio/mpeg"
        "wav" -> "audio/wav"
        "flac" -> "audio/flac"
        "webm" -> "audio/webm"
        else -> "application/octet-stream"
    }

    private fun extensionForMime(mimeType: String?): String = when (mimeType?.lowercase()) {
        "audio/ogg" -> "ogg"
        "audio/mpeg" -> "mp3"
        "audio/wav", "audio/x-wav" -> "wav"
        "audio/flac" -> "flac"
        "audio/webm" -> "webm"
        else -> "m4a"
    }

    private data class UploadAudio(val file: File, val mimeType: String, val temporary: Boolean)

    companion object {
        private const val API_BASE_URL = "https://soundvoice-8dtidqem.manus.space"
        private const val CLIENT_HEADER = "android"
        private const val STS_MODEL_ID = "eleven_multilingual_sts_v2"
        // Leave room for multipart headers under the API's 15 MiB request cap.
        private const val MAX_AUDIO_BYTES = 15L * 1024L * 1024L - 64L * 1024L
        private const val MAX_RESPONSE_BYTES = 20L * 1024L * 1024L
        private val SUPPORTED_PROFILES = setOf(
            "young_woman", "middle_woman", "child_10", "young_man",
            "elderly_man", "robot", "alien", "monster"
        )
    }
}
