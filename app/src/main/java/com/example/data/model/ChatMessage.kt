package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String, // "user", "assistant", "system"
    val content: String,
    val language: String = "en",
    val timestamp: Long = System.currentTimeMillis(),
    val sourceLinks: String? = null, // JSON or formatted links for web search
    val codeSnippet: String? = null,
    val codeLanguage: String? = null,
    val imageUri: String? = null,
    val isError: Boolean = false
)
