package com.ivan.wallet.data.parser

import com.ivan.wallet.data.model.ParsedBankMessage
import java.math.RoundingMode
import java.security.MessageDigest
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class AltaBankSmsParser {

    // Card payment SMS:
    // "Placanje VISA karticom **9715: iznos 460.00RSD, mesto DOO CENTRAL PAR,
    //  dana 01.05.2026 u 12:09:42h. Rasp.: RSD 70,348.84. Vasa ALTA banka"
    private val cardPattern = Regex(
        pattern = """Placanje\s+VISA\s+karticom\s+\*+\d+:\s+iznos\s+([\d.,]+)\s*([A-Z]{3}),\s+mesto\s+(.+?)\s*,\s+dana\s+(\d{2}\.\d{2}\.\d{4})\s+u\s+(\d{2}:\d{2}:\d{2})h\.\s+Rasp\.:\s+([A-Z]{3})\s+([\d.,]+?)\.\s+Vasa\s+ALTA\s+banka""",
        option = RegexOption.IGNORE_CASE
    )

    // Inflow / credit SMS:
    // "Proknjizen je priliv na vas racun 0001000241543 u iznosu od 108,572.10 RSD, 02.02.2026"
    private val inflowPattern = Regex(
        pattern = """Proknjizen\s+je\s+priliv\s+na\s+vas\s+racun\s+(\d+)\s+u\s+iznosu\s+od\s+([\d.,]+)\s*([A-Z]{3}),\s+(\d{2}\.\d{2}\.\d{4})""",
        option = RegexOption.IGNORE_CASE
    )

    // Generic outflow SMS (bank fees, transfers, anything not a card payment):
    // "Odliv sa racuna: 0001000241543 u iznosu od: 330.00 RSD, dana: 30.04.2026.
    //  Raspolozivo stanje po odlivu 71,748.84 RSD. Vasa Alta banka"
    private val outflowPattern = Regex(
        pattern = """Odliv\s+sa\s+racuna:\s+(\d+)\s+u\s+iznosu\s+od:\s+([\d.,]+)\s*([A-Z]{3}),\s+dana:\s+(\d{2}\.\d{2}\.\d{4})\.\s+Raspolozivo\s+stanje\s+po\s+odlivu\s+([\d.,]+)\s*([A-Z]{3})""",
        option = RegexOption.IGNORE_CASE
    )

    private val cardDateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
    private val dayOnlyFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    fun parse(
        sender: String,
        body: String,
        receivedAt: Long
    ): ParsedBankMessage? {
        return parseCardPayment(sender, body, receivedAt)
            ?: parseInflow(sender, body, receivedAt)
            ?: parseOutflow(sender, body, receivedAt)
    }

    private fun parseCardPayment(
        sender: String,
        body: String,
        receivedAt: Long
    ): ParsedBankMessage? {
        val match = cardPattern.find(body) ?: return null
        val amountMinor = parseMinorUnits(match.groupValues[1]) ?: return null
        val currency = match.groupValues[2].uppercase(Locale.ROOT)
        val rawMerchant = cleanMerchant(match.groupValues[3])
        val normalizedMerchant = normalizeMerchant(rawMerchant)
        val timestamp = LocalDateTime
            .parse("${match.groupValues[4]} ${match.groupValues[5]}", cardDateFormatter)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val balanceCurrency = match.groupValues[6].uppercase(Locale.ROOT)
        val balanceMinor = parseMinorUnits(match.groupValues[7])
        val externalId = digest(
            "$sender|card|$currency|$amountMinor|$normalizedMerchant|$timestamp"
        )

        return ParsedBankMessage(
            externalId = externalId,
            sender = sender,
            rawBody = body,
            amountMinor = amountMinor,
            currency = currency,
            merchantDisplay = rawMerchant,
            normalizedMerchant = normalizedMerchant,
            occurredAt = timestamp,
            balanceMinor = balanceMinor,
            balanceCurrency = balanceCurrency,
            receivedAt = receivedAt
        )
    }

    private fun parseInflow(
        sender: String,
        body: String,
        receivedAt: Long
    ): ParsedBankMessage? {
        val match = inflowPattern.find(body) ?: return null
        val account = match.groupValues[1]
        val amountAbs = parseMinorUnits(match.groupValues[2]) ?: return null
        val currency = match.groupValues[3].uppercase(Locale.ROOT)
        val date = LocalDate.parse(match.groupValues[4], dayOnlyFormatter)
        val occurredAt = combineDateWithReceivedTime(date, receivedAt)
        val merchantDisplay = "Bank inflow"
        val normalizedMerchant = "BANK INFLOW"

        // Negative amount → counts toward Income tile.
        val amountSigned = -amountAbs
        // External id includes receivedAt because the SMS body has only date
        // precision; otherwise two same-day same-amount inflows would collide.
        val externalId = digest(
            "$sender|inflow|$account|$currency|$amountAbs|$occurredAt|$receivedAt"
        )

        return ParsedBankMessage(
            externalId = externalId,
            sender = sender,
            rawBody = body,
            amountMinor = amountSigned,
            currency = currency,
            merchantDisplay = merchantDisplay,
            normalizedMerchant = normalizedMerchant,
            occurredAt = occurredAt,
            balanceMinor = null,
            balanceCurrency = null,
            receivedAt = receivedAt
        )
    }

    private fun parseOutflow(
        sender: String,
        body: String,
        receivedAt: Long
    ): ParsedBankMessage? {
        val match = outflowPattern.find(body) ?: return null
        val account = match.groupValues[1]
        val amountMinor = parseMinorUnits(match.groupValues[2]) ?: return null
        val currency = match.groupValues[3].uppercase(Locale.ROOT)
        val date = LocalDate.parse(match.groupValues[4], dayOnlyFormatter)
        val balanceMinor = parseMinorUnits(match.groupValues[5])
        val balanceCurrency = match.groupValues[6].uppercase(Locale.ROOT)
        val occurredAt = combineDateWithReceivedTime(date, receivedAt)
        val merchantDisplay = "Bank outflow"
        val normalizedMerchant = "BANK OUTFLOW"

        val externalId = digest(
            "$sender|outflow|$account|$currency|$amountMinor|$occurredAt|$receivedAt"
        )

        return ParsedBankMessage(
            externalId = externalId,
            sender = sender,
            rawBody = body,
            amountMinor = amountMinor,
            currency = currency,
            merchantDisplay = merchantDisplay,
            normalizedMerchant = normalizedMerchant,
            occurredAt = occurredAt,
            balanceMinor = balanceMinor,
            balanceCurrency = balanceCurrency,
            receivedAt = receivedAt
        )
    }

    fun normalizeMerchant(merchant: String): String {
        val cleaned = merchant
            .uppercase(Locale.ROOT)
            .replace(Regex("[^A-Z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        val tokens = cleaned.split(" ")
            .filter { token -> token.isNotBlank() }
            .filterNot { token -> token.all(Char::isDigit) }
            .filterNot { token -> token in noiseTokens }

        return if (tokens.isEmpty()) cleaned else tokens.take(3).joinToString(" ")
    }

    private fun cleanMerchant(value: String): String {
        return value
            .replace(">", "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun parseMinorUnits(value: String): Long? {
        val normalized = when {
            value.contains(',') && value.contains('.') -> value.replace(",", "")
            value.contains(',') -> value.replace(",", ".")
            else -> value
        }

        return normalized.toBigDecimalOrNull()
            ?.setScale(2, RoundingMode.HALF_UP)
            ?.movePointRight(2)
            ?.longValueExact()
    }

    /**
     * The inflow/outflow SMS bodies only carry a date (no time). To keep occurredAt
     * unique per SMS within a day, combine the parsed date with the time-of-day from
     * the device receivedAt timestamp. Bank notifications typically arrive within
     * seconds of the booking, so this is a close proxy.
     */
    private fun combineDateWithReceivedTime(date: LocalDate, receivedAt: Long): Long {
        val zone = ZoneId.systemDefault()
        val time: LocalTime = if (receivedAt > 0L) {
            java.time.Instant.ofEpochMilli(receivedAt).atZone(zone).toLocalTime()
        } else {
            LocalTime.NOON
        }
        return LocalDateTime.of(date, time)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
    }

    private fun digest(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private companion object {
        private val noiseTokens = setOf(
            "MESTO",
            "SHOP",
            "STORE",
            "POS",
            "ONLINE"
        )
    }
}
