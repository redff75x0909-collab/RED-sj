package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.service.PhoneSmsService

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isNotEmpty()) {
                val service = PhoneSmsService(context)
                val sender = messages[0].originatingAddress ?: "Unknown Sender"
                val bodyBuilder = StringBuilder()
                for (sms in messages) {
                    bodyBuilder.append(sms.messageBody)
                }
                service.handleIncomingSms(sender, bodyBuilder.toString())
            }
        }
    }
}
