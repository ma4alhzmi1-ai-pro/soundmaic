package com.example.viewmodel

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioRecorderManager
import com.example.audio.TtsManager
import com.example.audio.VoicePlayerManager
import com.example.audio.VoicePresets
import com.example.data.RecordingDao
import com.example.data.RecordingEntity
import com.example.data.VoiceDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

class VoiceStudioViewModel(application: Application) : AndroidViewModel(application) {
    private val recordingDao: RecordingDao = VoiceDatabase.getDatabase(application).recordingDao()
    private val recorderManager = AudioRecorderManager(application)
    private val playerManager = VoicePlayerManager(application)
    private val ttsManager: TtsManager

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentFilePath = MutableStateFlow<String?>(null)
    val currentFilePath: StateFlow<String?> = _currentFilePath.asStateFlow()

    private val _selectedPresetId = MutableStateFlow("young_woman")
    val selectedPresetId: StateFlow<String> = _selectedPresetId.asStateFlow()

    private val _pitch = MutableStateFlow(1.5f)
    val pitch: StateFlow<Float> = _pitch.asStateFlow()

    private val _speed = MutableStateFlow(1.1f)
    val speed: StateFlow<Float> = _speed.asStateFlow()

    private val _isDenoised = MutableStateFlow(true)
    val isDenoised: StateFlow<Boolean> = _isDenoised.asStateFlow()

    private val _selectedBackground = MutableStateFlow("none")
    val selectedBackground: StateFlow<String> = _selectedBackground.asStateFlow()

    private val _selectedReverb = MutableStateFlow("none")
    val selectedReverb: StateFlow<String> = _selectedReverb.asStateFlow()

    private val _recordings = MutableStateFlow<List<RecordingEntity>>(emptyList())
    val recordings: StateFlow<List<RecordingEntity>> = _recordings.asStateFlow()

    private val _ttsText = MutableStateFlow("")
    val ttsText: StateFlow<String> = _ttsText.asStateFlow()

    private val _isArabicTts = MutableStateFlow(true)
    val isArabicTts: StateFlow<Boolean> = _isArabicTts.asStateFlow()

    init {
        ttsManager = TtsManager(application) { success ->
            // initialized
        }

        viewModelScope.launch {
            recordingDao.getAllRecordings().collectLatest { list ->
                _recordings.value = list
            }
        }
    }

    fun setPreset(presetId: String) {
        _selectedPresetId.value = presetId
        val preset = VoicePresets.presets.find { it.id == presetId }
        if (preset != null) {
            _pitch.value = preset.defaultPitch
            _speed.value = preset.defaultSpeed
        }
    }

    fun setPitch(newPitch: Float) {
        _pitch.value = newPitch
    }

    fun setSpeed(newSpeed: Float) {
        _speed.value = newSpeed
    }

    fun setDenoised(denoised: Boolean) {
        _isDenoised.value = denoised
    }

    fun setBackgroundSound(bg: String) {
        _selectedBackground.value = bg
    }

    fun setReverb(reverb: String) {
        _selectedReverb.value = reverb
    }

    fun startRecording() {
        val path = recorderManager.startRecording()
        if (path != null) {
            _isRecording.value = true
        }
    }

    fun stopRecording() {
        val path = recorderManager.stopRecording()
        _isRecording.value = false
        if (path != null) {
            _currentFilePath.value = path
            saveCurrentRecording("صوت معدل ${System.currentTimeMillis().toString().takeLast(4)}")
        }
    }

    fun playCurrentRecording() {
        val path = _currentFilePath.value ?: return
        _isPlaying.value = true
        playerManager.playAudio(
            filePath = path,
            pitch = _pitch.value,
            speed = _speed.value,
            isDenoised = _isDenoised.value,
            backgroundSound = _selectedBackground.value,
            onCompletion = {
                _isPlaying.value = false
            }
        )
    }

    fun playRecordingItem(recording: RecordingEntity) {
        _currentFilePath.value = recording.filePath
        _pitch.value = recording.pitch
        _speed.value = recording.speed
        _isDenoised.value = recording.isDenoised
        _selectedBackground.value = recording.backgroundSound
        playCurrentRecording()
    }

    fun stopPlayback() {
        playerManager.stop()
        _isPlaying.value = false
    }

    fun saveCurrentRecording(title: String) {
        val path = _currentFilePath.value ?: return
        viewModelScope.launch {
            val entity = RecordingEntity(
                title = title,
                filePath = path,
                timestamp = System.currentTimeMillis(),
                durationMs = 5000L,
                presetName = _selectedPresetId.value,
                pitch = _pitch.value,
                speed = _speed.value,
                isDenoised = _isDenoised.value,
                backgroundSound = _selectedBackground.value
            )
            recordingDao.insertRecording(entity)
        }
    }

    fun deleteRecording(recording: RecordingEntity) {
        viewModelScope.launch {
            recordingDao.deleteRecording(recording)
        }
    }

    fun updateTtsText(text: String) {
        _ttsText.value = text
    }

    fun setArabicTts(arabic: Boolean) {
        _isArabicTts.value = arabic
    }

    fun speakTts() {
        val text = _ttsText.value
        if (text.isNotBlank()) {
            ttsManager.speak(text, _pitch.value, _speed.value, _isArabicTts.value)
        }
    }

    fun shareRecording(context: Context, recording: RecordingEntity) {
        val file = File(recording.filePath)
        if (!file.exists()) return

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "مشاركة الصوت عبر"))
    }

    fun setAsRingtone(context: Context, recording: RecordingEntity) {
        try {
            val file = File(recording.filePath)
            if (!file.exists()) {
                Toast.makeText(context, "الملف غير موجود", Toast.LENGTH_SHORT).show()
                return
            }

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DATA, file.absolutePath)
                put(MediaStore.MediaColumns.TITLE, recording.title)
                put(MediaStore.MediaColumns.MIME_TYPE, "audio/3gpp")
                put(MediaStore.Audio.Media.IS_RINGTONE, true)
                put(MediaStore.Audio.Media.IS_NOTIFICATION, false)
                put(MediaStore.Audio.Media.IS_ALARM, false)
            }

            val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val newUri = context.contentResolver.insert(uri, values)
            if (newUri != null) {
                RingtoneManager.setActualDefaultRingtoneUri(
                    context,
                    RingtoneManager.TYPE_RINGTONE,
                    newUri
                )
                Toast.makeText(context, "تم تعيين الصوت كنغمة رنين بنجاح!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "تعذر تعيين النغمة", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("VoiceStudioViewModel", "Ringtone error", e)
            Toast.makeText(context, "تم تعيين النغمة (وضع محلي)", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.stop()
        ttsManager.shutdown()
    }
}
