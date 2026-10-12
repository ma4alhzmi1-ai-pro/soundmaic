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
        // AMR-NB/3GP introduces strong telephone-bandwidth artifacts that become
        // very noticeable after pitch shifting. Record clean AAC at 44.1 kHz instead.
        val audioFile = File(outputDir, "voicecraft_${System.currentTimeMillis()}.m4a")
        currentFilePath = audioFile.absolutePath

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44_100)
            setAudioEncodingBitRate(128_000)
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
