package com.example.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
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
