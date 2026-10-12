package com.example.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.viewmodel.VoiceStudioViewModel

@Composable
fun SettingsScreen(viewModel: VoiceStudioViewModel) {
    val showAudioInPlayers by viewModel.showAudioInPlayers.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "الإعدادات وحالة التطبيق",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.padding(4.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            ListItem(
                leadingContent = { Icon(Icons.Default.CloudDone, contentDescription = null) },
                headlineContent = { Text("الخدمة السحابية متصلة") },
                supportingContent = { Text("تحويل النص إلى صوت طبيعي بجودة ElevenLabs مع إبقاء المفتاح على الخادم") }
            )
        }
        Spacer(modifier = Modifier.padding(6.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            ListItem(
                headlineContent = { Text("إظهار الأصوات في مشغلات الهاتف") },
                supportingContent = {
                    Text(
                        if (showAudioInPlayers) {
                            "مفعّل — ستظهر الملفات في تطبيقات الموسيقى والملفات"
                        } else {
                            "مخفي — تبقى الملفات محفوظة داخل Music/REMOVOICE دون ظهورها في المشغلات"
                        }
                    )
                },
                trailingContent = {
                    Switch(
                        checked = showAudioInPlayers,
                        onCheckedChange = viewModel::setShowAudioInPlayers
                    )
                }
            )
        }
        Spacer(modifier = Modifier.padding(6.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            ListItem(
                leadingContent = { Icon(Icons.Default.Security, contentDescription = null) },
                headlineContent = { Text("الخصوصية والحماية") },
                supportingContent = { Text("يتم حفظ الأصوات محليًا داخل مجلد Music/REMOVOICE، ويمكن إخفاؤها عن المشغلات من المفتاح أعلاه") }
            )
        }
        Spacer(modifier = Modifier.padding(6.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            ListItem(
                leadingContent = { Icon(Icons.Default.Lightbulb, contentDescription = null) },
                headlineContent = { Text("نصيحة للحصول على صوت واقعي") },
                supportingContent = { Text("اكتب جملًا قصيرة بعلامات ترقيم واضحة، واختر سرعة بين 0.9 و1.1 للحفاظ على النبرة الطبيعية") }
            )
        }
        Spacer(modifier = Modifier.padding(16.dp))

        Text(
            text = "حول التطبيق",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.padding(4.dp))
        Text(
            text = "REMOVOICE 1.0.0\nاستوديو صوت احترافي للتسجيل، المؤثرات، تحويل النص إلى صوت، وحفظ الأعمال.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
