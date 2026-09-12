package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartScreen
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Text("Security & Privacy", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF005143),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00D4B2)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF00D4B2), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Personal Use & Safety Architecture", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "RDC AI is designed exclusively as a personal Android assistant with user-first privacy standards. No tracking, monetization, analytics, or background surveillance.",
                            fontSize = 12.sp,
                            color = Color(0xFFE6FFF9),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            item {
                PrivacyPolicyItem(
                    icon = Icons.Default.Key,
                    title = "API Key Protection",
                    description = "Your Gemini API keys are never hardcoded in binary releases. They are injected via secure build environment configurations or stored strictly inside Android's private app-isolated SharedPreferences."
                )
            }

            item {
                PrivacyPolicyItem(
                    icon = Icons.Default.SmartScreen,
                    title = "Screen Assistant Disclosures",
                    description = "Screen reading relies on standard Android Accessibility APIs and requires your explicit system authorization. Password fields and credential inputs are strictly skipped. RDC AI never silently observes or logs your screen in the background."
                )
            }

            item {
                PrivacyPolicyItem(
                    icon = Icons.Default.Lock,
                    title = "Security Safeguards & Boundaries",
                    description = "RDC AI will never attempt to bypass screen locks, device passwords, banking app security layers, CAPTCHAs, biometric authentication, or DRM-protected content."
                )
            }

            item {
                PrivacyPolicyItem(
                    icon = Icons.Default.Mic,
                    title = "Microphone & Voice Privacy",
                    description = "The microphone is activated only when you press the microphone button or enable the wake word. Audio is streamed to Android's built-in SpeechRecognizer or processed locally; no voice recordings are retained."
                )
            }

            item {
                PrivacyPolicyItem(
                    icon = Icons.Default.Phone,
                    title = "Phone & SMS Automation",
                    description = "Incoming call detection, Busy Mode rejections, and SMS replies operate strictly with user-granted permissions (READ_PHONE_STATE, ANSWER_PHONE_CALLS, SEND_SMS). Auto-replies require explicit user activation."
                )
            }

            item {
                PrivacyPolicyItem(
                    icon = Icons.Default.Storage,
                    title = "Local On-Device Database",
                    description = "Chat history, call logs, and SMS assistant logs are kept locally in a private on-device Room database. You have full control to clear history at any time with a single tap."
                )
            }
        }
    }
}

@Composable
fun PrivacyPolicyItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFF00D4B2).copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color(0xFF00D4B2), modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(4.dp))
                Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
            }
        }
    }
}
