package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartScreen
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToPrivacy: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val isAccessibilityConnected by viewModel.isAccessibilityConnected.collectAsState()
    val context = LocalContext.current

    var apiKeyText by remember { mutableStateOf(settings.customApiKey) }
    var isApiKeyVisible by remember { mutableStateOf(false) }

    var languageExpanded by remember { mutableStateOf(false) }
    var modelExpanded by remember { mutableStateOf(false) }

    val languages = listOf(
        "auto" to "Auto-Detect Language",
        "bn" to "Bangla (বাংলা)",
        "en" to "English",
        "hi" to "Hindi (हिन्दी)",
        "ar" to "Arabic (العربية)",
        "ur" to "Urdu (اردو)",
        "es" to "Spanish (Español)",
        "fr" to "French (Français)",
        "de" to "German (Deutsch)",
        "zh" to "Chinese (中文)",
        "ja" to "Japanese (日本語)"
    )

    val models = listOf(
        "gemini-3.5-flash" to "Gemini 3.5 Flash (Fast, Recommended)",
        "gemini-3.1-pro-preview" to "Gemini 3.1 Pro Preview (Complex Reasoning)"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Text("Settings", fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- AI Engine & API Key ---
            item {
                Text("AI Engine Configuration", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00D4B2))
            }

            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Gemini API Key", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Configurable for personal use. Automatically defaults to environment key when available.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = apiKeyText,
                            onValueChange = {
                                apiKeyText = it
                                viewModel.updateCustomApiKey(it)
                            },
                            placeholder = { Text("Enter custom Gemini API key...") },
                            visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                    Icon(
                                        imageVector = if (isApiKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle Key Visibility"
                                    )
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00D4B2)
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Model Selector
                        Text("Model Selection", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))

                        ExposedDropdownMenuBox(
                            expanded = modelExpanded,
                            onExpandedChange = { modelExpanded = !modelExpanded }
                        ) {
                            OutlinedTextField(
                                value = models.firstOrNull { it.first == settings.customModel }?.second ?: settings.customModel,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = modelExpanded,
                                onDismissRequest = { modelExpanded = false }
                            ) {
                                models.forEach { (key, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label, fontSize = 13.sp) },
                                        onClick = {
                                            viewModel.updateCustomModel(key)
                                            modelExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- Language Settings ---
            item {
                Text("Language & Region", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00D4B2))
            }

            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Primary Assistant Language", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Supports Bangla, English, Hindi, Arabic, Urdu, Spanish, French, German, Chinese, and Japanese.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        ExposedDropdownMenuBox(
                            expanded = languageExpanded,
                            onExpandedChange = { languageExpanded = !languageExpanded }
                        ) {
                            OutlinedTextField(
                                value = languages.firstOrNull { it.first == settings.language }?.second ?: settings.language,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = languageExpanded,
                                onDismissRequest = { languageExpanded = false }
                            ) {
                                languages.forEach { (code, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label, fontSize = 13.sp) },
                                        onClick = {
                                            viewModel.updateLanguage(code)
                                            languageExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- Voice Assistant Settings ---
            item {
                Text("Voice Assistant Options", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00D4B2))
            }

            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Voice Gender Selection
                        Text("Voice Gender Option", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { viewModel.updateVoiceGender("female") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (settings.voiceGender == "female") Color(0xFF00D4B2) else MaterialTheme.colorScheme.surface,
                                    contentColor = if (settings.voiceGender == "female") Color(0xFF00382E) else MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Text("Female Voice", fontSize = 12.sp)
                            }
                            Button(
                                onClick = { viewModel.updateVoiceGender("male") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (settings.voiceGender == "male") Color(0xFF00D4B2) else MaterialTheme.colorScheme.surface,
                                    contentColor = if (settings.voiceGender == "male") Color(0xFF00382E) else MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Text("Male Voice", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Speech rate slider
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Speech Speed: ", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(String.format(java.util.Locale.US, "%.1fx", settings.speechSpeed), fontSize = 13.sp, color = Color(0xFF00D4B2))
                        }
                        Slider(
                            value = settings.speechSpeed,
                            onValueChange = { viewModel.updateSpeechSpeed(it) },
                            valueRange = 0.5f..2.0f,
                            steps = 5,
                            colors = SliderDefaults.colors(thumbColor = Color(0xFF00D4B2), activeTrackColor = Color(0xFF00D4B2))
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Auto-speak toggle
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Auto-Speak Responses", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("Automatically read out AI replies with Text-to-Speech", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = settings.autoSpeak,
                                onCheckedChange = { viewModel.updateAutoSpeak(it) }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Wake-word toggle
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Wake Word (\"Hey RDC\")", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("Voice trigger subject to Android microphone limits", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = settings.wakeWordEnabled,
                                onCheckedChange = { viewModel.updateWakeWord(it) }
                            )
                        }
                    }
                }
            }

            // --- System Permissions & Services ---
            item {
                Text("Permissions & System Integrations", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00D4B2))
            }

            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SmartScreen, contentDescription = null, tint = if (isAccessibilityConnected) Color(0xFF00D4B2) else Color(0xFFFA7970))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Screen Assistant Accessibility", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(if (isAccessibilityConnected) "Enabled and active" else "Disabled in Android Settings", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Button(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface)
                            ) {
                                Text("Configure", fontSize = 11.sp)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF388BFD))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Device App Permissions", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("Manage Microphone, Phone, and SMS permissions", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Button(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = android.net.Uri.fromParts("package", context.packageName, null)
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface)
                            ) {
                                Text("Permissions", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // --- Privacy & Security Link ---
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToPrivacy() }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PrivacyTip, contentDescription = null, tint = Color(0xFF00D4B2))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Privacy & Security Architecture", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Learn how your data, screen info, and credentials are kept safe", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
