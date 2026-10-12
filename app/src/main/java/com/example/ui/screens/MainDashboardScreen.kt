package com.example.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.viewmodel.VoiceStudioViewModel

sealed class Screen(val route: String, val titleAr: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Recorder : Screen("recorder", "المسجل", Icons.Default.Mic)
    object Studio : Screen("studio", "التأثيرات", Icons.Default.Tune)
    object Tts : Screen("tts", "النص لصوت", Icons.Default.RecordVoiceOver)
    object Saved : Screen("saved", "المحفوظات", Icons.Default.Folder)
    object Settings : Screen("settings", "الإعدادات", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(viewModel: VoiceStudioViewModel) {
    val items = listOf(
        Screen.Recorder,
        Screen.Studio,
        Screen.Tts,
        Screen.Saved,
        Screen.Settings
    )
    var selectedScreen by remember { mutableStateOf<Screen>(Screen.Recorder) }
    var copyrightVisible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1800L)
            copyrightVisible = !copyrightVisible
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.titleAr) },
                        label = { Text(screen.titleAr) },
                        selected = selectedScreen == screen,
                        onClick = { selectedScreen = screen }
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = copyrightVisible,
                    enter = androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "هذا التطبيق برمجة وتطوير المطور محمد الحزمي\nجميع الحقوق محفوظة للمطور 2026",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedScreen) {
                        Screen.Recorder -> RecorderScreen(viewModel)
                        Screen.Studio -> EffectsStudioScreen(viewModel)
                        Screen.Tts -> TtsScreen(viewModel)
                        Screen.Saved -> RecordingsListScreen(viewModel)
                        Screen.Settings -> SettingsScreen()
                    }
                }
            }
        }
    }
}
