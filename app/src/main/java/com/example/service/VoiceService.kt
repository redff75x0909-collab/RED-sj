package com.example.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

interface VoiceService {
    val isListening: StateFlow<Boolean>
    val recognizedSpeech: StateFlow<String>
    val isSpeaking: StateFlow<Boolean>

    fun startListening(languageCode: String = "en", onResult: (String) -> Unit)
    fun stopListening()
    fun speak(text: String, languageCode: String = "en", gender: String = "female", speed: Float = 1.0f)
    fun stopSpeaking()
    fun release()
}

class AndroidVoiceService(private val context: Context) : VoiceService {

    private val _isListening = MutableStateFlow(false)
    override val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _recognizedSpeech = MutableStateFlow("")
    override val recognizedSpeech: StateFlow<String> = _recognizedSpeech.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    override val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false
    private var pendingSpeechText: String? = null

    init {
        initTts()
    }

    private fun initTts() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsInitialized = true
                textToSpeech?.language = Locale.ENGLISH
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        _isSpeaking.value = false
                    }
                })
                pendingSpeechText?.let { text ->
                    speak(text)
                    pendingSpeechText = null
                }
            }
        }
    }

    override fun startListening(languageCode: String, onResult: (String) -> Unit) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _recognizedSpeech.value = "Speech recognition is not available on this device."
            return
        }

        stopListening()
        _recognizedSpeech.value = ""

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                    }

                    override fun onBeginningOfSpeech() {}

                    override fun onRmsChanged(rmsdB: Float) {}

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _isListening.value = false
                    }

                    override fun onError(error: Int) {
                        _isListening.value = false
                        val errorMsg = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized."
                            SpeechRecognizer.ERROR_NETWORK -> "Network error during speech recognition."
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                            else -> "Recognition error ($error)."
                        }
                        if (_recognizedSpeech.value.isEmpty()) {
                            _recognizedSpeech.value = errorMsg
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            _recognizedSpeech.value = text
                            onResult(text)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            _recognizedSpeech.value = text
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, getLocaleForLanguage(languageCode).toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _isListening.value = false
            _recognizedSpeech.value = "Speech recognition start failed: ${e.message}"
        }
    }

    override fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            // Ignore cleanup exceptions
        }
        speechRecognizer = null
        _isListening.value = false
    }

    override fun speak(text: String, languageCode: String, gender: String, speed: Float) {
        if (!isTtsInitialized) {
            pendingSpeechText = text
            initTts()
            return
        }

        val tts = textToSpeech ?: return

        try {
            val locale = getLocaleForLanguage(languageCode)
            val langResult = tts.setLanguage(locale)
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.language = Locale.ENGLISH
            }

            // Adjust pitch: slightly higher (1.15f) for clear female voice clarity
            val pitch = if (gender.lowercase() == "female") 1.15f else 0.95f
            tts.setPitch(pitch)
            tts.setSpeechRate(speed.coerceIn(0.5f, 2.0f))

            // Select gender voice if available
            configureVoice(gender, locale)

            // Remove markdown, URLs, code blocks and formatting characters for crisp, natural human-like speech
            val cleanText = text
                .replace("```[a-zA-Z0-9_+-]*[\\s\\S]*?```".toRegex(), " Code block omitted. ")
                .replace("`[^`]+`".toRegex(), "")
                .replace("https?://\\S+".toRegex(), " link ")
                .replace("(?m)^\\s*[-*#>]+\\s*".toRegex(), "")
                .replace("[#*_~`>\\[\\](){}]".toRegex(), " ")
                .replace("\\s+".toRegex(), " ")
                .trim()
                .take(600) // Prevent excessively long reading

            if (cleanText.isNotBlank()) {
                _isSpeaking.value = true
                tts.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "RDC_TTS_ID")
            }
        } catch (e: Exception) {
            _isSpeaking.value = false
        }
    }

    private fun configureVoice(gender: String, locale: Locale) {
        val tts = textToSpeech ?: return
        try {
            val voices = tts.voices ?: return
            val targetVoice = voices.firstOrNull { voice ->
                val name = voice.name.lowercase()
                val matchLocale = voice.locale.language.equals(locale.language, ignoreCase = true)
                val matchGender = if (gender.lowercase() == "female") {
                    name.contains("female") || name.contains("woman") || name.contains("fem") || 
                    name.contains("-f-") || name.contains("zira") || name.contains("ban-network") ||
                    name.contains("sfg") || name.contains("f00")
                } else {
                    name.contains("male") && !name.contains("female")
                }
                matchLocale && matchGender
            } ?: voices.firstOrNull { voice ->
                val name = voice.name.lowercase()
                if (gender.lowercase() == "female") {
                    name.contains("female") || name.contains("woman") || name.contains("-f-")
                } else {
                    name.contains("male") && !name.contains("female")
                }
            } ?: voices.firstOrNull { it.locale.language.equals(locale.language, ignoreCase = true) }

            if (targetVoice != null) {
                tts.voice = targetVoice
            }
        } catch (e: Exception) {
            // Ignore voice selection failure and continue with default
        }
    }

    override fun stopSpeaking() {
        try {
            textToSpeech?.stop()
        } catch (e: Exception) {
            // Ignore
        }
        _isSpeaking.value = false
    }

    override fun release() {
        stopListening()
        stopSpeaking()
        textToSpeech?.shutdown()
        textToSpeech = null
    }

    private fun getLocaleForLanguage(code: String): Locale {
        return when (code) {
            "bn" -> Locale("bn", "BD")
            "hi" -> Locale("hi", "IN")
            "ar" -> Locale("ar", "SA")
            "ur" -> Locale("ur", "PK")
            "es" -> Locale("es", "ES")
            "fr" -> Locale.FRENCH
            "de" -> Locale.GERMAN
            "zh" -> Locale.CHINESE
            "ja" -> Locale.JAPANESE
            else -> Locale.ENGLISH
        }
    }
}
