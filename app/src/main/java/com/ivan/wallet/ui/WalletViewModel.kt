package com.ivan.wallet.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ivan.wallet.WalletApplication
import com.ivan.wallet.data.db.BudgetEntity
import com.ivan.wallet.data.db.CategoryRuleEntity
import com.ivan.wallet.data.db.TransactionEntity
import com.ivan.wallet.data.model.Category
import com.ivan.wallet.data.sms.ImportSummary
import com.ivan.wallet.util.CsvExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class TransactionUiModel(
    val id: Long,
    val merchant: String,
    val normalizedMerchant: String,
    val amountMinor: Long,
    val currency: String,
    val occurredAt: Long,
    val balanceMinor: Long?,
    val balanceCurrency: String?,
    val category: Category?,
    val rawBody: String,
    val isManual: Boolean
)

data class ManualEntryDraft(
    val id: Long?,
    val merchant: String,
    val amountMinor: Long,
    val currency: String,
    val occurredAt: Long,
    val category: Category?
)

data class RuleUiModel(
    val id: Long,
    val matchKey: String,
    val category: Category
)

data class BudgetUiModel(
    val category: Category,
    val currency: String,
    val monthlyLimitMinor: Long,
    val startedAt: Long
)

data class WalletUiState(
    val transactions: List<TransactionUiModel> = emptyList(),
    val rules: List<RuleUiModel> = emptyList(),
    val budgets: List<BudgetUiModel> = emptyList(),
    val supportedCurrencies: List<String> = listOf("EUR", "JPY", "RSD", "USD"),
    val lastImportSummary: String? = null,
    val currencySettingsMessage: String? = null,
    val exportMessage: String? = null,
    val hasSmsPermission: Boolean = false,
    val reportMonth: YearMonth = YearMonth.now(),
    val selectedCurrency: String? = null
)

class WalletViewModel(application: Application) : AndroidViewModel(application) {
    private val appContainer = (application as WalletApplication).appContainer
    private val repository = appContainer.transactionRepository
    private val smsImporter = appContainer.smsImporter

    private val importSummary = MutableStateFlow<String?>(null)
    private val currencySettingsMessage = MutableStateFlow<String?>(null)
    private val exportMessage = MutableStateFlow<String?>(null)
    private val hasSmsPermission = MutableStateFlow(false)
    private val selectedMonth = MutableStateFlow(YearMonth.now())
    private val selectedCurrency = MutableStateFlow<String?>(null)

    private val contentState = combine(
        repository.observeTransactions(),
        repository.observeRules(),
        repository.observeSupportedCurrencies(),
        repository.observeBudgets()
    ) { transactions, rules, supportedCurrencies, budgets ->
        Content(
            transactions = transactions.map(::toTransactionUi),
            rules = rules.mapNotNull(::toRuleUi),
            supportedCurrencies = supportedCurrencies.map { it.code },
            budgets = budgets.mapNotNull(::toBudgetUi)
        )
    }

    private val baseState = combine(
        contentState,
        importSummary,
        currencySettingsMessage,
        exportMessage,
        hasSmsPermission
    ) { content, summary, currencyMsg, exportMsg, permission ->
        WalletUiState(
            transactions = content.transactions,
            rules = content.rules,
            budgets = content.budgets,
            supportedCurrencies = content.supportedCurrencies,
            lastImportSummary = summary,
            currencySettingsMessage = currencyMsg,
            exportMessage = exportMsg,
            hasSmsPermission = permission
        )
    }

