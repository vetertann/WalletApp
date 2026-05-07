package com.ivan.wallet.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivan.wallet.data.model.Category
import com.ivan.wallet.ui.BudgetUiModel
import com.ivan.wallet.ui.TransactionUiModel
import com.ivan.wallet.ui.WalletUiState
import com.ivan.wallet.util.RecurringDetector
import com.ivan.wallet.util.toMoney
import java.math.RoundingMode
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

private val PrimaryText = Color(0xFF1F1E1B)
private val SecondaryText = Color(0xFF7A736A)
private val Surface = Color(0xFFFFFCF7)
private val DividerColor = Color(0xFFE6E0D5)
private val Warn = Color(0xFFE07A2B)
private val Danger = Color(0xFFB04848)
private val Good = Color(0xFF1D9E75)

@Composable
fun BudgetsScreen(
    state: WalletUiState,
    onSaveBudget: (Category, String, Long) -> Unit,
    onDeleteBudget: (Category, String) -> Unit
) {
    var dialogTarget by remember { mutableStateOf<BudgetEditTarget?>(null) }
    val currency = state.selectedCurrency ?: state.supportedCurrencies.firstOrNull() ?: "RSD"
    val month = YearMonth.now()
    val monthSpend = monthSpendByCategory(state, currency, month)
    val budgets = state.budgets.filter { it.currency == currency }
    val recurring = RecurringDetector.detect(state.transactions.filter { it.currency == currency })
    val now = System.currentTimeMillis()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            HeaderRow(
                title = "Budgets",
                subtitle = "$currency · ${month.month.name.lowercase().replaceFirstChar { it.titlecase() }}",
                onAdd = { dialogTarget = BudgetEditTarget(category = null, currency = currency, limitMinor = 0L) }
            )
        }

        if (budgets.isEmpty()) {
            item {
                EmptyBudgets(
                    onAdd = { dialogTarget = BudgetEditTarget(category = null, currency = currency, limitMinor = 0L) }
                )
            }
        } else {
            val nowMs = System.currentTimeMillis()
            val perBudgetRunning = budgets.associateWith { budget ->
                runningBalance(state.transactions, budget, nowMs)
            }
            val totalCap = budgets.sumOf { it.monthlyLimitMinor }
            val totalSpent = budgets.sumOf { monthSpend[it.category] ?: 0L }
            val totalRunning = perBudgetRunning.values.sumOf { it.first }
            val maxMonths = perBudgetRunning.values.maxOfOrNull { it.second } ?: 1

            item {
                BudgetsTotalsCard(
                    currency = currency,
                    totalCapMinor = totalCap,
                    totalSpentMinor = totalSpent,
                    totalRunningMinor = totalRunning,
                    maxMonths = maxMonths,
                    budgetCount = budgets.size
                )
            }

            items(budgets, key = { it.category.name + it.currency }) { budget ->
                val (running, monthsCounted) = perBudgetRunning.getValue(budget)
                BudgetRow(
                    budget = budget,
                    spentMinor = monthSpend[budget.category] ?: 0L,
                    runningBalanceMinor = running,
                    monthsCounted = monthsCounted,
                    onClick = {
                        dialogTarget = BudgetEditTarget(
                            category = budget.category,
                            currency = budget.currency,
                            limitMinor = budget.monthlyLimitMinor
                        )
                    },
                    onDelete = { onDeleteBudget(budget.category, budget.currency) }
                )
            }
        }

        item { Spacer16() }

        item {
            Text(
                text = "Subscriptions",
                color = PrimaryText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        if (recurring.isEmpty()) {
            item {
                Text(
                    text = "Recurring charges will surface here once we see the same merchant 3+ times at a steady cadence.",
                    color = SecondaryText,
                    fontSize = 12.sp
                )
            }
        } else {
            items(recurring, key = { it.normalizedMerchant + it.currency }) { match ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Surface)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = match.merchant,
                            color = PrimaryText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${match.cadenceLabel} · ${match.occurrences}× · last ${RecurringDetector.lastSeen(match, now)}",
                            color = SecondaryText,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = "~${match.averageAmountMinor.toMoney(match.currency)}",
                        color = if (RecurringDetector.isFresh(match, now)) PrimaryText else SecondaryText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    dialogTarget?.let { target ->
        BudgetEditDialog(
            target = target,
            existingBudgets = state.budgets,
            onDismiss = { dialogTarget = null },
            onSave = { category, currencyCode, limit ->
                onSaveBudget(category, currencyCode, limit)
                dialogTarget = null
            }
        )
    }
}

@Composable
private fun Spacer16() {
    Box(modifier = Modifier.height(16.dp))
}

@Composable
private fun HeaderRow(title: String, subtitle: String, onAdd: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = title, color = PrimaryText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, color = SecondaryText, fontSize = 12.sp)
        }
        IconButton(onClick = onAdd) {
            Icon(Icons.Outlined.Add, contentDescription = "Add budget", tint = PrimaryText)
        }
    }
}

