package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.viewmodel.VoiceStudioViewModel
import kotlinx.coroutines.launch

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
    val items = listOf(Screen.Recorder, Screen.Studio, Screen.Tts, Screen.Saved, Screen.Settings)
    var selectedScreen by remember { mutableStateOf<Screen>(Screen.Recorder) }
    var copyrightVisible by remember { mutableStateOf(true) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1800L)
            copyrightVisible = !copyrightVisible
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.padding(vertical = 20.dp)) {
                    Text(
                        text = "Soundmaic",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Text(
                        text = "استوديو الصوت الذكي",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.padding(8.dp))
                    items.forEach { screen ->
                        NavigationDrawerItem(
                            icon = { Icon(screen.icon, contentDescription = screen.titleAr) },
                            label = { Text(screen.titleAr) },
                            selected = selectedScreen == screen,
                            onClick = {
                                selectedScreen = screen
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        )
                    }
                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                    ListItem(
                        leadingContent = { Icon(Icons.Default.CloudDone, contentDescription = null) },
                        headlineContent = { Text("الخدمة السحابية متصلة") },
                        supportingContent = { Text("تحويل صوت طبيعي عبر ElevenLabs") }
                    )
                    ListItem(
                        leadingContent = { Icon(Icons.Default.Lightbulb, contentDescription = null) },
                        headlineContent = { Text("اقتراح سريع") },
                        supportingContent = { Text("استخدم التسجيل بجودة AAC قبل التحويل") }
                    )
                    ListItem(
                        leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                        headlineContent = { Text("عن التطبيق") },
                        supportingContent = { Text("إصدار 1.0.0 • جميع الحقوق محفوظة 2026") }
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Soundmaic | ${selectedScreen.titleAr}") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "فتح الإعدادات الجانبية")
                        }
                    },
                    actions = {
                        IconButton(onClick = { selectedScreen = Screen.Settings }) {
                            Icon(Icons.Default.Settings, contentDescription = "الإعدادات")
                        }
                    }
                )
            },
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
                    AnimatedVisibility(
                        visible = copyrightVisible,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "هذا التطبيق برمجة وتطوير المطور محمد الحزمي\nجميع الحقوق محفوظة للمطور 2026",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
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
}
