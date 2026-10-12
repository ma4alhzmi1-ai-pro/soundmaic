package com.example

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.ui.screens.MainDashboardScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.OnboardingTipsDialog
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.VoiceStudioViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: VoiceStudioViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val hasInternet = isInternetAvailable()
        setContent {
            MyApplicationTheme {
                var showLogin by remember { mutableStateOf(true) }
                var showOnboarding by remember { mutableStateOf(false) }
                if (showLogin) {
                    LoginScreen(
                        hasInternet = hasInternet,
                        onFinished = {
                            showLogin = false
                            showOnboarding = !getPreferences(MODE_PRIVATE)
                                .getBoolean(ONBOARDING_SEEN, false)
                        }
                    )
                } else {
                    MainDashboardScreen(viewModel = viewModel)
                    if (showOnboarding) {
                        OnboardingTipsDialog(
                            onHide = {
                                getPreferences(MODE_PRIVATE)
                                    .edit()
                                    .putBoolean(ONBOARDING_SEEN, true)
                                    .apply()
                                showOnboarding = false
                            }
                        )
                    }
                }
            }
        }
    }

    private fun isInternetAvailable(): Boolean {
        val connectivity = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivity.activeNetwork ?: return false
        val capabilities = connectivity.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    companion object {
        private const val ONBOARDING_SEEN = "soundmaic_onboarding_seen"
    }
}