@Composable
private fun BudgetsTotalsCard(
    currency: String,
    totalCapMinor: Long,
    totalSpentMinor: Long,
    totalRunningMinor: Long,
    maxMonths: Int,
    budgetCount: Int
) {
    val ratio = if (totalCapMinor <= 0L) 0f
    else (totalSpentMinor.toFloat() / totalCapMinor.toFloat()).coerceAtLeast(0f)
    val barColor = when {
        ratio >= 1f -> Danger
        ratio >= 0.8f -> Warn
        else -> Good
    }
    val running = totalRunningMinor
    val runningSign = if (running >= 0) "+" else "−"
    val runningColor = if (running >= 0) Good else Danger

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF2C2C2A))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "All budgets · this month",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
                Text(
                    text = totalSpentMinor.toMoney(currency),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "of ${totalCapMinor.toMoney(currency)}",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
                Text(
                    text = "${(ratio * 100).toInt()}%",
                    color = barColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.12f))
        ) {
            val width = size.width * ratio.coerceAtMost(1f)
            drawRoundRect(
                color = barColor,
                topLeft = Offset(0f, 0f),
                size = Size(width, size.height),
                cornerRadius = CornerRadius(0f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$budgetCount budgets · running over $maxMonths ${if (maxMonths == 1) "month" else "months"}",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp
            )
            Text(
                text = "$runningSign${abs(running).toMoney(currency)}",
                color = runningColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun BudgetRow(
    budget: BudgetUiModel,
    spentMinor: Long,
    runningBalanceMinor: Long,
    monthsCounted: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val ratio = if (budget.monthlyLimitMinor <= 0) 0f
    else (spentMinor.toFloat() / budget.monthlyLimitMinor.toFloat()).coerceAtLeast(0f)
    val barColor = when {
        ratio >= 1f -> Danger
        ratio >= 0.8f -> Warn
        else -> Good
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(budget.category.accent, RoundedCornerShape(3.dp))
                )
                Text(
                    text = budget.category.label,
                    color = PrimaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Delete budget",
                    tint = SecondaryText,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(DividerColor)
        ) {
            val width = size.width * ratio.coerceAtMost(1f)
            drawRoundRect(
                color = barColor,
                topLeft = Offset(0f, 0f),
                size = Size(width, size.height),
                cornerRadius = CornerRadius(0f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${spentMinor.toMoney(budget.currency)} of ${budget.monthlyLimitMinor.toMoney(budget.currency)}",
                color = SecondaryText,
                fontSize = 12.sp
            )
            Text(
                text = "${(ratio * 100).toInt()}%",
                color = barColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
        RunningBalanceLine(
            runningBalanceMinor = runningBalanceMinor,
            monthsCounted = monthsCounted,
            startedAt = budget.startedAt,
            currency = budget.currency
        )
    }
}

@Composable
private fun RunningBalanceLine(
    runningBalanceMinor: Long,
    monthsCounted: Int,
    startedAt: Long,
    currency: String
) {
    val isPositive = runningBalanceMinor >= 0
    val color = if (isPositive) Good else Danger
    val sign = if (isPositive) "+" else "−"
    val abs = abs(runningBalanceMinor).toMoney(currency)
    val sinceLabel = if (startedAt > 0) {
        val month = YearMonth.from(
            Instant.ofEpochMilli(startedAt).atZone(ZoneId.systemDefault())
        )
        "Since ${month.format(DateTimeFormatter.ofPattern("MMM yyyy"))}"
    } else "Since first entry"
    val monthsLabel = if (monthsCounted == 1) "1 month" else "$monthsCounted months"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$sinceLabel · $monthsLabel",
            color = SecondaryText,
            fontSize = 11.sp
        )
        Text(
            text = "$sign$abs",
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun EmptyBudgets(onAdd: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "No budgets yet",
            color = PrimaryText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "Set a monthly cap per category to keep spending in check.",
            color = SecondaryText,
            fontSize = 12.sp
        )
        TextButton(onClick = onAdd) {
            Text("Add a budget", color = PrimaryText)
        }
    }
}

private data class BudgetEditTarget(
    val category: Category?,
    val currency: String,
    val limitMinor: Long
)

@Composable
private fun BudgetEditDialog(
    target: BudgetEditTarget,
    existingBudgets: List<BudgetUiModel>,
    onDismiss: () -> Unit,
    onSave: (Category, String, Long) -> Unit
) {
    var category by remember { mutableStateOf(target.category) }
    var amount by remember {
        mutableStateOf(
            if (target.limitMinor > 0) "%.2f".format(target.limitMinor / 100.0) else ""
        )
    }
    var error by remember { mutableStateOf<String?>(null) }
    val isEditing = target.category != null
    // Don't let the user pick a category that already has a budget for this currency,
    // unless it's the one they're editing.
    val excluded = existingBudgets
        .filter { it.currency == target.currency && it.category != target.category }
        .map { it.category }
        .toSet()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edit budget" else "Add budget", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Monthly limit (${target.currency})") },
                    singleLine = true
                )
                Text(text = "Category", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                if (isEditing) {
                    Text(
                        text = "Category is locked while editing. Delete this budget and add a new one to switch.",
                        color = SecondaryText,
                        fontSize = 12.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(target.category!!.accent, RoundedCornerShape(3.dp))
                        )
                        Text(
                            text = "${target.category.group.label} · ${target.category.label}",
                            color = PrimaryText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    CategoryPicker(
                        selected = category,
                        onSelect = { category = it },
                        excluded = excluded,
                        allowNone = false
                    )
                }
                error?.let { Text(it, color = Danger, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cat = category
                if (cat == null) {
                    error = "Pick a category."
                    return@TextButton
                }
                val minor = parseMinor(amount)
                if (minor == null || minor <= 0) {
                    error = "Enter a positive limit."
                    return@TextButton
                }
                onSave(cat, target.currency, minor)
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun monthSpendByCategory(
    state: WalletUiState,
    currency: String,
    month: YearMonth
): Map<Category, Long> {
    return state.transactions
        .filter { tx ->
            tx.currency == currency && tx.amountMinor > 0 && tx.category != null && YearMonth.from(
                Instant.ofEpochMilli(tx.occurredAt).atZone(ZoneId.systemDefault())
            ) == month
        }
        .groupBy { it.category!! }
        .mapValues { entry -> entry.value.sumOf { it.amountMinor } }
}

/**
 * Cumulative envelope-style balance for a budget.
 *
 * For each calendar month from [BudgetUiModel.startedAt]'s month up to and including
 * the current month, we credit the budget cap and debit the actual category spend.
 * The running balance is the sum: positive means underspent over the lifetime,
 * negative means over.
 *
 * Returns (runningBalanceMinor, monthsCounted). If startedAt is 0 (no history),
 * we fall back to the earliest matching transaction; if there's still nothing,
 * we count just the current month.
 */
private fun runningBalance(
    transactions: List<TransactionUiModel>,
    budget: BudgetUiModel,
    nowMs: Long
): Pair<Long, Int> {
    val zone = ZoneId.systemDefault()
    val matching = transactions.filter {
        it.category == budget.category && it.currency == budget.currency && it.amountMinor > 0
    }

    val effectiveStart = when {
        budget.startedAt > 0L -> budget.startedAt
        matching.isNotEmpty() -> matching.minOf { it.occurredAt }
        else -> nowMs
    }
    val startMonth = YearMonth.from(Instant.ofEpochMilli(effectiveStart).atZone(zone))
    val endMonth = YearMonth.from(Instant.ofEpochMilli(nowMs).atZone(zone))

    var months = 0
    var balance = 0L
    var month = startMonth
    while (!month.isAfter(endMonth)) {
        months++
        val spentInMonth = matching
            .filter { YearMonth.from(Instant.ofEpochMilli(it.occurredAt).atZone(zone)) == month }
            .sumOf { it.amountMinor }
        balance += budget.monthlyLimitMinor - spentInMonth
        month = month.plusMonths(1)
    }
    return balance to months.coerceAtLeast(1)
}

private fun parseMinor(value: String): Long? {
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
