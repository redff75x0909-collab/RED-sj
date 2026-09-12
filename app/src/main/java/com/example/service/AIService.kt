package com.example.service

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.example.data.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

interface AIService {
    suspend fun generateResponse(
        prompt: String,
        history: List<ChatMessage> = emptyList(),
        imageBitmap: Bitmap? = null,
        language: String = "auto",
        systemInstruction: String? = null,
        customApiKey: String? = null,
        customModel: String? = null
    ): AIResult

    fun detectLanguage(text: String): String
}

data class AIResult(
    val text: String,
    val detectedLanguage: String,
    val isError: Boolean = false,
    val codeSnippet: String? = null,
    val codeLanguage: String? = null
)

class GeminiAIService : AIService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    override fun detectLanguage(text: String): String {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return "en"

        // Script-based Unicode detection
        for (char in trimmed) {
            val code = char.code
            when {
                code in 0x0980..0x09FF -> return "bn" // Bengali / Bangla
                code in 0x0900..0x097F -> return "hi" // Hindi (Devanagari)
                code in 0x0600..0x06FF || code in 0x0750..0x077F -> {
                    // Arabic or Urdu
                    return if (trimmed.contains("ہیں") || trimmed.contains("کیا") || trimmed.contains("کے") || trimmed.contains("ہے")) "ur" else "ar"
                }
                code in 0x4E00..0x9FFF -> return "zh" // Chinese
                code in 0x3040..0x309F || code in 0x30A0..0x30FF -> return "ja" // Japanese
            }
        }

        // Basic Latin lexical detection
        val lower = trimmed.lowercase()
        val words = lower.split("\\s+".toRegex())
        val spanishWords = setOf("hola", "gracias", "por", "favor", "como", "esta", "que", "bueno")
        val frenchWords = setOf("bonjour", "merci", "oui", "comment", "pourquoi", "salut")
        val germanWords = setOf("hallo", "danke", "bitte", "wie", "guten", "morgen", "nicht")

        if (words.any { it in spanishWords }) return "es"
        if (words.any { it in frenchWords }) return "fr"
        if (words.any { it in germanWords }) return "de"

