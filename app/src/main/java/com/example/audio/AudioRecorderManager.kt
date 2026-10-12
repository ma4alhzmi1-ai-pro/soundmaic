package com.example.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException

class AudioRecorderManager(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentFilePath: String? = null

    val recordingFilePath: String?
        get() = currentFilePath

    fun startRecording(): String? {
        val outputDir = context.cacheDir
        val audioFile = File(outputDir, "voicecraft_${System.currentTimeMillis()}.3gp")
        currentFilePath = audioFile.absolutePath

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(currentFilePath)
            try {
                prepare()
                start()
            } catch (e: IOException) {
                Log.e("AudioRecorderManager", "prepare() failed", e)
                return null
            } catch (e: Exception) {
                Log.e("AudioRecorderManager", "start() failed", e)
                return null
            }
        }
        return currentFilePath
    }

    fun stopRecording(): String? {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioRecorderManager", "stopRecording error", e)
        } finally {
            mediaRecorder = null
        }
        return currentFilePath
    }
}
