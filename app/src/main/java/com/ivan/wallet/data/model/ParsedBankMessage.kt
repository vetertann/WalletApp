package com.ivan.wallet.data.model

data class ParsedBankMessage(
    val externalId: String,
    val sender: String,
    val rawBody: String,
    val amountMinor: Long,
    val currency: String,
    val merchantDisplay: String,
    val normalizedMerchant: String,
    val occurredAt: Long,
    val balanceMinor: Long?,
    val balanceCurrency: String?,
    val receivedAt: Long
)
