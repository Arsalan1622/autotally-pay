package com.kakar.autotallypay.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.kakar.autotallypay.data.AppDatabase
import com.kakar.autotallypay.data.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * FULL flavor only. Receives incoming SMS, groups multi-part messages
 * by sender, and hands each complete message to the Repository.
 * Fake senders are stored flagged and never counted as paid.
 */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val pending = goAsync()
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope.launch {
            try {
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                // Group parts of one multi-part SMS by sender, in order.
                val grouped = linkedMapOf<String, StringBuilder>()
                for (msg in messages) {
                    val sender = msg.originatingAddress ?: continue
                    val body = msg.messageBody ?: continue
                    grouped.getOrPut(sender) { StringBuilder() }.append(body)
                }
                val db = AppDatabase.get(context)
                val repo = Repository(db)
                for ((sender, body) in grouped) {
                    repo.processSms(sender, body.toString())
                }
            } catch (_: Exception) {
                // Never crash the broadcast; the message stays in the inbox
                // and the user can paste it from the SMS Inbox screen.
            } finally {
                pending.finish()
            }
        }
    }
}
