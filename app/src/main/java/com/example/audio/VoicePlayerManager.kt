package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import java.io.File

class VoicePlayerManager(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var backgroundPlayer: MediaPlayer? = null

    fun playAudio(
        filePath: String,
        pitch: Float,
        speed: Float,
        isDenoised: Boolean,
        backgroundSound: String,
        onCompletion: () -> Unit
    ) {
        stop()
        try {
            mediaPlayer = MediaPlayer().apply {
                if (filePath.startsWith("content://")) {
                    setDataSource(context, Uri.parse(filePath))
                } else {
                    val file = File(filePath)
                    if (!file.exists()) return
                    setDataSource(context, Uri.fromFile(file))
                }
                setOnPreparedListener { mp ->
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        try {
                            // Large pitch changes are the main source of the robotic
                            // artefact. Keep human voices in a conservative range
                            // and avoid changing the duration aggressively.
                            val naturalPitch = pitch.coerceIn(0.85f, 1.15f)
                            val naturalSpeed = speed.coerceIn(0.92f, 1.08f)
                            mp.playbackParams = mp.playbackParams
                                .setSpeed(naturalSpeed)
                                .setPitch(naturalPitch)
                        } catch (e: Exception) {
                            Log.e("VoicePlayer", "PlaybackParams error", e)
                        }
                    }
                    mp.start()
                }
                setOnCompletionListener {
                    stopBackground()
                    onCompletion()
                }
                prepareAsync()
            }

            // Play background sound if requested
            if (backgroundSound != "none") {
                startBackground(backgroundSound)
            }
        } catch (e: Exception) {
            Log.e("VoicePlayerManager", "playAudio error", e)
        }
    }

    private fun startBackground(soundType: String) {
        // For demonstration of background sound overlay, we can use synthesized beeps or silent loop or bundled sound if available, or looped media if resource exists.
        // To be robust offline without missing asset errors, we can use ToneGenerator or a safe synthetic stream if needed.
    }

    private fun stopBackground() {
        try {
            backgroundPlayer?.stop()
            backgroundPlayer?.release()
        } catch (e: Exception) {
            // ignore
        }
        backgroundPlayer = null
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            // ignore
        }
        mediaPlayer = null
        stopBackground()
    }
}
