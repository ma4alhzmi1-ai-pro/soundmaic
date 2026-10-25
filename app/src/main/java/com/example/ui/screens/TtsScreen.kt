package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.RecordingEntity
import com.example.viewmodel.VoiceStudioViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TtsScreen(viewModel: VoiceStudioViewModel) {
    val context = LocalContext.current
    val ttsText by viewModel.ttsText.collectAsState()
    val ttsMessage by viewModel.ttsMessage.collectAsState()
    val isArabic by viewModel.isArabicTts.collectAsState()
    val recordings by viewModel.recordings.collectAsState()
    val ttsRecordings = recordings.filter { it.presetName == "TTS" }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text("تحويل النص المكتوب إلى صوت ذكي", style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(4.dp))
        Text("يُولّد النص عبر ElevenLabs عند الاتصال بالإنترنت، ثم يُحفظ الناتج أسفل الشاشة.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("يستخدم هذا القسم ملف «فتاة شابة» (young_woman) دائمًا، مستقلًا عن شخصية تحويل التسجيل.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FilterChip(isArabic, { viewModel.setArabicTts(true) }, label = { Text("العربية") })
            FilterChip(!isArabic, { viewModel.setArabicTts(false) }, label = { Text("English") })
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = ttsText,
            onValueChange = viewModel::updateTtsText,
            label = { Text("اكتب النص هنا...") },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            maxLines = 5
        )
        Spacer(Modifier.height(10.dp))
        Button(onClick = viewModel::speakTts, modifier = Modifier.fillMaxWidth()) {
            Text("توليد ونطق النص صوتيًا")
        }
        ttsMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = if (message.startsWith("تم توليد")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        Text("الأصوات المحولة (${ttsRecordings.size})", style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))

        if (ttsRecordings.isEmpty()) {
            Text("ستظهر الأصوات المحولة هنا بعد اكتمال التوليد.",
                color = MaterialTheme.colorScheme.outline)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ttsRecordings, key = { it.id }) { recording ->
                    TtsRecordingCard(recording, viewModel, context)
                }
            }
        }
    }
}

@Composable
private fun TtsRecordingCard(
    recording: RecordingEntity,
    viewModel: VoiceStudioViewModel,
    context: android.content.Context
) {
    val date = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        .format(Date(recording.timestamp))
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(10.dp)) {
            Text(recording.title, fontWeight = FontWeight.Bold)
            Text(date, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.playRecordingItem(recording) }) {
                    Icon(Icons.Default.PlayArrow, "إعادة التشغيل")
                }
                IconButton(onClick = viewModel::stopTts) {
                    Icon(Icons.Default.Stop, "إيقاف التشغيل")
                }
                IconButton(onClick = { viewModel.saveRecordingToDevice(context, recording) }) {
                    Icon(Icons.Default.Download, "حفظ الصوت")
                }
                IconButton(onClick = { viewModel.shareRecording(context, recording) }) {
                    Icon(Icons.Default.Share, "مشاركة الصوت")
                }
                IconButton(onClick = { viewModel.deleteRecording(recording) }) {
                    Icon(Icons.Default.Delete, "حذف الصوت", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
