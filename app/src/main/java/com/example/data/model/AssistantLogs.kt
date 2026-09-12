package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_logs")
data class CallLogItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phoneNumber: String,
    val contactName: String? = null,
    val callType: String, // "INCOMING", "REJECTED_BUSY", "ANSWERED"
    val timestamp: Long = System.currentTimeMillis(),
    val busyMessageSent: String? = null
)

@Entity(tableName = "sms_logs")
data class SmsLogItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val senderNumber: String,
    val senderName: String? = null,
    val messageBody: String,
    val timestamp: Long = System.currentTimeMillis(),
    val suggestedReply: String? = null,
    val autoReplied: Boolean = false,
    val replySent: String? = null
)
