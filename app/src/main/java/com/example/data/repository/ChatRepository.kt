package com.example.data.repository

import com.example.data.dao.ChatDao
import com.example.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow

class ChatRepository(private val chatDao: ChatDao) {
    val allMessages: Flow<List<ChatMessage>> = chatDao.getAllMessages()

    suspend fun getRecentMessages(limit: Int = 10): List<ChatMessage> {
        return chatDao.getRecentMessages(limit)
    }

    suspend fun saveMessage(
        sender: String,
        content: String,
        language: String = "en",
        sourceLinks: String? = null,
        codeSnippet: String? = null,
        codeLanguage: String? = null,
        imageUri: String? = null,
        isError: Boolean = false
    ): Long {
        val msg = ChatMessage(
            sender = sender,
            content = content,
            language = language,
            timestamp = System.currentTimeMillis(),
            sourceLinks = sourceLinks,
            codeSnippet = codeSnippet,
            codeLanguage = codeLanguage,
            imageUri = imageUri,
            isError = isError
        )
        return chatDao.insertMessage(msg)
    }

    suspend fun deleteMessage(id: Long) {
        chatDao.deleteMessageById(id)
    }

    suspend fun clearHistory() {
        chatDao.clearAll()
    }
}
