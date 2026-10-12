package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import kotlinx.coroutines.delay

@Composable
fun LoginScreen(
    hasInternet: Boolean,
    onFinished: () -> Unit
) {
    var progress by remember { mutableFloatStateOf(0f) }
    var statusVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Deliberately slow so the user can see the connection check and progress.
        for (step in 0..100) {
            progress = step / 100f
            if (step == 18) statusVisible = true
            delay(70L)
        }
        delay(350L)
        onFinished()
    }


    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.login_background),
            contentDescription = "شاشة دخول Soundmaic",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.24f))
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 42.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = "Soundmaic",
                color = Color(0xFFFFD76A),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(18.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp),
                color = Color(0xFFFFC642),
                trackColor = Color.White.copy(alpha = 0.22f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (statusVisible && hasInternet) {
                    "تم الاتصال بالإنترنت بنجاح"
                } else if (statusVisible) {
                    "لا يوجد اتصال بالإنترنت — سيتم التشغيل بوضع محدود"
                } else {
                    "جاري الاتصال والتحقق من الخدمة..."
                },
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.alpha(0.96f)
            )
        }
    }
}