        return "en"
    }

    override suspend fun generateResponse(
        prompt: String,
        history: List<ChatMessage>,
        imageBitmap: Bitmap?,
        language: String,
        systemInstruction: String?,
        customApiKey: String?,
        customModel: String?
    ): AIResult = withContext(Dispatchers.IO) {
        val detectedLang = if (language == "auto") detectLanguage(prompt) else language
        val apiKey = when {
            !customApiKey.isNullOrBlank() -> customApiKey.trim()
            try { BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY" } catch (e: Throwable) { false } -> BuildConfig.GEMINI_API_KEY
            else -> ""
        }

        val model = if (!customModel.isNullOrBlank()) customModel else "gemini-3.5-flash"

        if (apiKey.isEmpty()) {
            return@withContext getOfflineFallbackResponse(prompt, detectedLang)
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

            val rootJson = JSONObject()

            // System instructions
            val langInstruction = getLanguageInstruction(detectedLang)
            val fullSys = buildString {
                append("You are RDC AI, an advanced, capable, and respectful Android personal AI assistant. ")
                append("CRITICAL DIRECTIVE: Never repeat or echo back what the user says. Do NOT begin with 'You asked', 'You said', or quote the user's input. Directly and concisely provide the answer. ")
                append(langInstruction)
                if (!systemInstruction.isNullOrBlank()) {
                    append(" ")
                    append(systemInstruction)
                }
            }

            val sysPart = JSONObject().put("text", fullSys)
            val sysContent = JSONObject().put("parts", JSONArray().put(sysPart))
            rootJson.put("systemInstruction", sysContent)

            // Build contents history
            val contentsArray = JSONArray()

            // Take last 6 turns for context
            val recentTurns = history.takeLast(6)
            for (turn in recentTurns) {
                if (turn.content.isNotBlank()) {
                    val role = if (turn.sender == "user") "user" else "model"
                    val turnObj = JSONObject()
                    turnObj.put("role", role)
                    val turnParts = JSONArray()
                    turnParts.put(JSONObject().put("text", turn.content))
                    turnObj.put("parts", turnParts)
                    contentsArray.put(turnObj)
                }
            }

            // Current user turn
            val currentTurn = JSONObject()
            currentTurn.put("role", "user")
            val currentParts = JSONArray()

            if (prompt.isNotBlank()) {
                currentParts.put(JSONObject().put("text", prompt))
            }

            // Optional Image part (multimodal)
            if (imageBitmap != null) {
                val base64Image = bitmapToBase64(imageBitmap)
                val inlineData = JSONObject()
                inlineData.put("mimeType", "image/jpeg")
                inlineData.put("data", base64Image)
                currentParts.put(JSONObject().put("inlineData", inlineData))
            }

            currentTurn.put("parts", currentParts)
            contentsArray.put(currentTurn)

            rootJson.put("contents", contentsArray)

            // Request config
            val genConfig = JSONObject()
            genConfig.put("temperature", 0.7)
            rootJson.put("generationConfig", genConfig)

            val requestBody = rootJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext AIResult(
                    text = "API Error (${response.code}): ${parseErrorMessage(responseBody)}. Please check your API key in Settings.",
                    detectedLanguage = detectedLang,
                    isError = true
                )
            }

            val parsedJson = JSONObject(responseBody)
            val candidates = parsedJson.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val contentObj = firstCandidate?.optJSONObject("content")
            val partsArray = contentObj?.optJSONArray("parts")

            val responseText = buildString {
                if (partsArray != null) {
                    for (i in 0 until partsArray.length()) {
                        val part = partsArray.optJSONObject(i)
                        append(part?.optString("text", ""))
                    }
                }
            }.trim()

            if (responseText.isEmpty()) {
                return@withContext AIResult(
                    text = "No text was returned by the AI model. Please try rephrasing your request.",
                    detectedLanguage = detectedLang,
                    isError = true
                )
            }

            // Extract code snippet if present
            val (cleanText, snippet, codeLang) = extractCodeBlock(responseText)

            AIResult(
                text = cleanText,
                detectedLanguage = detectedLang,
                codeSnippet = snippet,
                codeLanguage = codeLang
            )
        } catch (e: Exception) {
            AIResult(
                text = "Network error: ${e.localizedMessage ?: "Unable to connect to AI server"}. Check your connection or API key.",
                detectedLanguage = detectedLang,
                isError = true
            )
        }
    }

    private fun getLanguageInstruction(code: String): String {
        return when (code) {
            "bn" -> "Always respond clearly and naturally in Bangla (বাংলা). ব্যবহারকারীর প্রশ্নের শব্দগুলো পুনরাবৃত্তি (repeat) না করে সরাসরি এবং পরিষ্কারভাবে সঠিক উত্তর দিন।"
            "hi" -> "Always respond clearly and naturally in Hindi (हिन्दी). प्रश्न को दोहराए बिना सीधे उत्तर दें।"
            "ar" -> "Always respond clearly and naturally in Arabic (العربية)."
            "ur" -> "Always respond clearly and naturally in Urdu (اردو)."
            "es" -> "Always respond clearly and naturally in Spanish (Español)."
            "fr" -> "Always respond clearly and naturally in French (Français)."
            "de" -> "Always respond clearly and naturally in German (Deutsch)."
            "zh" -> "Always respond clearly and naturally in Simplified Chinese (中文)."
            "ja" -> "Always respond clearly and naturally in Japanese (日本語)."
            else -> "Respond clearly and naturally in English."
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    private fun parseErrorMessage(jsonStr: String): String {
        return try {
            val json = JSONObject(jsonStr)
            val error = json.optJSONObject("error")
            error?.optString("message") ?: "Unknown error"
        } catch (e: Exception) {
            "Invalid response"
        }
    }

    private fun extractCodeBlock(fullText: String): Triple<String, String?, String?> {
        val pattern = "```([a-zA-Z0-9_+-]*)\\n([\\s\\S]*?)```".toRegex()
        val match = pattern.find(fullText) ?: return Triple(fullText, null, null)

        val codeLang = match.groupValues[1].ifBlank { "code" }
        val codeContent = match.groupValues[2].trimEnd()

        return Triple(fullText, codeContent, codeLang)
    }

    private fun getOfflineFallbackResponse(prompt: String, lang: String): AIResult {
        val lower = prompt.lowercase()
        val msg = when {
            lower.contains("hello") || lower.contains("hi") || lower.contains("কেমন আছো") || lower.contains("नमस्ते") -> {
                when (lang) {
                    "bn" -> "হ্যালো! আমি RDC AI, আপনার ব্যক্তিগত সহকারী। একটি Gemini API Key কনফিগার করতে সেটিংস এ যান।"
                    "hi" -> "नमस्ते! मैं RDC AI हूँ, आपका व्यक्तिगत सहायक। AI सेवा सक्रिय करने के लिए सेटिंग्स में API कुंजी दर्ज करें।"
                    "ar" -> "مرحبًا! أنا RDC AI، مساعدك الشخصي. يرجى إدخال مفتاح API في الإعدادات لتفعيل الذكاء الاصطناعي."
                    "ur" -> "السلام علیکم! میں RDC AI ہوں، آپ کا ذاتی معاون۔ مکمل AI سروس کے لیے سیٹنگز میں جائیں۔"
                    "es" -> "¡Hola! Soy RDC AI, tu asistente personal. Configura tu clave de API en Ajustes para activar el motor de IA."
                    "fr" -> "Bonjour ! Je suis RDC AI, votre assistant personnel. Configurez votre clé API dans les Paramètres."
                    "de" -> "Hallo! Ich bin RDC AI, Ihr persönlicher Assistent. Hinterlegen Sie einen API-Schlüssel in den Einstellungen."
                    "zh" -> "您好！我是 RDC AI 个人助手。请在设置中配置 API Key 以开启完整的智能对话。"
                    "ja" -> "こんにちは！私はRDC AIパーソナルアシスタントです。設定でAPIキーを構成してください。"
                    else -> "Hello! I am RDC AI, your personal Android AI assistant. To activate full generative capabilities, please enter your Gemini API Key in Settings."
                }
            }
            lower.contains("screen") || lower.contains("স্ক্রিন") -> {
                "You can ask me to inspect your screen or tap buttons using the Screen Assistant tab. Make sure Accessibility permission is granted in Settings."
            }
            lower.contains("code") || lower.contains("analyzer") -> {
                "You can paste code, upload files, or upload an Android project ZIP in the Code Analyzer tab for instant diagnostic reports."
            }
            else -> {
                "RDC AI is ready. To enable live cloud intelligence, configure your Gemini API key in Settings (or run with GEMINI_API_KEY environment variable). Local commands (Device shortcuts, Screen inspector, Voice TTS, Code diagnostics) are active."
            }
        }

        return AIResult(
            text = msg,
            detectedLanguage = lang,
            isError = false
        )
    }
}
