package com.ivan.wallet.data.sms

import android.content.Context
import android.provider.Telephony
import com.ivan.wallet.data.model.ParsedBankMessage
import com.ivan.wallet.data.parser.AltaBankSmsParser
import com.ivan.wallet.data.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SmsImporter(
    private val repository: TransactionRepository,
    private val parser: AltaBankSmsParser
) {
    suspend fun importInbox(context: Context): ImportSummary = withContext(Dispatchers.IO) {
        val parsedMessages = mutableListOf<ParsedBankMessage>()
        var scanned = 0
        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            projection,
            null,
            null,
            "${Telephony.Sms.DATE} DESC"
        )?.use { cursor ->
            val addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)

            while (cursor.moveToNext()) {
                val sender = cursor.getString(addressIndex).orEmpty()
                if (!sender.contains("Alta_Banka", ignoreCase = true)) continue
                scanned += 1

                val body = cursor.getString(bodyIndex).orEmpty()
                val receivedAt = cursor.getLong(dateIndex)
                parser.parse(sender = sender, body = body, receivedAt = receivedAt)?.let(parsedMessages::add)
            }
        }

        val inserted = repository.importParsedMessages(parsedMessages)
        ImportSummary(
            scanned = scanned,
            parsed = parsedMessages.size,
            inserted = inserted
        )
    }
}

data class ImportSummary(
    val scanned: Int,
    val parsed: Int,
    val inserted: Int
) {
    val duplicates: Int
        get() = (parsed - inserted).coerceAtLeast(0)
}
