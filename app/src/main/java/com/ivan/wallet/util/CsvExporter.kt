package com.ivan.wallet.util

import android.content.Context
import android.net.Uri
import com.ivan.wallet.ui.TransactionUiModel
import java.io.OutputStreamWriter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object CsvExporter {
    private val timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun export(context: Context, uri: Uri, transactions: List<TransactionUiModel>): Result<Int> {
        return runCatching {
            context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
                OutputStreamWriter(stream, Charsets.UTF_8).use { writer ->
                    writer.appendLine("date,merchant,category,amount_major,currency,source,balance_major,balance_currency")
                    transactions.forEach { tx ->
                        val date = Instant.ofEpochMilli(tx.occurredAt)
                            .atZone(ZoneId.systemDefault())
                            .format(timestampFormatter)
                        val amount = "%.2f".format(tx.amountMinor / 100.0)
                        val balance = tx.balanceMinor?.let { "%.2f".format(it / 100.0) } ?: ""
                        val source = if (tx.isManual) "manual" else "bank"
                        writer.appendLine(
                            listOf(
                                date,
                                csvEscape(tx.merchant),
                                tx.category?.label ?: "",
                                amount,
                                tx.currency,
                                source,
                                balance,
                                tx.balanceCurrency ?: ""
                            ).joinToString(",")
                        )
                    }
                }
                transactions.size
            } ?: throw IllegalStateException("Could not open output stream")
        }
    }

    private fun csvEscape(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }
}
