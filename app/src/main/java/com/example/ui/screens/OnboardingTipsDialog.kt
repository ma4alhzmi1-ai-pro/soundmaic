package com.example.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingTipsDialog(onHide: () -> Unit) {
    AlertDialog(
        onDismissRequest = onHide,
        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
        title = {
            Text(
                text = "مرحبًا بك في REMOVOICE",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "دليل سريع لاستخدام أدوات التطبيق:",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                ListItem(
                    leadingContent = { Icon(Icons.Default.Mic, contentDescription = null) },
                    headlineContent = { Text("المسجل") },
                    supportingContent = { Text("سجّل صوتك بجودة AAC ثم احفظه أو حوّله إلى نبرة أخرى") }
                )
                ListItem(
                    leadingContent = { Icon(Icons.Default.RecordVoiceOver, contentDescription = null) },
                    headlineContent = { Text("النص لصوت") },
                    supportingContent = { Text("اكتب النص واختر صوتًا طبيعيًا ثم اضغط تشغيل") }
                )
                ListItem(
                    leadingContent = { Icon(Icons.Default.Tune, contentDescription = null) },
                    headlineContent = { Text("التأثيرات") },
                    supportingContent = { Text("اضبط السرعة والنبرة والمؤثرات بدون مبالغة") }
                )
                Text(
                    text = "يمكنك فتح القائمة الجانبية من زر ☰ للوصول إلى الإعدادات والمحفوظات والنصائح في REMOVOICE.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = onHide) {
                Text("فهمت — إخفاء الدليل")
            }
        }
    )
}
