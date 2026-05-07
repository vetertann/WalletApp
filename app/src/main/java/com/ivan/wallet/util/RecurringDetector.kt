package com.ivan.wallet.util

import com.ivan.wallet.ui.TransactionUiModel
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs

data class RecurringMatch(
    val merchant: String,
    val normalizedMerchant: String,
    val averageAmountMinor: Long,
    val currency: String,
    val cadenceLabel: String,
    val occurrences: Int,
    val lastOccurrenceMs: Long
)

object RecurringDetector {
    private const val MIN_OCCURRENCES = 3
    private const val DAY_MS = 24L * 60L * 60L * 1000L

    fun detect(transactions: List<TransactionUiModel>): List<RecurringMatch> {
        val byKey = transactions
            .filter { it.amountMinor > 0 }
            .groupBy { it.normalizedMerchant to it.currency }

        return byKey.mapNotNull { (key, txs) ->
            if (txs.size < MIN_OCCURRENCES) return@mapNotNull null

            val sorted = txs.sortedBy { it.occurredAt }
            val gaps = sorted.zipWithNext { a, b -> (b.occurredAt - a.occurredAt) / DAY_MS }
            if (gaps.isEmpty()) return@mapNotNull null

            val avgGap = gaps.average()
            val variance = gaps.map { abs(it - avgGap) }.average()
            if (variance > avgGap * 0.4) return@mapNotNull null

            val cadence = when {
                avgGap in 5.0..9.0 -> "Weekly"
                avgGap in 12.0..16.0 -> "Bi-weekly"
                avgGap in 25.0..35.0 -> "Monthly"
                avgGap in 85.0..95.0 -> "Quarterly"
                avgGap in 350.0..380.0 -> "Yearly"
                else -> return@mapNotNull null
            }

            val avgAmount = sorted.sumOf { it.amountMinor } / sorted.size
            RecurringMatch(
                merchant = sorted.last().merchant,
                normalizedMerchant = key.first,
                averageAmountMinor = avgAmount,
                currency = key.second,
                cadenceLabel = cadence,
                occurrences = sorted.size,
                lastOccurrenceMs = sorted.last().occurredAt
            )
        }.sortedByDescending { it.lastOccurrenceMs }
    }

    fun isFresh(match: RecurringMatch, nowMs: Long): Boolean {
        val days = (nowMs - match.lastOccurrenceMs) / DAY_MS
        return days <= 90
    }

    fun lastSeen(match: RecurringMatch, nowMs: Long): String {
        val days = (nowMs - match.lastOccurrenceMs) / DAY_MS
        return when {
            days < 1 -> "today"
            days < 2 -> "yesterday"
            days < 30 -> "${days}d ago"
            else -> Instant.ofEpochMilli(match.lastOccurrenceMs)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .toString()
        }
    }
}
