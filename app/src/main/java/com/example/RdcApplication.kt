package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.repository.ChatRepository
import com.example.data.repository.SettingsRepository
import com.example.service.AndroidVoiceService
import com.example.service.DefaultCodeAnalysisService
import com.example.service.DuckDuckGoSearchService
import com.example.service.GeminiAIService
import com.example.service.PhoneSmsService
import com.example.service.VoiceService

class RdcApplication : Application() {
    lateinit var database: AppDatabase
        private set
    lateinit var chatRepository: ChatRepository
        private set
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var aiService: GeminiAIService
        private set
    lateinit var webSearchService: DuckDuckGoSearchService
        private set
    lateinit var codeAnalysisService: DefaultCodeAnalysisService
        private set
    lateinit var voiceService: VoiceService
        private set
    lateinit var phoneSmsService: PhoneSmsService
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        chatRepository = ChatRepository(database.chatDao())
        settingsRepository = SettingsRepository(this)
        aiService = GeminiAIService()
        webSearchService = DuckDuckGoSearchService()
        codeAnalysisService = DefaultCodeAnalysisService()
        voiceService = AndroidVoiceService(this)
        phoneSmsService = PhoneSmsService(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        voiceService.release()
    }
}
