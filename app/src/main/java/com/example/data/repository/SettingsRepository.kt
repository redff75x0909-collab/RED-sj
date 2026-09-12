package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
    val language: String = "auto", // "auto", "en", "bn", "hi", "ar", "ur", "es", "fr", "de", "zh", "ja"
    val voiceGender: String = "female", // "female", "male"
    val speechSpeed: Float = 1.0f,
    val autoSpeak: Boolean = true,
    val wakeWordEnabled: Boolean = false,
    val busyMode: Boolean = false,
    val busyMessage: String = "I am busy right now. Please leave a message.",
    val autoReplySms: Boolean = false,
    val customApiKey: String = "",
    val customModel: String = "gemini-3.5-flash",
    val customEndpoint: String = "",
    val floatingBubbleEnabled: Boolean = true
)

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("rdc_ai_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettings(): AppSettings {
        return AppSettings(
            language = prefs.getString("language", "auto") ?: "auto",
            voiceGender = prefs.getString("voice_gender", "female") ?: "female",
            speechSpeed = prefs.getFloat("speech_speed", 1.0f),
            autoSpeak = prefs.getBoolean("auto_speak", true),
            wakeWordEnabled = prefs.getBoolean("wake_word", false),
            busyMode = prefs.getBoolean("busy_mode", false),
            busyMessage = prefs.getString("busy_message", "I am busy right now. Please leave a message.") ?: "I am busy right now. Please leave a message.",
            autoReplySms = prefs.getBoolean("auto_reply_sms", false),
            customApiKey = prefs.getString("custom_api_key", "") ?: "",
            customModel = prefs.getString("custom_model", "gemini-3.5-flash") ?: "gemini-3.5-flash",
            customEndpoint = prefs.getString("custom_endpoint", "") ?: "",
            floatingBubbleEnabled = prefs.getBoolean("floating_bubble", true)
        )
    }

    fun updateFloatingBubble(enabled: Boolean) {
        prefs.edit().putBoolean("floating_bubble", enabled).apply()
        _settings.value = _settings.value.copy(floatingBubbleEnabled = enabled)
    }

    fun updateLanguage(language: String) {
        prefs.edit().putString("language", language).apply()
        _settings.value = _settings.value.copy(language = language)
    }

    fun updateVoiceGender(gender: String) {
        prefs.edit().putString("voice_gender", gender).apply()
        _settings.value = _settings.value.copy(voiceGender = gender)
    }

    fun updateSpeechSpeed(speed: Float) {
        prefs.edit().putFloat("speech_speed", speed).apply()
        _settings.value = _settings.value.copy(speechSpeed = speed)
    }

    fun updateAutoSpeak(enabled: Boolean) {
        prefs.edit().putBoolean("auto_speak", enabled).apply()
        _settings.value = _settings.value.copy(autoSpeak = enabled)
    }

    fun updateWakeWord(enabled: Boolean) {
        prefs.edit().putBoolean("wake_word", enabled).apply()
        _settings.value = _settings.value.copy(wakeWordEnabled = enabled)
    }

    fun updateBusyMode(enabled: Boolean) {
        prefs.edit().putBoolean("busy_mode", enabled).apply()
        _settings.value = _settings.value.copy(busyMode = enabled)
    }

    fun updateBusyMessage(message: String) {
        prefs.edit().putString("busy_message", message).apply()
        _settings.value = _settings.value.copy(busyMessage = message)
    }

    fun updateAutoReplySms(enabled: Boolean) {
        prefs.edit().putBoolean("auto_reply_sms", enabled).apply()
        _settings.value = _settings.value.copy(autoReplySms = enabled)
    }

    fun updateCustomApiKey(key: String) {
        prefs.edit().putString("custom_api_key", key).apply()
        _settings.value = _settings.value.copy(customApiKey = key)
    }

    fun updateCustomModel(model: String) {
        prefs.edit().putString("custom_model", model).apply()
        _settings.value = _settings.value.copy(customModel = model)
    }

    fun updateCustomEndpoint(endpoint: String) {
        prefs.edit().putString("custom_endpoint", endpoint).apply()
        _settings.value = _settings.value.copy(customEndpoint = endpoint)
    }

    // Direct synchronous reads for Background Receivers and Services
    fun isBusyModeSync(): Boolean = prefs.getBoolean("busy_mode", false)
    fun getBusyMessageSync(): String = prefs.getString("busy_message", "I am busy right now. Please leave a message.") ?: "I am busy right now. Please leave a message."
    fun isAutoReplySmsSync(): Boolean = prefs.getBoolean("auto_reply_sms", false)
}