    val uiState: StateFlow<WalletUiState> = combine(
        baseState,
        selectedMonth,
        selectedCurrency
    ) { state, month, currency ->
        state.copy(
            reportMonth = month,
            selectedCurrency = currency ?: state.transactions.firstOrNull()?.currency
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WalletUiState()
    )

    fun onSmsPermissionChanged(isGranted: Boolean) {
        hasSmsPermission.value = isGranted
    }

    fun selectReportMonth(month: YearMonth) {
        selectedMonth.value = month
    }

    fun selectCurrency(currency: String) {
        selectedCurrency.value = currency
    }

    fun importSms(context: Context) {
        viewModelScope.launch {
            val summary = smsImporter.importInbox(context)
            importSummary.value = formatImportSummary(summary)
            val currentCurrency = selectedCurrency.value
            if (currentCurrency == null) {
                selectedCurrency.value = uiState.value.transactions.firstOrNull()?.currency
            }
        }
    }

    fun saveEntry(draft: ManualEntryDraft) {
        viewModelScope.launch {
            val normalizedMerchant = appContainer.bankParser.normalizeMerchant(draft.merchant)
            if (draft.id == null) {
                repository.addManualTransaction(
                    merchantDisplay = draft.merchant,
                    normalizedMerchant = normalizedMerchant,
                    amountMinor = draft.amountMinor,
                    currency = draft.currency,
                    occurredAt = draft.occurredAt,
                    categoryId = draft.category?.name
                )
            } else {
                repository.updateTransaction(
                    id = draft.id,
                    merchantDisplay = draft.merchant,
                    normalizedMerchant = normalizedMerchant,
                    amountMinor = draft.amountMinor,
                    currency = draft.currency,
                    occurredAt = draft.occurredAt,
                    categoryId = draft.category?.name
                )
            }
            if (selectedCurrency.value == null) {
                selectedCurrency.value = draft.currency
            }
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
        }
    }

    fun deleteTransactions(ids: List<Long>) {
        viewModelScope.launch {
            repository.deleteTransactions(ids)
        }
    }

    fun assignCategoryToTransactions(ids: List<Long>, category: Category?) {
        viewModelScope.launch {
            repository.applyCategoryToTransactions(ids, category?.name)
        }
    }

    fun addSupportedCurrency(input: String) {
        viewModelScope.launch {
            val code = input.trim().uppercase(Locale.ROOT)
            when {
                !code.matches(Regex("[A-Z]{3}")) -> {
                    currencySettingsMessage.value = "Currency code must be exactly 3 letters."
                }

                code in uiState.value.supportedCurrencies -> {
                    currencySettingsMessage.value = "$code already exists."
                }

                repository.addSupportedCurrency(code) -> {
                    currencySettingsMessage.value = "Added $code."
                }

                else -> {
                    currencySettingsMessage.value = "Could not add $code."
                }
            }
        }
    }

    fun deleteSupportedCurrency(code: String) {
        viewModelScope.launch {
            if (uiState.value.supportedCurrencies.size <= 1) {
                currencySettingsMessage.value = "At least one currency must remain."
                return@launch
            }

            repository.deleteSupportedCurrency(code)
            currencySettingsMessage.value = "Removed $code."
            if (selectedCurrency.value == code) {
                selectedCurrency.value = uiState.value.supportedCurrencies.firstOrNull { it != code }
            }
        }
    }

    fun assignCategory(transactionId: Long, category: Category) {
        viewModelScope.launch {
            repository.applyCategory(transactionId, category.name)
        }
    }

    fun deleteRule(ruleId: Long) {
        viewModelScope.launch {
            repository.deleteRule(ruleId)
        }
    }

    fun upsertRule(matchKey: String, category: Category) {
        viewModelScope.launch {
            val key = matchKey.trim()
            if (key.isNotEmpty()) {
                repository.upsertRule(key, category.name)
            }
        }
    }

    fun saveBudget(category: Category, currency: String, monthlyLimitMinor: Long) {
        viewModelScope.launch {
            repository.upsertBudget(category.name, currency, monthlyLimitMinor)
        }
    }

    fun deleteBudget(category: Category, currency: String) {
        viewModelScope.launch {
            repository.deleteBudget(category.name, currency)
        }
    }

    fun exportCsv(context: Context, uri: Uri) {
        viewModelScope.launch {
            val transactions = uiState.value.transactions
            val result = CsvExporter.export(context, uri, transactions)
            exportMessage.value = result.fold(
                onSuccess = { count -> "Exported $count transactions." },
                onFailure = { "Export failed: ${it.message}" }
            )
        }
    }

    fun availableMonths(transactions: List<TransactionUiModel>): List<YearMonth> {
        return transactions
            .map {
                YearMonth.from(
                    Instant.ofEpochMilli(it.occurredAt).atZone(ZoneId.systemDefault())
                )
            }
            .distinct()
            .sortedDescending()
            .ifEmpty { listOf(YearMonth.now()) }
    }

    private fun toTransactionUi(entity: TransactionEntity): TransactionUiModel {
        return TransactionUiModel(
            id = entity.id,
            merchant = entity.merchantDisplay,
            normalizedMerchant = entity.normalizedMerchant,
            amountMinor = entity.amountMinor,
            currency = entity.currency,
            occurredAt = entity.occurredAt,
            balanceMinor = entity.balanceMinor,
            balanceCurrency = entity.balanceCurrency,
            category = Category.fromId(entity.categoryId),
            rawBody = entity.rawBody,
            isManual = entity.sender == "MANUAL"
        )
    }

    private fun toRuleUi(entity: CategoryRuleEntity): RuleUiModel? {
        val category = Category.fromId(entity.categoryId) ?: return null
        return RuleUiModel(
            id = entity.id,
            matchKey = entity.matchKey,
            category = category
        )
    }

    private fun toBudgetUi(entity: BudgetEntity): BudgetUiModel? {
        val category = Category.fromId(entity.categoryId) ?: return null
        return BudgetUiModel(
            category = category,
            currency = entity.currency,
            monthlyLimitMinor = entity.monthlyLimitMinor,
            startedAt = entity.startedAt
        )
    }

    private fun formatImportSummary(summary: ImportSummary): String {
        return "Scanned ${summary.scanned} Alta Bank SMS, parsed ${summary.parsed}, inserted ${summary.inserted} new transactions, skipped ${summary.duplicates} duplicates."
    }

    private data class Content(
        val transactions: List<TransactionUiModel>,
        val rules: List<RuleUiModel>,
        val supportedCurrencies: List<String>,
        val budgets: List<BudgetUiModel>
    )

    companion object {
        private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        fun buildManualEntryDraft(
            id: Long?,
            merchant: String,
            amount: String,
            currency: String,
            date: String,
            time: String,
            category: Category?,
            isIncome: Boolean,
            allowedCurrencies: Collection<String>
        ): Result<ManualEntryDraft> {
            val merchantValue = merchant.trim()
            if (merchantValue.isEmpty()) {
                return Result.failure(IllegalArgumentException("Merchant is required."))
            }

            val absAmountMinor = parseAmountMinor(amount)
                ?: return Result.failure(IllegalArgumentException("Amount must be a valid positive number."))
            if (absAmountMinor <= 0) {
                return Result.failure(IllegalArgumentException("Amount must be greater than zero."))
            }
            val amountMinor = if (isIncome) -absAmountMinor else absAmountMinor

            val currencyValue = currency.trim().uppercase(Locale.ROOT)
            val supportedCurrencies = allowedCurrencies.map { it.uppercase(Locale.ROOT) }.toSet()
            if (currencyValue !in supportedCurrencies) {
                return Result.failure(
                    IllegalArgumentException(
                        "Currency must be one of: ${allowedCurrencies.joinToString(", ")}."
                    )
                )
            }

            val occurredAt = try {
                LocalDateTime.of(
                    LocalDate.parse(date.trim(), dateFormatter),
                    LocalTime.parse(time.trim(), timeFormatter)
                ).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            } catch (_: Exception) {
                return Result.failure(
                    IllegalArgumentException("Date must be YYYY-MM-DD and time must be HH:MM.")
                )
            }

            return Result.success(
                ManualEntryDraft(
                    id = id,
                    merchant = merchantValue,
                    amountMinor = amountMinor,
                    currency = currencyValue,
                    occurredAt = occurredAt,
                    category = category
                )
            )
        }

        private fun parseAmountMinor(value: String): Long? {
            val trimmed = value.trim()
            if (trimmed.isEmpty()) return null
            val normalized = when {
                trimmed.contains(',') && trimmed.contains('.') -> trimmed.replace(",", "")
                trimmed.contains(',') -> trimmed.replace(",", ".")
                else -> trimmed
            }

            return normalized.toBigDecimalOrNull()
                ?.setScale(2, RoundingMode.HALF_UP)
                ?.movePointRight(2)
                ?.longValueExact()
        }
    }
}
