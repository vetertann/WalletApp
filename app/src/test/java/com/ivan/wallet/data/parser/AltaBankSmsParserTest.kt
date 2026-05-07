package com.ivan.wallet.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class AltaBankSmsParserTest {
    private val parser = AltaBankSmsParser()

    @Test
    fun parsesDomesticRsdPayment() {
        val sms = "Placanje VISA karticom **9715: iznos 686.96RSD, mesto 213 - MAXI 208>, dana 01.05.2026 u 19:55:43h. Rasp.: RSD 69,453.88. Vasa ALTA banka"

        val parsed = parser.parse(
            sender = "Alta_Banka",
            body = sms,
            receivedAt = 1_777_777_777_000
        )

        assertNotNull(parsed)
        parsed!!
        assertEquals(68_696L, parsed.amountMinor)
        assertEquals("RSD", parsed.currency)
        assertEquals("213 - MAXI 208", parsed.merchantDisplay)
        assertEquals("MAXI", parsed.normalizedMerchant)
        assertEquals("RSD", parsed.balanceCurrency)
        assertEquals(6_945_388L, parsed.balanceMinor)
        val dateTime = Instant.ofEpochMilli(parsed.occurredAt).atZone(ZoneId.systemDefault()).toLocalDateTime()
        assertEquals(2026, dateTime.year)
        assertEquals(5, dateTime.monthValue)
        assertEquals(1, dateTime.dayOfMonth)
        assertEquals(19, dateTime.hour)
        assertEquals(55, dateTime.minute)
    }

    @Test
    fun parsesInflowAsNegativeAmount() {
        val sms = "Proknjizen je priliv na vas racun 0001000241543 u iznosu od 108,572.10 RSD, 02.02.2026"

        val parsed = parser.parse(
            sender = "Alta_Banka",
            body = sms,
            receivedAt = 1_777_777_777_000
        )

        assertNotNull(parsed)
        parsed!!
        assertEquals(-10_857_210L, parsed.amountMinor)
        assertEquals("RSD", parsed.currency)
        assertEquals("Bank inflow", parsed.merchantDisplay)
        assertEquals("BANK INFLOW", parsed.normalizedMerchant)
        val date = Instant.ofEpochMilli(parsed.occurredAt).atZone(ZoneId.systemDefault()).toLocalDate()
        assertEquals(2026, date.year)
        assertEquals(2, date.monthValue)
        assertEquals(2, date.dayOfMonth)
    }

    @Test
    fun parsesGenericOutflowAsExpenseWithBalance() {
        val sms = "Odliv sa racuna: 0001000241543 u iznosu od: 330.00 RSD, dana: 30.04.2026. Raspolozivo stanje po odlivu 71,748.84 RSD. Vasa Alta banka"

        val parsed = parser.parse(
            sender = "Alta_Banka",
            body = sms,
            receivedAt = 1_777_777_777_000
        )

        assertNotNull(parsed)
        parsed!!
        assertEquals(33_000L, parsed.amountMinor)
        assertEquals("RSD", parsed.currency)
        assertEquals("Bank outflow", parsed.merchantDisplay)
        assertEquals("BANK OUTFLOW", parsed.normalizedMerchant)
        assertEquals("RSD", parsed.balanceCurrency)
        assertEquals(7_174_884L, parsed.balanceMinor)
        val date = Instant.ofEpochMilli(parsed.occurredAt).atZone(ZoneId.systemDefault()).toLocalDate()
        assertEquals(2026, date.year)
        assertEquals(4, date.monthValue)
        assertEquals(30, date.dayOfMonth)
    }

    @Test
    fun parsesForeignCurrencyPayment() {
        val sms = "Placanje VISA karticom **9715: iznos 5.00USD, mesto PYTHONANYWHERE>, dana 28.04.2026 u 14:52:22h. Rasp.: EUR 3,422.77. Vasa ALTA banka."

        val parsed = parser.parse(
            sender = "Alta_Banka",
            body = sms,
            receivedAt = 1_777_700_000_000
        )

        assertNotNull(parsed)
        parsed!!
        assertEquals(500L, parsed.amountMinor)
        assertEquals("USD", parsed.currency)
        assertEquals("PYTHONANYWHERE", parsed.merchantDisplay)
        assertEquals("PYTHONANYWHERE", parsed.normalizedMerchant)
        assertEquals("EUR", parsed.balanceCurrency)
        assertEquals(342_277L, parsed.balanceMinor)
    }
}
