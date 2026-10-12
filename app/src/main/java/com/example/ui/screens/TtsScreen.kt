package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.viewmodel.VoiceStudioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TtsScreen(viewModel: VoiceStudioViewModel) {
    val ttsText by viewModel.ttsText.collectAsState()
    val isArabic by viewModel.isArabicTts.collectAsState()
    val pitch by viewModel.pitch.collectAsState()
    val speed by viewModel.speed.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "تحويل النص المكتوب إلى صوت ذكي",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "اكتب أي نص لتحويله بصوت فتاة، امرأة، شاب أو طفل بدون إنترنت",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Language selector
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = isArabic,
                onClick = { viewModel.setArabicTts(true) },
                label = { Text("اللغة العربية") }
            )
            FilterChip(
                selected = !isArabic,
                onClick = { viewModel.setArabicTts(false) },
                label = { Text("English") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = ttsText,
            onValueChange = { viewModel.updateTtsText(it) },
            label = { Text("اكتب النص هنا...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            maxLines = 5
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.speakTts() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "توليد ونطق النص صوتياً")
        }
    }
}
