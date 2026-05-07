package com.ivan.wallet.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val amountSymbols = DecimalFormatSymbols(Locale.US).apply {
    decimalSeparator = '.'
    groupingSeparator = ','
}

private val amountFormatter = DecimalFormat("#,##0.00", amountSymbols)
private val timestampFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")

fun Long.toMoney(currency: String): String {
    val major = this / 100.0
    return "${amountFormatter.format(major)} $currency"
}

fun Long.toDateTime(): String {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .format(timestampFormatter)
}
