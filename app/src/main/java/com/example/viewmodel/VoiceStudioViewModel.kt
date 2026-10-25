package com.example.viewmodel

import android.app.Application
import android.content.ContentValues
import android.content.ClipData
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
import com.example.audio.AudioStorageManager
import com.example.audio.TtsManager
import com.example.audio.VoiceConversionManager
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
    private val audioStorage = AudioStorageManager(application)
    private val voiceConversionManager = VoiceConversionManager(application)
    private val playerManager = VoicePlayerManager(application)
    private val ttsManager: TtsManager

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentFilePath = MutableStateFlow<String?>(null)
    val currentFilePath: StateFlow<String?> = _currentFilePath.asStateFlow()

    private val _convertedFilePath = MutableStateFlow<String?>(null)
    val convertedFilePath: StateFlow<String?> = _convertedFilePath.asStateFlow()

    private val _isConvertingVoice = MutableStateFlow(false)
    val isConvertingVoice: StateFlow<Boolean> = _isConvertingVoice.asStateFlow()

    private val _voiceConversionMessage = MutableStateFlow<String?>(null)
    val voiceConversionMessage: StateFlow<String?> = _voiceConversionMessage.asStateFlow()

    private val _selectedPresetId = MutableStateFlow("young_woman")
    val selectedPresetId: StateFlow<String> = _selectedPresetId.asStateFlow()

    private val _pitch = MutableStateFlow(1.08f)
    val pitch: StateFlow<Float> = _pitch.asStateFlow()

    private val _speed = MutableStateFlow(1.0f)
    val speed: StateFlow<Float> = _speed.asStateFlow()

    private val _isDenoised = MutableStateFlow(true)
    val isDenoised: StateFlow<Boolean> = _isDenoised.asStateFlow()

    private val _selectedBackground = MutableStateFlow("none")
    val selectedBackground: StateFlow<String> = _selectedBackground.asStateFlow()

    private val _selectedReverb = MutableStateFlow("none")
    val selectedReverb: StateFlow<String> = _selectedReverb.asStateFlow()

    private val _recordings = MutableStateFlow<List<RecordingEntity>>(emptyList())
    val recordings: StateFlow<List<RecordingEntity>> = _recordings.asStateFlow()

    private val _showAudioInPlayers = MutableStateFlow(audioStorage.isVisibleInPlayers())
    val showAudioInPlayers: StateFlow<Boolean> = _showAudioInPlayers.asStateFlow()

    private val _ttsText = MutableStateFlow("")
    val ttsText: StateFlow<String> = _ttsText.asStateFlow()

    private val _ttsMessage = MutableStateFlow<String?>(null)
    val ttsMessage: StateFlow<String?> = _ttsMessage.asStateFlow()

    private val _isArabicTts = MutableStateFlow(true)
    val isArabicTts: StateFlow<Boolean> = _isArabicTts.asStateFlow()

    init {
        ttsManager = TtsManager(
            context = application,
            onAudioGenerated = { reference, title ->
                _ttsMessage.value = "تم توليد الصوت عبر ElevenLabs بنجاح"
                saveGeneratedTts(reference, title)
            },
            onError = { message -> _ttsMessage.value = message }
        )

        viewModelScope.launch {
            recordingDao.getAllRecordings().collectLatest { list ->
                _recordings.value = list
            }
        }
    }

    fun setPreset(presetId: String) {
        _selectedPresetId.value = presetId
        _convertedFilePath.value = null
        _voiceConversionMessage.value = null
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
            _convertedFilePath.value = null
            _voiceConversionMessage.value = null
            val title = "REMOVOICE_${System.currentTimeMillis().toString().takeLast(6)}"
            val storedPath = audioStorage.saveFile(File(path), title) ?: path
            _currentFilePath.value = storedPath
            if (storedPath != path) File(path).delete()
            saveCurrentRecording(title)
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
        _convertedFilePath.value = null
        _voiceConversionMessage.value = null
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

    fun convertCurrentRecording() {
        val recordingReference = _currentFilePath.value
        if (recordingReference.isNullOrBlank()) {
            _voiceConversionMessage.value = "سجّل صوتًا أو اختر تسجيلًا أولًا"
            return
        }
        if (_isConvertingVoice.value) return

        val targetProfile = _selectedPresetId.value
        _convertedFilePath.value = null
        _voiceConversionMessage.value = null
        _isConvertingVoice.value = true
        viewModelScope.launch {
            try {
                val convertedFile = voiceConversionManager.convertRecording(recordingReference, targetProfile)
                val title = "REMOVOICE_ELEVENLABS_${System.currentTimeMillis().toString().takeLast(6)}"
                val savedReference = audioStorage.saveFile(convertedFile, title) ?: convertedFile.absolutePath
                _convertedFilePath.value = savedReference
                _voiceConversionMessage.value = "اكتمل التحويل الفعلي عبر ElevenLabs إلى ${VoicePresets.presets.firstOrNull { it.id == targetProfile }?.nameAr ?: targetProfile}"
                recordingDao.insertRecording(
                    RecordingEntity(
                        title = title,
                        filePath = savedReference,
                        timestamp = System.currentTimeMillis(),
                        durationMs = 0L,
                        presetName = "ElevenLabs:$targetProfile",
                        pitch = 1.0f,
                        speed = 1.0f,
                        isDenoised = false,
                        backgroundSound = "none"
                    )
                )
                if (savedReference != convertedFile.absolutePath) convertedFile.delete()
            } catch (error: Exception) {
                Log.e("VoiceStudioViewModel", "ElevenLabs voice conversion failed", error)
                _voiceConversionMessage.value = error.message ?: "تعذر تحويل التسجيل حاليًا"
            } finally {
                _isConvertingVoice.value = false
            }
        }
    }

    fun playConvertedRecording() {
        val path = _convertedFilePath.value ?: return
        _isPlaying.value = true
        playerManager.playAudio(
            filePath = path,
            pitch = 1.0f,
            speed = 1.0f,
            isDenoised = false,
            backgroundSound = "none",
            onCompletion = { _isPlaying.value = false }
        )
    }

    fun shareConvertedRecording(context: Context) {
        val path = _convertedFilePath.value ?: return
        shareAudioReference(context, path, "صوت ElevenLabs المحوّل")
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
            if (recording.filePath.startsWith("content://")) {
                runCatching {
                    getApplication<Application>().contentResolver.delete(Uri.parse(recording.filePath), null, null)
                }
            } else {
                File(recording.filePath).delete()
            }
        }
    }

    private fun saveGeneratedTts(reference: String, title: String) {
        viewModelScope.launch {
            recordingDao.insertRecording(RecordingEntity(
                title = title,
                filePath = reference,
                timestamp = System.currentTimeMillis(),
                durationMs = 0L,
                presetName = "TTS",
                pitch = 1.0f,
                speed = 1.0f,
                isDenoised = false,
                backgroundSound = "none"
            ))
        }
    }

    fun saveRecordingToDevice(context: Context, recording: RecordingEntity) {
        if (recording.filePath.startsWith("content://")) {
            Toast.makeText(context, "الصوت محفوظ بالفعل داخل Music/REMOVOICE", Toast.LENGTH_SHORT).show()
            return
        }
        val saved = audioStorage.saveFile(File(recording.filePath), recording.title)
        Toast.makeText(context, if (saved != null) "تم حفظ الصوت داخل Music/REMOVOICE" else "تعذر حفظ الصوت", Toast.LENGTH_SHORT).show()
    }

    fun updateTtsText(text: String) {
        _ttsText.value = text
        _ttsMessage.value = null
    }

    fun setArabicTts(arabic: Boolean) {
        _isArabicTts.value = arabic
    }

    fun setShowAudioInPlayers(visible: Boolean) {
        audioStorage.setVisibleInPlayers(visible)
        _showAudioInPlayers.value = visible
    }

    fun speakTts() {
        val text = _ttsText.value
        if (text.isNotBlank()) {
            _ttsMessage.value = null
            ttsManager.speak(
                text = text,
                speed = 1.0f,
                isArabic = _isArabicTts.value,
                profileId = "young_woman"
            )
        }
    }

    fun stopTts() {
        ttsManager.stop()
    }

    fun shareRecording(context: Context, recording: RecordingEntity) {
        shareAudioReference(context, recording.filePath, recording.title)
    }

    fun shareCurrentRecording(context: Context) {
        val path = _currentFilePath.value ?: return
        shareAudioReference(context, path, "تسجيل REMOVOICE")
    }

    private fun shareAudioReference(context: Context, reference: String, title: String) {
        val file = if (reference.startsWith("content://")) null else File(reference)
        if (file != null && !file.exists()) {
            Toast.makeText(context, "الملف الصوتي غير موجود", Toast.LENGTH_SHORT).show()
            return
        }

        val uri = if (reference.startsWith("content://")) Uri.parse(reference)
        else FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file!!)
        val mimeType = if (file == null) {
            context.contentResolver.getType(uri) ?: "audio/*"
        } else {
            audioMimeType(file.extension)
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, title)
            clipData = ClipData.newRawUri(title, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "مشاركة الصوت عبر"))
    }

    private fun audioMimeType(extension: String): String = when (extension.lowercase()) {
        "m4a", "mp4", "aac" -> "audio/mp4"
        "3gp" -> "audio/3gpp"
        "ogg", "oga" -> "audio/ogg"
        "wav" -> "audio/wav"
        "flac" -> "audio/flac"
        "webm" -> "audio/webm"
        else -> "audio/mpeg"
    }

    fun setAsRingtone(context: Context, recording: RecordingEntity) {
        try {
            if (recording.filePath.startsWith("content://")) {
                RingtoneManager.setActualDefaultRingtoneUri(
                    context,
                    RingtoneManager.TYPE_RINGTONE,
                    Uri.parse(recording.filePath)
                )
                Toast.makeText(context, "تم تعيين الصوت كنغمة رنين بنجاح!", Toast.LENGTH_LONG).show()
                return
            }
            val file = File(recording.filePath)
            if (!file.exists()) {
                Toast.makeText(context, "الملف غير موجود", Toast.LENGTH_SHORT).show()
                return
            }

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DATA, file.absolutePath)
                put(MediaStore.MediaColumns.TITLE, recording.title)
                put(MediaStore.MediaColumns.MIME_TYPE, audioMimeType(file.extension))
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
            Toast.makeText(context, "تعذر تعيين النغمة", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.stop()
        ttsManager.shutdown()
    }
}
