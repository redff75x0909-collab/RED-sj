package com.example.service

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.telecom.TelecomManager
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.example.data.AppDatabase
import com.example.data.model.CallLogItem
import com.example.data.model.SmsLogItem
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PhoneSmsService(private val context: Context) {
    private val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
    private val database = AppDatabase.getInstance(context)
    private val settingsRepo = SettingsRepository(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    fun resolveContactName(phoneNumber: String): String? {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return null
        }
        return try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
            val cursor = context.contentResolver.query(uri, projection, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    it.getString(it.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun handleIncomingCall(number: String) {
        scope.launch {
            val contactName = resolveContactName(number)
            val isBusy = settingsRepo.isBusyModeSync()
            val busyMsg = settingsRepo.getBusyMessageSync()

            if (isBusy) {
                // In busy mode, log call and reject if permission granted
                rejectCall()
                database.assistantDao().insertCallLog(
                    CallLogItem(
                        phoneNumber = number,
                        contactName = contactName,
                        callType = "REJECTED_BUSY",
                        busyMessageSent = busyMsg
                    )
                )
            } else {
                database.assistantDao().insertCallLog(
                    CallLogItem(
                        phoneNumber = number,
                        contactName = contactName,
                        callType = "INCOMING"
                    )
                )
            }
        }
    }

    fun answerCall(): Boolean {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ANSWER_PHONE_CALLS) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                telecomManager?.acceptRingingCall()
                true
            } else {
                false
            }
        } catch (e: SecurityException) {
            false
        }
    }

    fun rejectCall(): Boolean {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ANSWER_PHONE_CALLS) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                telecomManager?.endCall() ?: false
            } else {
                false
            }
        } catch (e: SecurityException) {
            false
        }
    }

    fun handleIncomingSms(sender: String, messageBody: String) {
        scope.launch {
            val contactName = resolveContactName(sender)
            val isAutoReply = settingsRepo.isAutoReplySmsSync()
            val busyMsg = settingsRepo.getBusyMessageSync()

            // AI suggested reply generator
            val suggestedReply = generateSuggestedReply(messageBody)

            var autoSent = false
            var replySentText: String? = null

            if (isAutoReply) {
                val sent = sendSms(sender, busyMsg)
                if (sent) {
                    autoSent = true
                    replySentText = busyMsg
                }
            }

            database.assistantDao().insertSmsLog(
                SmsLogItem(
                    senderNumber = sender,
                    senderName = contactName,
                    messageBody = messageBody,
                    suggestedReply = suggestedReply,
                    autoReplied = autoSent,
                    replySent = replySentText
                )
            )
        }
    }

    fun sendSms(destinationAddress: String, text: String): Boolean {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        return try {
            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            smsManager.sendTextMessage(destinationAddress, null, text, null, null)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun generateSuggestedReply(incomingText: String): String {
        val lower = incomingText.lowercase()
        return when {
            lower.contains("where are you") || lower.contains("kothay") -> "I'm busy right now. I'll reply later."
            lower.contains("call me") -> "I can't talk right now. Please text me."
            lower.contains("urgent") -> "Received your message. Looking into it immediately."
            lower.contains("thanks") || lower.contains("dhonnobad") -> "You're welcome!"
            else -> "Thank you for your message. I will get back to you shortly."
        }
    }
}
