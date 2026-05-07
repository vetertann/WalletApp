package com.ivan.wallet.data.repository

import com.ivan.wallet.data.db.BudgetDao
import com.ivan.wallet.data.db.BudgetEntity
import com.ivan.wallet.data.db.CategoryRuleDao
import com.ivan.wallet.data.db.CategoryRuleEntity
import com.ivan.wallet.data.db.SupportedCurrencyDao
import com.ivan.wallet.data.db.SupportedCurrencyEntity
import com.ivan.wallet.data.db.TransactionDao
import com.ivan.wallet.data.db.TransactionEntity
import com.ivan.wallet.data.model.ParsedBankMessage
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val ruleDao: CategoryRuleDao,
    private val supportedCurrencyDao: SupportedCurrencyDao,
    private val budgetDao: BudgetDao
) {
    fun observeTransactions(): Flow<List<TransactionEntity>> = transactionDao.observeAll()

    fun observeRules(): Flow<List<CategoryRuleEntity>> = ruleDao.observeAll()

    fun observeSupportedCurrencies(): Flow<List<SupportedCurrencyEntity>> = supportedCurrencyDao.observeAll()

    fun observeBudgets(): Flow<List<BudgetEntity>> = budgetDao.observeAll()

    suspend fun importParsedMessages(messages: List<ParsedBankMessage>): Int {
        if (messages.isEmpty()) return 0
        val rules = ruleDao.getAll()
        var inserted = 0

        messages.forEach { message ->
            if (
                transactionDao.existsImportedEquivalent(
                    sender = message.sender,
                    amountMinor = message.amountMinor,
                    currency = message.currency,
                    normalizedMerchant = message.normalizedMerchant,
                    occurredAt = message.occurredAt
                )
            ) {
                return@forEach
            }

            val categoryId = resolveCategory(message.normalizedMerchant, rules)
            val rowId = transactionDao.insertIgnore(
                TransactionEntity(
                    externalId = message.externalId,
                    sender = message.sender,
                    rawBody = message.rawBody,
                    amountMinor = message.amountMinor,
                    currency = message.currency,
                    merchantDisplay = message.merchantDisplay,
                    normalizedMerchant = message.normalizedMerchant,
                    occurredAt = message.occurredAt,
                    balanceMinor = message.balanceMinor,
                    balanceCurrency = message.balanceCurrency,
                    receivedAt = message.receivedAt,
                    categoryId = categoryId
                )
            )
            if (rowId != -1L) {
                inserted += 1
            }
        }

        return inserted
    }

    suspend fun addManualTransaction(
        merchantDisplay: String,
        normalizedMerchant: String,
        amountMinor: Long,
        currency: String,
        occurredAt: Long,
        categoryId: String?
    ) {
        val rules = ruleDao.getAll()
        val resolvedCategory = categoryId ?: resolveCategory(normalizedMerchant, rules)
        val now = System.currentTimeMillis()

        transactionDao.insertIgnore(
            TransactionEntity(
                externalId = "manual:${UUID.randomUUID()}",
                sender = MANUAL_SENDER,
                rawBody = "",
                amountMinor = amountMinor,
                currency = currency,
                merchantDisplay = merchantDisplay,
                normalizedMerchant = normalizedMerchant,
                occurredAt = occurredAt,
                balanceMinor = null,
                balanceCurrency = null,
                receivedAt = now,
                categoryId = resolvedCategory
            )
        )

        if (categoryId != null) {
            learnCategory(normalizedMerchant = normalizedMerchant, categoryId = categoryId)
        }
    }

    suspend fun updateTransaction(
        id: Long,
        merchantDisplay: String,
        normalizedMerchant: String,
        amountMinor: Long,
        currency: String,
        occurredAt: Long,
        categoryId: String?
    ) {
        transactionDao.updateEntry(
            id = id,
            merchantDisplay = merchantDisplay,
            normalizedMerchant = normalizedMerchant,
            amountMinor = amountMinor,
            currency = currency,
            occurredAt = occurredAt,
            categoryId = categoryId
        )
        if (categoryId != null) {
            learnCategory(normalizedMerchant = normalizedMerchant, categoryId = categoryId)
        }
    }

    suspend fun deleteTransaction(id: Long) {
        transactionDao.deleteById(id)
    }

    suspend fun deleteTransactions(ids: List<Long>) {
        if (ids.isNotEmpty()) {
            transactionDao.deleteByIds(ids)
        }
    }

    suspend fun applyCategoryToTransactions(ids: List<Long>, categoryId: String?) {
        if (ids.isEmpty()) return
        transactionDao.updateCategoryForIds(ids, categoryId)
        if (categoryId != null) {
            val merchants = ids.mapNotNull { id -> transactionDao.getById(id)?.normalizedMerchant }
                .filter { it.isNotBlank() }
                .distinct()
            merchants.forEach { merchant ->
                learnCategory(normalizedMerchant = merchant, categoryId = categoryId)
            }
        }
    }

    suspend fun applyCategory(transactionId: Long, categoryId: String?) {
        val transaction = transactionDao.getById(transactionId) ?: return
        transactionDao.updateCategory(transactionId, categoryId)
        if (categoryId != null) {
            learnCategory(normalizedMerchant = transaction.normalizedMerchant, categoryId = categoryId)
        }
    }

    suspend fun deleteRule(ruleId: Long) {
        ruleDao.deleteById(ruleId)
    }

    suspend fun upsertRule(matchKey: String, categoryId: String) {
        ruleDao.upsert(
            CategoryRuleEntity(
                matchKey = matchKey.uppercase(),
                categoryId = categoryId,
                updatedAt = System.currentTimeMillis()
            )
        )
        transactionDao.applyCategoryByMerchantMatch(
            key = matchKey.uppercase(),
            categoryId = categoryId
        )
    }

    suspend fun addSupportedCurrency(code: String): Boolean {
        return supportedCurrencyDao.insertIgnore(SupportedCurrencyEntity(code = code)) != -1L
    }

    suspend fun deleteSupportedCurrency(code: String) {
        supportedCurrencyDao.deleteByCode(code)
    }

    suspend fun upsertBudget(categoryId: String, currency: String, monthlyLimitMinor: Long) {
        val existing = budgetDao.findByCategoryAndCurrency(categoryId, currency)
        budgetDao.upsert(
            BudgetEntity(
                id = existing?.id ?: 0L,
                categoryId = categoryId,
                currency = currency,
                monthlyLimitMinor = monthlyLimitMinor,
                startedAt = existing?.startedAt ?: System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteBudget(categoryId: String, currency: String) {
        budgetDao.deleteByCategoryAndCurrency(categoryId, currency)
    }

    private fun resolveCategory(
        normalizedMerchant: String,
        rules: List<CategoryRuleEntity>
    ): String? {
        val exact = rules.firstOrNull { it.matchKey == normalizedMerchant }
        if (exact != null) return exact.categoryId

        return rules.firstOrNull { rule ->
            normalizedMerchant.contains(rule.matchKey) || rule.matchKey.contains(normalizedMerchant)
        }?.categoryId
    }

    private suspend fun learnCategory(
        normalizedMerchant: String,
        categoryId: String
    ) {
        ruleDao.upsert(
            CategoryRuleEntity(
                matchKey = normalizedMerchant,
                categoryId = categoryId,
                updatedAt = System.currentTimeMillis()
            )
        )
        transactionDao.applyCategoryByMerchantMatch(
            key = normalizedMerchant,
            categoryId = categoryId
        )
    }

    private companion object {
        const val MANUAL_SENDER = "MANUAL"
    }
}
