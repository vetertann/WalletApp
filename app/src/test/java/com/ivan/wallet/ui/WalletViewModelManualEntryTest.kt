package com.ivan.wallet.ui

import com.ivan.wallet.data.model.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class WalletViewModelManualEntryTest {
    @Test
    fun buildsValidManualEntryDraft() {
        val result = WalletViewModel.buildManualEntryDraft(
            id = null,
            merchant = "Maxi 208",
            amount = "686.96",
            currency = "rsd",
            date = "2026-05-01",
            time = "19:55",
            category = Category.GROCERIES,
            isIncome = false,
            allowedCurrencies = listOf("USD", "EUR", "JPY", "RSD")
        )

        assertTrue(result.isSuccess)
        val draft = result.getOrThrow()
        assertEquals(null, draft.id)
        assertEquals("Maxi 208", draft.merchant)
        assertEquals(68_696L, draft.amountMinor)
        assertEquals("RSD", draft.currency)
        assertEquals(Category.GROCERIES, draft.category)
        val dateTime = Instant.ofEpochMilli(draft.occurredAt)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
        assertEquals(2026, dateTime.year)
        assertEquals(5, dateTime.monthValue)
        assertEquals(1, dateTime.dayOfMonth)
        assertEquals(19, dateTime.hour)
        assertEquals(55, dateTime.minute)
    }

    @Test
    fun storesIncomeAsNegativeAmount() {
        val result = WalletViewModel.buildManualEntryDraft(
            id = null,
            merchant = "Salary",
            amount = "1500.00",
            currency = "RSD",
            date = "2026-05-01",
            time = "09:00",
            category = null,
            isIncome = true,
            allowedCurrencies = listOf("USD", "EUR", "JPY", "RSD")
        )

        assertTrue(result.isSuccess)
        assertEquals(-150_000L, result.getOrThrow().amountMinor)
    }

    @Test
    fun preservesIdWhenEditing() {
        val result = WalletViewModel.buildManualEntryDraft(
            id = 42L,
            merchant = "Maxi 208",
            amount = "10.00",
            currency = "RSD",
            date = "2026-05-01",
            time = "12:00",
            category = null,
            isIncome = false,
            allowedCurrencies = listOf("RSD")
        )

        assertTrue(result.isSuccess)
        assertEquals(42L, result.getOrThrow().id)
    }

    @Test
    fun rejectsInvalidCurrency() {
        val result = WalletViewModel.buildManualEntryDraft(
            id = null,
            merchant = "PythonAnywhere",
            amount = "5.00",
            currency = "CHF",
            date = "2026-04-28",
            time = "14:52",
            category = null,
            isIncome = false,
            allowedCurrencies = listOf("USD", "EUR", "JPY", "RSD")
        )

        assertTrue(result.isFailure)
        assertEquals(
            "Currency must be one of: USD, EUR, JPY, RSD.",
            result.exceptionOrNull()?.message
        )
    }
}
