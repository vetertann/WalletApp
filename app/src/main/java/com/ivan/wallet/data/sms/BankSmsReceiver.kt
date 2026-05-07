package com.ivan.wallet.data.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.ivan.wallet.WalletApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BankSmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val container = WalletApplication.container(context)
        val parser = container.bankParser
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: emptyArray()
        val bodyByOriginating = messages
            .groupBy { it.originatingAddress.orEmpty() }
            .mapValues { entry -> entry.value.joinToString("") { it.messageBody.orEmpty() } }

        val parsed = bodyByOriginating.mapNotNull { (sender, body) ->
            if (!sender.contains("Alta_Banka", ignoreCase = true)) return@mapNotNull null
            parser.parse(sender = sender, body = body, receivedAt = System.currentTimeMillis())
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                if (parsed.isNotEmpty()) {
                    container.transactionRepository.importParsedMessages(parsed)
                } else {
                    container.smsImporter.importInbox(context)
                }
            }
            pendingResult.finish()
        }
    }
}
