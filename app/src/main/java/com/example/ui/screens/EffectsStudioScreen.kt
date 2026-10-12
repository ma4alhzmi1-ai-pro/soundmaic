package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.audio.VoicePresets
import com.example.viewmodel.VoiceStudioViewModel

@Composable
fun EffectsStudioScreen(viewModel: VoiceStudioViewModel) {
    val pitch by viewModel.pitch.collectAsState()
    val speed by viewModel.speed.collectAsState()
    val isDenoised by viewModel.isDenoised.collectAsState()
    val selectedBackground by viewModel.selectedBackground.collectAsState()
    val selectedReverb by viewModel.selectedReverb.collectAsState()

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "استوديو المؤثرات المتقدم والصدى",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "تحكم في صدى الصوت، القاعات، الحدة، والسرعة",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Pitch Slider
        Text(text = "حدة الصوت (Pitch): ${String.format("%.2f", pitch)}x", fontWeight = FontWeight.Bold)
        Slider(
            value = pitch,
            onValueChange = { viewModel.setPitch(it) },
            valueRange = 0.3f..2.5f,
            steps = 22
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Speed Slider
        Text(text = "سرعة الكلام (Speed): ${String.format("%.2f", speed)}x", fontWeight = FontWeight.Bold)
        Slider(
            value = speed,
            onValueChange = { viewModel.setSpeed(it) },
            valueRange = 0.5f..2.0f,
            steps = 15
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Reverb / Echo Effects
        Text(text = "تأثيرات الصدى والقاعات (Reverb & Echo):", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        VoicePresets.reverbs.forEach { rev ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = rev.nameAr)
                RadioButton(
                    selected = selectedReverb == rev.id,
                    onClick = { viewModel.setReverb(rev.id) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Noise Denoising Switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "إلغاء الضجيج المجاور وتنقيتها", fontWeight = FontWeight.Bold)
                Text(text = "فلترة الأصوات المحيطة للحصول على نقاء استوديوي عالٍ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
            Switch(
                checked = isDenoised,
                onCheckedChange = { viewModel.setDenoised(it) }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Background Sounds
        Text(text = "إضافات أصوات الخلفية الحية:", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        VoicePresets.backgroundSounds.forEach { bg ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = bg.nameAr)
                RadioButton(
                    selected = selectedBackground == bg.id,
                    onClick = { viewModel.setBackgroundSound(bg.id) }
                )
            }
        }
    }
}
