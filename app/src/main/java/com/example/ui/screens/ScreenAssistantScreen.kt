package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartScreen
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.RdcAccessibilityService
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenAssistantScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val isConnected by viewModel.isAccessibilityConnected.collectAsState()
    val isBubbleVisible by viewModel.isFloatingBubbleVisible.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val screenSummary by viewModel.screenSummary.collectAsState()
    val context = LocalContext.current

    var screenDescription by remember { mutableStateOf("") }
    var actionFeedback by remember { mutableStateOf("") }

    // When screen analysis / accessibility becomes connected, instantly inspect screen
    LaunchedEffect(isConnected) {
        if (isConnected) {
            viewModel.analyzeCurrentScreen(speakDescription = false)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Column {
                    Text("Screen Assistant", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Accessibility screen reader & control", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
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
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Card
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isConnected) Color(0xFF005143) else MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isConnected) Color(0xFF00D4B2) else Color(0xFFD29922)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isConnected) Color(0xFF00D4B2) else Color(0xFFD29922),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isConnected) "Screen Assistant Connected" else "Accessibility Permission Required",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isConnected) Color(0xFFE6FFF9) else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isConnected) "Ready to read screen content, navigate UI, and execute user commands."
                                else "Enable 'RDC AI Screen Assistant' under Accessibility settings to activate.",
                                fontSize = 12.sp,
                                color = if (isConnected) Color(0xFFB2DFDB) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (!isConnected) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D4B2), contentColor = Color(0xFF00382E))
                            ) {
                                Text("Enable", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Interactive Floating Assistant Bubble (Movable, Dotted, Step Pulsing)
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF161B22),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isBubbleVisible) Color(0xFF00D4B2) else Color(0xFF30363D)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00382E)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color(0xFF00D4B2), modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "ভাসমান সহকারী (Floating AI Bubble)",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "গুল আকৃতির ফুটা ফুটা বাবল • ধাপ ধাপ পালস",
                                        fontSize = 11.sp,
                                        color = Color(0xFF8B949E)
                                    )
                                }
                            }

                            Switch(
                                checked = isBubbleVisible,
                                onCheckedChange = { enabled ->
                                    viewModel.updateFloatingBubbleSetting(enabled)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF00D4B2),
                                    checkedTrackColor = Color(0xFF005143),
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = Color(0xFF21262D)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "এটি চালু করার পর মোবাইলের স্ক্রিনের ওপর একটি গোল আকৃতির ফুটা ফুটা বাবল দেখা যাবে। এটি যেকোনো জায়গায় নড়ানো যাবে। কথা বলার সময় এটি ধাপ ধাপ করে পালস দিবে এবং কোনো কথা রিপিট না করে সরাসরি পরিষ্কার ফিমেল কণ্ঠে উত্তর দিবে।",
                            fontSize = 12.sp,
                            color = Color(0xFFC9D1D9),
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Test Clear Female Voice button
                            Button(
                                onClick = {
                                    viewModel.sendMessage("আমি আরডিসি এআই। আমি আপনার স্ক্রিন দেখতে পারি এবং কোনো কথা পুনরাবৃত্তি না করে পরিষ্কার ফিমেল কণ্ঠে উত্তর দেই।")
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005143), contentColor = Color(0xFFE6FFF9))
                            ) {
                                Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ফিমেল কণ্ঠ শুনুন", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // Direct Screen Analysis button
                            Button(
                                onClick = {
                                    viewModel.analyzeCurrentScreen(speakDescription = true)
                                    actionFeedback = "স্ক্রিন বিশ্লেষণ সম্পন্ন হয়েছে।"
                                },
                                enabled = isConnected,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D4B2), contentColor = Color(0xFF00382E))
                            ) {
                                Icon(Icons.Default.SmartScreen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("স্ক্রিন এনালাইসিস", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Quick Commands Section
            item {
                Text(
                    text = "Quick Assistant Commands",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val desc = RdcAccessibilityService.instance?.describeScreen() ?: "Accessibility service not active."
                                screenDescription = desc
                                actionFeedback = "Screen content read."
                            },
                            enabled = isConnected,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
                        ) {
                            Icon(Icons.Default.SmartScreen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Read Screen", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val success = RdcAccessibilityService.instance?.scroll(forward = true) ?: false
                                actionFeedback = if (success) "Scrolled down successfully." else "Cannot scroll."
                            },
                            enabled = isConnected,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scroll Down", fontSize = 12.sp)
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val success = RdcAccessibilityService.instance?.scroll(forward = false) ?: false
                                actionFeedback = if (success) "Scrolled up successfully." else "Cannot scroll."
                            },
                            enabled = isConnected,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
                        ) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scroll Up", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val success = RdcAccessibilityService.instance?.performBack() ?: false
                                actionFeedback = if (success) "Performed Back action." else "Failed to navigate back."
                            },
                            enabled = isConnected,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
                        ) {
                            Text("Back Action", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Action Feedback Pill
            if (actionFeedback.isNotBlank()) {
                item {
                    Surface(
                        color = Color(0xFF00D4B2).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Status: $actionFeedback",
                            fontSize = 12.sp,
                            color = Color(0xFF00D4B2),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }

            // Screen Inspection Results
            if (screenDescription.isNotBlank()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Screen Understanding Result:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00D4B2))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(screenDescription, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            // Live Visible Elements
            item {
                val snapshot = RdcAccessibilityService.instance?.getActiveScreenSnapshot()
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Visible UI Elements", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.weight(1f))
                        Text("${snapshot?.elements?.size ?: 0} nodes detected", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (snapshot == null || snapshot.elements.isEmpty()) {
                        Text("No elements inspected. Ensure Accessibility is enabled and open another app to control it.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            snapshot.elements.take(12).forEach { elem ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF161B22),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(elem.text.ifBlank { elem.contentDescription ?: "<unnamed view>" }, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium)
                                            Text("Type: ${elem.className?.substringAfterLast(".") ?: "View"} • Clickable: ${elem.isClickable}", fontSize = 10.sp, color = Color(0xFF8B949E))
                                        }
                                        if (elem.isClickable) {
                                            Button(
                                                onClick = {
                                                    val clicked = RdcAccessibilityService.instance?.tapElementWithText(elem.text) ?: false
                                                    actionFeedback = if (clicked) "Tapped: ${elem.text}" else "Tap failed"
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D4B2), contentColor = Color(0xFF00382E)),
                                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text("Tap", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Safety and Privacy Guarantee
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF161B22),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF00D4B2), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Safety & Privacy Guarantee", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "• RDC AI only accesses screen data when you request it.\n" +
                                    "• Password, credential, and banking fields are strictly skipped and ignored.\n" +
                                    "• RDC AI does not bypass security locks, CAPTCHAs, biometric authentication, or DRM.\n" +
                                    "• All actions run locally on your Android device.",
                            fontSize = 11.sp,
                            color = Color(0xFF8B949E),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}
