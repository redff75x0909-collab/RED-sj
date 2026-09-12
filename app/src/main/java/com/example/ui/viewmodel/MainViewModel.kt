package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.RdcApplication
import com.example.data.model.CallLogItem
import com.example.data.model.ChatMessage
import com.example.data.model.SmsLogItem
import com.example.data.repository.AppSettings
import com.example.service.CodeAnalysisReport
import com.example.service.DeviceControlHelper
import com.example.service.RdcAccessibilityService
import com.example.service.ScreenSnapshot
import com.example.service.WebSearchResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as RdcApplication

    // State flows
    val messages: StateFlow<List<ChatMessage>> = app.chatRepository.allMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<AppSettings> = app.settingsRepository.settings

    val callLogs: StateFlow<List<CallLogItem>> = app.database.assistantDao().getAllCallLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val smsLogs: StateFlow<List<SmsLogItem>> = app.database.assistantDao().getAllSmsLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isVoiceListening = app.voiceService.isListening
    val recognizedSpeech = app.voiceService.recognizedSpeech
    val isVoiceSpeaking = app.voiceService.isSpeaking
    val isAccessibilityConnected = RdcAccessibilityService.connectedFlow

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentStatusMessage = MutableStateFlow("Ready")
    val currentStatusMessage: StateFlow<String> = _currentStatusMessage.asStateFlow()

    // Code Analyzer state
    private val _codeReport = MutableStateFlow<CodeAnalysisReport?>(null)
    val codeReport: StateFlow<CodeAnalysisReport?> = _codeReport.asStateFlow()

    private val _isAnalyzingCode = MutableStateFlow(false)
    val isAnalyzingCode: StateFlow<Boolean> = _isAnalyzingCode.asStateFlow()

    // Web Search state
    private val _lastSearchResponse = MutableStateFlow<WebSearchResponse?>(null)
    val lastSearchResponse: StateFlow<WebSearchResponse?> = _lastSearchResponse.asStateFlow()

    // Selected image for vision analysis
    private val _selectedImageBitmap = MutableStateFlow<Bitmap?>(null)
    val selectedImageBitmap: StateFlow<Bitmap?> = _selectedImageBitmap.asStateFlow()

    fun setSelectedImage(bitmap: Bitmap?) {
        _selectedImageBitmap.value = bitmap
    }

    fun setSelectedImageFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                @Suppress("DEPRECATION")
                val bitmap = MediaStore.Images.Media.getBitmap(app.contentResolver, uri)
                _selectedImageBitmap.value = bitmap
            } catch (e: Exception) {
                _currentStatusMessage.value = "Image load error: ${e.message}"
            }
        }
    }

    fun sendMessage(text: String, attachedImage: Bitmap? = _selectedImageBitmap.value) {
        if (text.isBlank() && attachedImage == null) return

        val prompt = text.trim()
        val currentSettings = settings.value

        viewModelScope.launch {
            // Save User message
            app.chatRepository.saveMessage(
                sender = "user",
                content = prompt,
                language = currentSettings.language
            )

            _isLoading.value = true
            _currentStatusMessage.value = "Processing..."

            // 1. Check for local app launching commands ("open youtube", "open calculator", etc.)
            if (prompt.lowercase().startsWith("open ") || prompt.lowercase().startsWith("launch ")) {
                val launched = DeviceControlHelper.launchAppByName(app, prompt)
                if (launched) {
                    val reply = "Launching application: ${prompt.replace("(?i)^(open|launch)\\s+".toRegex(), "")}."
                    app.chatRepository.saveMessage(sender = "assistant", content = reply)
                    speakIfNeeded(reply, currentSettings)
                    _isLoading.value = false
                    _currentStatusMessage.value = "Ready"
                    return@launch
                }
            }

            // 2. Check for Accessibility screen commands ("read my screen", "what is on my screen", "scroll down", "tap ...")
            val screenService = RdcAccessibilityService.instance
            val lower = prompt.lowercase()
            if (lower.contains("read my screen") || lower.contains("what is on my screen") || lower.contains("screen summary") || lower.contains("আমার স্ক্রিন পড়")) {
                val description = screenService?.describeScreen()
                    ?: "Screen Assistant is currently inactive. Please enable Accessibility permission in Settings -> Screen Assistant."
                app.chatRepository.saveMessage(sender = "assistant", content = description)
                speakIfNeeded(description, currentSettings)
                _isLoading.value = false
                _currentStatusMessage.value = "Ready"
                return@launch
            }

            if (lower.startsWith("tap ") || lower.startsWith("click ")) {
                val target = prompt.substringAfter(" ").trim()
                val clicked = screenService?.tapElementWithText(target) ?: false
                val reply = if (clicked) "Tapped on \"$target\"." else "Could not locate interactive element \"$target\" on screen."
                app.chatRepository.saveMessage(sender = "assistant", content = reply)
                speakIfNeeded(reply, currentSettings)
                _isLoading.value = false
                _currentStatusMessage.value = "Ready"
                return@launch
            }

            if (lower == "scroll down" || lower == "scroll forward") {
                val scrolled = screenService?.scroll(forward = true) ?: false
                val reply = if (scrolled) "Scrolled down." else "Could not scroll current screen."
                app.chatRepository.saveMessage(sender = "assistant", content = reply)
                _isLoading.value = false
                _currentStatusMessage.value = "Ready"
                return@launch
            }

            if (lower == "scroll up" || lower == "scroll back") {
                val scrolled = screenService?.scroll(forward = false) ?: false
                val reply = if (scrolled) "Scrolled up." else "Could not scroll current screen."
                app.chatRepository.saveMessage(sender = "assistant", content = reply)
                _isLoading.value = false
                _currentStatusMessage.value = "Ready"
                return@launch
            }

            // 3. Check for Web Search trigger
            if (app.webSearchService.shouldTriggerSearch(prompt)) {
                _currentStatusMessage.value = "Searching live web..."
                val searchResult = app.webSearchService.searchWeb(prompt, app.aiService)
                _lastSearchResponse.value = searchResult

                // Save search response
                app.chatRepository.saveMessage(
                    sender = "assistant",
                    content = searchResult.summary,
                    sourceLinks = searchResult.sources.joinToString("\n") { "${it.title}: ${it.url}" }
                )
                speakIfNeeded(searchResult.summary, currentSettings)
                _isLoading.value = false
                _currentStatusMessage.value = "Ready"
                return@launch
            }

            // 4. Regular AI conversation (or vision understanding)
            _currentStatusMessage.value = "Thinking..."
            val recentTurns = app.chatRepository.getRecentMessages(6).reversed()

            val result = app.aiService.generateResponse(
                prompt = prompt,
                history = recentTurns,
                imageBitmap = attachedImage,
                language = currentSettings.language,
                customApiKey = currentSettings.customApiKey,
                customModel = currentSettings.customModel
            )

            app.chatRepository.saveMessage(
                sender = "assistant",
                content = result.text,
                language = result.detectedLanguage,
                codeSnippet = result.codeSnippet,
                codeLanguage = result.codeLanguage,
                isError = result.isError
            )

            // Clear attached image after sending
            _selectedImageBitmap.value = null

            if (!result.isError) {
                speakIfNeeded(result.text, currentSettings, result.detectedLanguage)
            }

            _isLoading.value = false
            _currentStatusMessage.value = "Ready"
        }
    }

    private fun speakIfNeeded(text: String, settings: AppSettings, languageCode: String = "en") {
        if (settings.autoSpeak) {
            val targetLang = if (settings.language != "auto") settings.language else languageCode
            app.voiceService.speak(
                text = text,
                languageCode = targetLang,
                gender = settings.voiceGender,
                speed = settings.speechSpeed
            )
        }
    }

    fun startVoiceInput() {
        val lang = settings.value.language
        app.voiceService.startListening(lang) { recognizedText ->
            sendMessage(recognizedText)
        }
    }

    fun stopVoiceInput() {
        app.voiceService.stopListening()
    }

    fun stopSpeaking() {
        app.voiceService.stopSpeaking()
    }

    // Code Analyzer Actions
    fun analyzeCodeSnippet(code: String, fileName: String = "snippet.kt") {
        viewModelScope.launch {
            _isAnalyzingCode.value = true
            val report = app.codeAnalysisService.analyzeCode(code, fileName, null, app.aiService)
            _codeReport.value = report
            _isAnalyzingCode.value = false
        }
    }

    fun analyzeZipFile(inputStream: InputStream) {
        viewModelScope.launch {
            _isAnalyzingCode.value = true
            val report = app.codeAnalysisService.analyzeZipProject(inputStream, app.aiService)
            _codeReport.value = report
            _isAnalyzingCode.value = false
        }
    }

    fun fixCurrentCode(code: String, onResult: (String) -> Unit) {
        val report = _codeReport.value ?: return
        viewModelScope.launch {
            _isAnalyzingCode.value = true
            val fixed = app.codeAnalysisService.fixCode(code, report.problems, report.language, app.aiService)
            _codeReport.value = _codeReport.value?.copy(fixedCode = fixed)
            _isAnalyzingCode.value = false
            onResult(fixed)
        }
    }

    // Web Search Direct Action
    fun executeWebSearch(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = app.webSearchService.searchWeb(query, app.aiService)
            _lastSearchResponse.value = result
            _isLoading.value = false
        }
    }

    // Real-time Screen Analysis state
    private val _screenSnapshot = MutableStateFlow<ScreenSnapshot?>(null)
    val screenSnapshot: StateFlow<ScreenSnapshot?> = _screenSnapshot.asStateFlow()

    private val _screenSummary = MutableStateFlow("")
    val screenSummary: StateFlow<String> = _screenSummary.asStateFlow()

    private val _isScreenAnalysisActive = MutableStateFlow(false)
    val isScreenAnalysisActive: StateFlow<Boolean> = _isScreenAnalysisActive.asStateFlow()

    private val _floatingBubbleDismissed = MutableStateFlow(false)
    val isFloatingBubbleVisible: StateFlow<Boolean> = combine(
        settings,
        _floatingBubbleDismissed
    ) { set, dismissed ->
        set.floatingBubbleEnabled && !dismissed
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun showFloatingBubble() {
        _floatingBubbleDismissed.value = false
    }

    fun hideFloatingBubble() {
        _floatingBubbleDismissed.value = true
    }

    fun updateFloatingBubbleSetting(enabled: Boolean) {
        app.settingsRepository.updateFloatingBubble(enabled)
        if (enabled) _floatingBubbleDismissed.value = false
    }

    fun analyzeCurrentScreen(speakDescription: Boolean = false) {
        val screenService = RdcAccessibilityService.instance
        if (screenService == null || !RdcAccessibilityService.connectedFlow.value) {
            _screenSummary.value = "Accessibility permission is required to analyze screen. Please enable RDC AI Screen Assistant in Accessibility settings."
            _currentStatusMessage.value = "Accessibility not connected"
            return
        }

        val snapshot = screenService.getActiveScreenSnapshot()
        _screenSnapshot.value = snapshot

        if (snapshot != null) {
            _isScreenAnalysisActive.value = true
            val desc = screenService.describeScreen()
            _screenSummary.value = desc
            _currentStatusMessage.value = "Screen analyzed successfully"

            if (speakDescription && settings.value.autoSpeak) {
                val lang = if (settings.value.language != "auto") settings.value.language else "bn"
                val speechText = if (lang == "bn") {
                    "স্ক্রিন বিশ্লেষণ সম্পন্ন হয়েছে। বর্তমান স্ক্রিনে ${snapshot.elements.size} টি উপাদান সক্রিয় রয়েছে।"
                } else {
                    "Screen analysis complete. ${snapshot.elements.size} UI elements detected."
                }
                app.voiceService.speak(
                    text = speechText,
                    languageCode = lang,
                    gender = settings.value.voiceGender,
                    speed = settings.value.speechSpeed
                )
            }
        } else {
            _screenSummary.value = "Could not capture active window. Please ensure an application screen is open."
        }
    }

    fun startVoiceFromFloatingBubble() {
        analyzeCurrentScreen(speakDescription = false)
        val lang = settings.value.language
        app.voiceService.startListening(lang) { spokenText ->
            handleFloatingVoiceQuery(spokenText)
        }
    }

    private fun handleFloatingVoiceQuery(spokenText: String) {
        if (spokenText.isBlank()) return
        val currentSettings = settings.value
        viewModelScope.launch {
            _isLoading.value = true
            _currentStatusMessage.value = "Processing voice..."

            // User message is logged directly - no echoing
            app.chatRepository.saveMessage(sender = "user", content = spokenText, language = currentSettings.language)

            val snapshot = _screenSnapshot.value ?: RdcAccessibilityService.instance?.getActiveScreenSnapshot()
            val screenContextPrompt = if (snapshot != null && snapshot.fullText.isNotBlank()) {
                "User query: $spokenText\n\nActive Screen Snapshot Context:\nApp: ${snapshot.packageName ?: "Unknown"}\nScreen Text: ${snapshot.fullText.take(500)}\nInteractive Elements: ${snapshot.elements.filter { it.isClickable }.joinToString { it.text.ifBlank { it.contentDescription ?: "" } }.take(300)}"
            } else {
                spokenText
            }

            val result = app.aiService.generateResponse(
                prompt = screenContextPrompt,
                history = app.chatRepository.getRecentMessages(4).reversed(),
                language = currentSettings.language,
                systemInstruction = "CRITICAL DIRECTIVE: Do NOT repeat the user's words or quote the question. Answer directly, concisely and clearly in 1-3 sentences so it can be comfortably spoken in a clear female voice.",
                customApiKey = currentSettings.customApiKey,
                customModel = currentSettings.customModel
            )

            app.chatRepository.saveMessage(
                sender = "assistant",
                content = result.text,
                language = result.detectedLanguage,
                isError = result.isError
            )

            if (!result.isError) {
                val targetLang = if (currentSettings.language != "auto") currentSettings.language else result.detectedLanguage
                app.voiceService.speak(
                    text = result.text,
                    languageCode = targetLang,
                    gender = currentSettings.voiceGender,
                    speed = currentSettings.speechSpeed
                )
            }

            _isLoading.value = false
            _currentStatusMessage.value = "Ready"
        }
    }

    // Settings modifiers
    fun updateLanguage(lang: String) = app.settingsRepository.updateLanguage(lang)
    fun updateVoiceGender(gender: String) = app.settingsRepository.updateVoiceGender(gender)
    fun updateSpeechSpeed(speed: Float) = app.settingsRepository.updateSpeechSpeed(speed)
    fun updateAutoSpeak(enabled: Boolean) = app.settingsRepository.updateAutoSpeak(enabled)
    fun updateWakeWord(enabled: Boolean) = app.settingsRepository.updateWakeWord(enabled)
    fun updateBusyMode(enabled: Boolean) = app.settingsRepository.updateBusyMode(enabled)
    fun updateBusyMessage(msg: String) = app.settingsRepository.updateBusyMessage(msg)
    fun updateAutoReplySms(enabled: Boolean) = app.settingsRepository.updateAutoReplySms(enabled)
    fun updateCustomApiKey(key: String) = app.settingsRepository.updateCustomApiKey(key)
    fun updateCustomModel(model: String) = app.settingsRepository.updateCustomModel(model)

    fun clearChat() {
        viewModelScope.launch {
            app.chatRepository.clearHistory()
        }
    }
}
