package com.example.audio

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

/** Persists generated audio in the user's Music/REMOVOICE folder. */
class AudioStorageManager(private val context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun isVisibleInPlayers(): Boolean = preferences.getBoolean(KEY_SHOW_IN_PLAYERS, false)

    fun setVisibleInPlayers(visible: Boolean) {
        preferences.edit().putBoolean(KEY_SHOW_IN_PLAYERS, visible).apply()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(MediaStore.Audio.Media._ID)
            val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
            val args = arrayOf("${Environment.DIRECTORY_MUSIC}/REMOVOICE/%")
            resolver.query(collection, projection, selection, args, null)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val item = Uri.withAppendedPath(collection, id.toString())
                    val values = ContentValues().apply {
                        put(MediaStore.Audio.AudioColumns.IS_MUSIC, if (visible) 1 else 0)
                    }
                    resolver.update(item, values, null, null)
                }
            }
        }
    }

    /** Returns a content URI on Android 10+, or a FileProvider-compatible path on older devices. */
    fun saveFile(source: File, displayName: String): String? {
        if (!source.exists()) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveThroughMediaStore(source, displayName)
        } else {
            saveForLegacyDevices(source, displayName)
        }
    }

    private fun saveThroughMediaStore(source: File, displayName: String): String? {
        val extension = source.extension.lowercase().ifBlank { "m4a" }
        val mime = if (extension == "mp3") "audio/mpeg" else "audio/mp4"
        val safeName = displayName.replace(Regex("[^A-Za-z0-9_-]"), "_").take(48)
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$safeName.$extension")
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/REMOVOICE")
            put(MediaStore.Audio.AudioColumns.IS_MUSIC, if (isVisibleInPlayers()) 1 else 0)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        return try {
            resolver.openOutputStream(uri)?.use { output -> source.inputStream().use { it.copyTo(output) } }
                ?: throw IllegalStateException("Unable to open MediaStore output")
            resolver.update(uri, ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }, null, null)
            uri.toString()
        } catch (_: Throwable) {
            resolver.delete(uri, null, null)
            null
        }
    }

    private fun saveForLegacyDevices(source: File, displayName: String): String? {
        val directory = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "REMOVOICE")
        if (!directory.exists()) directory.mkdirs()
        val target = File(directory, "${displayName.replace(Regex("[^A-Za-z0-9_-]"), "_")}.${source.extension}")
        return try {
            source.copyTo(target, overwrite = true)
            MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), arrayOf("audio/*"), null)
            target.absolutePath
        } catch (_: Throwable) {
            null
        }
    }

    companion object {
        private const val PREFERENCES = "soundmaic_settings"
        private const val KEY_SHOW_IN_PLAYERS = "show_audio_in_players"
    }
}
