package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.FloatingScreenAssistantBubble
import com.example.ui.screens.CodeAnalyzerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PhoneSmsScreen
import com.example.ui.screens.PrivacyScreen
import com.example.ui.screens.ScreenAssistantScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WebSearchScreen
import com.example.ui.theme.RdcTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RdcTheme {
                val viewModel: MainViewModel = viewModel()
                var currentScreen by remember { mutableStateOf("home") }

                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .imePadding()
                    ) { paddingValues ->
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "screen_transition",
                            modifier = Modifier.fillMaxSize()
                        ) { screen ->
                            when (screen) {
                                "home" -> HomeScreen(
                                    viewModel = viewModel,
                                    onNavigateToCodeAnalyzer = { currentScreen = "code_analyzer" },
                                    onNavigateToScreenAssistant = { currentScreen = "screen_assistant" },
                                    onNavigateToWebSearch = { currentScreen = "web_search" },
                                    onNavigateToPhoneSms = { currentScreen = "phone_sms" },
                                    onNavigateToSettings = { currentScreen = "settings" },
                                    onNavigateToPrivacy = { currentScreen = "privacy" }
                                )
                                "code_analyzer" -> CodeAnalyzerScreen(
                                    viewModel = viewModel,
                                    onBack = { currentScreen = "home" }
                                )
                                "screen_assistant" -> ScreenAssistantScreen(
                                    viewModel = viewModel,
                                    onBack = { currentScreen = "home" }
                                )
                                "web_search" -> WebSearchScreen(
                                    viewModel = viewModel,
                                    onBack = { currentScreen = "home" }
                                )
                                "phone_sms" -> PhoneSmsScreen(
                                    viewModel = viewModel,
                                    onBack = { currentScreen = "home" }
                                )
                                "settings" -> SettingsScreen(
                                    viewModel = viewModel,
                                    onBack = { currentScreen = "home" },
                                    onNavigateToPrivacy = { currentScreen = "privacy" }
                                )
                                "privacy" -> PrivacyScreen(
                                    onBack = { currentScreen = "home" }
                                )
                            }
                        }
                    }

                    // Interactive Floating Dotted Assistant Bubble (Movable, pulsing during speech)
                    FloatingScreenAssistantBubble(
                        viewModel = viewModel,
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                    )
                }
            }
        }
    }
}
