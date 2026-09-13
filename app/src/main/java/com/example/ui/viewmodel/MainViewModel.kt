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

    // Credit & Device Eligibility State
    val remainingCreditMs: StateFlow<Long> = app.creditManager.remainingCreditMs
    val cooldownRemainingMs: StateFlow<Long> = app.creditManager.cooldownRemainingMs
    val isCreditActive: StateFlow<Boolean> = app.creditManager.isCreditActive
    val deviceSpecs = app.deviceEligibilityManager.deviceSpecs

    fun toggleTestingBypass(enable: Boolean) {
        app.deviceEligibilityManager.toggleTestingBypass(enable)
    }

    fun formatDuration(ms: Long): String = app.creditManager.formatDuration(ms)
    fun formatDurationBengali(ms: Long): String = app.creditManager.formatDurationBengali(ms)

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
            app.creditManager.setSessionActive(true)

            // 0. Check Device Eligibility (Vivo 4GB RAM requirement)
            if (!app.deviceEligibilityManager.isDeviceAllowed()) {
                val specs = app.deviceEligibilityManager.deviceSpecs.value
                val reply = "⚠️ ডিভাইস সীমাবদ্ধতা: এই অ্যাপটি শুধুমাত্র Vivo 4GB RAM স্মার্টফোনের জন্য অনুমোদিত। আপনার বর্তমান ডিভাইস: ${specs.manufacturer} ${specs.model} (${String.format(java.util.Locale.US, "%.1f", specs.totalRamGb)} GB RAM)। আপনি হোমস্ক্রিন থেকে ডেমো/টেস্ট মোড অন করতে পারেন।"
                app.chatRepository.saveMessage(sender = "assistant", content = reply, isError = true)
                speakIfNeeded(reply, currentSettings, "bn")
                _isLoading.value = false
                _currentStatusMessage.value = "Device verification required"
                return@launch
            }

            // 0.1 Check Credit Limit (2 hours active credit, 6 hours cooldown)
            if (!app.creditManager.isCreditAvailable()) {
                val cooldownBengali = app.creditManager.formatDurationBengali(app.creditManager.cooldownRemainingMs.value)
                val reply = "আপনার ২ ঘণ্টার ব্যবহারের লিমিট শেষ হয়েছে। ১ ক্রেডিট শেষ না হওয়া পর্যন্ত অন্য ক্রেডিট যুক্ত হবে না। ৬ ঘণ্টা পর পরবর্তী ক্রেডিট যোগ হবে। বাকি সময়: $cooldownBengali।"
                app.chatRepository.saveMessage(sender = "assistant", content = reply, isError = true)
                speakIfNeeded(reply, currentSettings, "bn")
                _isLoading.value = false
                _currentStatusMessage.value = "Credit in cooldown"
                return@launch
            }

            // 1. Check for URL / Web Link opening ("কোন লিংকের ভিতর ঢুকতে পারবে")
            val extractedUrl = DeviceControlHelper.extractUrl(prompt)
            val lower = prompt.lowercase()
            if (extractedUrl != null || lower.contains("লিংক") || lower.contains("লিংকে") || lower.contains("ওয়েবসাইট") || lower.contains("website") || lower.contains("open link")) {
                if (extractedUrl != null) {
                    val opened = DeviceControlHelper.openUrl(app, extractedUrl)
                    val reply = if (opened) "জি, লিংকটি ব্রাউজারে ওপেন করা হয়েছে: $extractedUrl" else "লিংকটি ওপেন করা যায়নি।"
                    app.chatRepository.saveMessage(sender = "assistant", content = reply)
                    speakIfNeeded(reply, currentSettings, "bn")
                    _isLoading.value = false
                    _currentStatusMessage.value = "Ready"
                    return@launch
                }
            }

            // 2. Check for App Launching ("মোবাইলের যতগুলো অ্যাপ আছে বললে ওপেন করতে পারবে নাম বলে দিলেই")
            if (DeviceControlHelper.isAppLaunchCommand(prompt)) {
                val launchResult = DeviceControlHelper.launchAppByName(app, prompt)
                if (launchResult.success) {
                    val reply = launchResult.message
                    app.chatRepository.saveMessage(sender = "assistant", content = reply)
                    speakIfNeeded(reply, currentSettings, "bn")
                    _isLoading.value = false
                    _currentStatusMessage.value = "Ready"
                    return@launch
                }
            }

            // 3. Check for Accessibility screen commands ("read my screen", "what is on my screen", "scroll down", "tap ...")
            val screenService = RdcAccessibilityService.instance
            if (lower.contains("read my screen") || lower.contains("what is on my screen") || lower.contains("screen summary") || lower.contains("আমার স্ক্রিন পড়") || lower.contains("স্ক্রিন দেখ")) {
                val description = screenService?.describeScreen()
                    ?: "Screen Assistant is currently inactive. Please enable Accessibility permission in Settings -> Screen Assistant."
                app.chatRepository.saveMessage(sender = "assistant", content = description)
                speakIfNeeded(description, currentSettings)
                _isLoading.value = false
                _currentStatusMessage.value = "Ready"
                return@launch
            }

            if (lower.startsWith("tap ") || lower.startsWith("click ") || lower.contains("ক্লিক করো") || lower.contains("ট্যাপ করো")) {
                val target = prompt.replace("(?i)^(tap|click)\\s+".toRegex(), "")
                    .replace("ক্লিক করো", "")
                    .replace("ট্যাপ করো", "")
                    .trim()
                val clicked = screenService?.tapElementWithText(target) ?: false
                val reply = if (clicked) "স্ক্রিনে \"$target\" ওপেন/ক্লিক করা হয়েছে।" else "স্ক্রিনে \"$target\" বাটন বা লিংকটি খুঁজে পাওয়া যায়নি।"
                app.chatRepository.saveMessage(sender = "assistant", content = reply)
                speakIfNeeded(reply, currentSettings, "bn")
                _isLoading.value = false
                _currentStatusMessage.value = "Ready"
                return@launch
            }

            if (lower == "scroll down" || lower == "scroll forward" || lower.contains("নিচে নামাও")) {
                val scrolled = screenService?.scroll(forward = true) ?: false
                val reply = if (scrolled) "স্ক্রিন নিচে স্ক্রোল করা হয়েছে।" else "স্ক্রিন স্ক্রোল করা যায়নি।"
                app.chatRepository.saveMessage(sender = "assistant", content = reply)
                _isLoading.value = false
                _currentStatusMessage.value = "Ready"
                return@launch
            }

            if (lower == "scroll up" || lower == "scroll back" || lower.contains("উপরে তোলো")) {
                val scrolled = screenService?.scroll(forward = false) ?: false
                val reply = if (scrolled) "স্ক্রিন উপরে স্ক্রোল করা হয়েছে।" else "স্ক্রিন স্ক্রোল করা যায়নি।"
                app.chatRepository.saveMessage(sender = "assistant", content = reply)
                _isLoading.value = false
                _currentStatusMessage.value = "Ready"
                return@launch
            }

            // 4. Check for Web Search trigger
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

            // 5. Regular AI conversation (or vision understanding)
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
        sendMessage(spokenText)
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
