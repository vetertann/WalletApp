package com.ivan.wallet.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.Button
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivan.wallet.data.model.Category
import com.ivan.wallet.ui.ManualEntryDraft
import com.ivan.wallet.ui.TransactionUiModel
import com.ivan.wallet.ui.WalletUiState
import com.ivan.wallet.util.toMoney
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

private enum class PeriodFilter(val label: String, val days: Long?) {
    WEEK("Week", 7),
    THIS_MONTH("This month", null),
    THIRTY("30 days", 30),
    SIX_MONTHS("6 months", 182),
    YEAR("Year", 365),
    ALL("All", null),
    CUSTOM("Custom", null)
}

private enum class SourceFilter(val label: String) {
    ALL("All"),
    BANK("Bank"),
    MANUAL("Manual")
}

private enum class SortMode(val label: String) {
    LATEST("Latest"),
    OLDEST("Oldest")
}

private data class CategoryShare(
    val label: String,
    val amountMinor: Long,
    val percentage: Int,
    val color: Color
)

private data class DateSection(
    val title: String,
    val transactions: List<TransactionUiModel>
)

private data class PeriodTotals(
    val incomeMinor: Long,
    val spentMinor: Long
)

private val PrimaryText = Color(0xFF1F1E1B)
private val SecondaryText = Color(0xFF7A736A)
private val TertiaryText = Color(0xFFA8A097)
private val Divider = Color(0xFFE6E0D5)
private val ChipBg = Color(0xFFEFEAE0)
private val IncomeGreen = Color(0xFF0F6E56)
private val DeltaGood = Color(0xFF97C459)
private val DeltaBad = Color(0xFFF09595)
private val ScreenBackground = Color(0xFFF5F2EC)
private val SelectedBackground = Color(0xFFFFF1D6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    state: WalletUiState,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isSearchOpen: Boolean,
    onSearchOpenChange: (Boolean) -> Unit,
    onRequestSmsPermission: () -> Unit,
    onSyncSms: () -> Unit,
    onSaveEntry: (ManualEntryDraft) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    onDeleteTransactions: (List<Long>) -> Unit,
    onAssignCategoryToMany: (List<Long>, Category?) -> Unit,
    onOpenStats: () -> Unit
) {
    var editingTransaction by remember { mutableStateOf<TransactionUiModel?>(null) }
    var isCreateOpen by remember { mutableStateOf(false) }
    var selectedPeriod by remember { mutableStateOf(PeriodFilter.THIRTY) }
    var customRange by remember { mutableStateOf<LongRange?>(null) }
    var showCustomPicker by remember { mutableStateOf(false) }
    var selectedSource by remember { mutableStateOf(SourceFilter.ALL) }
    var selectedSort by remember { mutableStateOf(SortMode.LATEST) }
    var selectedCurrency by remember { mutableStateOf("") }
    var isFilterMenuOpen by remember { mutableStateOf(false) }
    var isSortMenuOpen by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var bulkCategoryMenuOpen by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    val refreshState = rememberPullToRefreshState()
    val scope = rememberCoroutineScope()

    val availableCurrencies = remember(state.transactions, state.supportedCurrencies) {
        (state.transactions.map { it.currency } + state.supportedCurrencies)
            .distinct()
            .sorted()
    }

    LaunchedEffect(availableCurrencies, state.selectedCurrency) {
        if (selectedCurrency !in availableCurrencies) {
            selectedCurrency = when {
                "RSD" in availableCurrencies -> "RSD"
                state.selectedCurrency in availableCurrencies -> state.selectedCurrency.orEmpty()
                availableCurrencies.isNotEmpty() -> availableCurrencies.first()
                else -> "RSD"
            }
        }
    }

    val now = System.currentTimeMillis()
    val currencyTransactions = state.transactions.filter { it.currency == selectedCurrency }
    val currentRange = activeRange(selectedPeriod, customRange, now, offsetPeriods = 0)
    val previousRange = activeRange(selectedPeriod, customRange, now, offsetPeriods = 1)

    val rangeTransactions = currencyTransactions.filter {
        currentRange == null || it.occurredAt in currentRange
    }
    val searched = if (searchQuery.isBlank()) rangeTransactions
    else rangeTransactions.filter {
        it.merchant.contains(searchQuery, ignoreCase = true) ||
                (it.category?.label?.contains(searchQuery, ignoreCase = true) == true)
    }
    val visibleTransactions = searched
        .filter { transaction ->
            when (selectedSource) {
                SourceFilter.ALL -> true
                SourceFilter.BANK -> !transaction.isManual
                SourceFilter.MANUAL -> transaction.isManual
            }
        }
        .let { transactions ->
            when (selectedSort) {
                SortMode.LATEST -> transactions.sortedByDescending { it.occurredAt }
                SortMode.OLDEST -> transactions.sortedBy { it.occurredAt }
            }
        }

    val currentTotals = totalsFor(rangeTransactions)
    val previousTotals = totalsFor(
        currencyTransactions.filter { previousRange != null && it.occurredAt in previousRange }
    )

    val latestBalanceMinor = currencyTransactions
        .filter { it.balanceCurrency == selectedCurrency }
        .sortedByDescending { it.occurredAt }
        .firstOrNull()
        ?.balanceMinor
    val categoryShares = categoryShares(visibleTransactions.filter { it.amountMinor > 0 })
    val sections = buildSections(visibleTransactions, selectedSort)
    val isAllEmpty = state.transactions.isEmpty()
    val inSelectionMode = selectedIds.isNotEmpty()

    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            state = refreshState,
            onRefresh = {
                if (state.hasSmsPermission) {
                    isRefreshing = true
                    onSyncSms()
                    scope.launch {
                        delay(900)
                        isRefreshing = false
                    }
                }
            }
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = rememberLazyListState(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 100.dp)
            ) {
                if (isSearchOpen) {
                    item {
                        SearchField(
                            query = searchQuery,
                            onQueryChange = onSearchQueryChange,
                            onClose = {
                                onSearchOpenChange(false)
                                onSearchQueryChange("")
                            }
                        )
                    }
                    item { Spacer8() }
                }

                item {
                    BalanceCard(
                        currency = selectedCurrency,
                        latestBalanceMinor = latestBalanceMinor,
                        current = currentTotals,
                        previous = previousTotals,
                        showDelta = previousRange != null
                    )
                }

                if (!state.hasSmsPermission) {
                    item {
                        SmsPermissionBanner(onRequestSmsPermission = onRequestSmsPermission)
                    }
                }

                if (availableCurrencies.size > 1) {
                    item {
                        CurrencyFilterRow(
                            currencies = availableCurrencies,
                            selectedCurrency = selectedCurrency,
                            onSelected = { selectedCurrency = it }
                        )
                    }
                }

                item { Spacer8() }

                item {
                    PeriodFilterRow(
                        selected = selectedPeriod,
                        customRange = customRange,
                        onSelected = { filter ->
                            selectedPeriod = filter
                            if (filter == PeriodFilter.CUSTOM) {
                                showCustomPicker = true
                            }
                        }
                    )
                }

                item { Spacer14() }

                item {
                    CategoryBreakdown(
                        categoryShares = categoryShares,
                        onOpenStats = onOpenStats
                    )
                }

                item { Spacer14() }

                item {
                    RecentHeader(
                        selectedSource = selectedSource,
                        selectedSort = selectedSort,
                        isFilterMenuOpen = isFilterMenuOpen,
                        isSortMenuOpen = isSortMenuOpen,
                        onFilterMenuToggle = { isFilterMenuOpen = !isFilterMenuOpen },
                        onSortMenuToggle = { isSortMenuOpen = !isSortMenuOpen },
                        onFilterSelected = {
                            selectedSource = it
                            isFilterMenuOpen = false
                        },
                        onSortSelected = {
                            selectedSort = it
                            isSortMenuOpen = false
                        }
                    )
                }

                if (isAllEmpty) {
                    item {
                        FirstRunCta(
                            hasSmsPermission = state.hasSmsPermission,
                            onRequestSmsPermission = onRequestSmsPermission,
                            onSyncSms = onSyncSms,
                            onAddManual = { isCreateOpen = true }
                        )
                    }
                } else if (sections.isEmpty()) {
                    item { EmptyFilteredState() }
                } else {
                    sections.forEach { section ->
                        item {
                            Text(
                                text = section.title,
                                color = TertiaryText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
                            )
                        }
                        items(section.transactions, key = { it.id }) { transaction ->
                            TransactionRow(
                                item = transaction,
                                isSelected = transaction.id in selectedIds,
                                onClick = {
                                    if (inSelectionMode) {
                                        selectedIds = if (transaction.id in selectedIds)
                                            selectedIds - transaction.id
                                        else selectedIds + transaction.id
                                    } else {
                                        editingTransaction = transaction
                                    }
                                },
                                onLongPress = {
                                    selectedIds = selectedIds + transaction.id
                                }
                            )
                        }
                    }
                }
            }
        }

        if (inSelectionMode) {
            SelectionActionBar(
                count = selectedIds.size,
                onClear = { selectedIds = emptySet() },
                onDelete = {
                    onDeleteTransactions(selectedIds.toList())
                    selectedIds = emptySet()
                },
                onCategoryMenuToggle = { bulkCategoryMenuOpen = !bulkCategoryMenuOpen },
                bulkCategoryMenuOpen = bulkCategoryMenuOpen,
                onCategorySelected = { c ->
                    onAssignCategoryToMany(selectedIds.toList(), c)
                    bulkCategoryMenuOpen = false
                    selectedIds = emptySet()
                }
            )
        } else {
            FloatingActionButton(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 22.dp, bottom = 22.dp)
                    .size(56.dp),
                shape = RoundedCornerShape(18.dp),
                containerColor = PrimaryText,
                contentColor = Color.White,
                onClick = { isCreateOpen = true }
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add transaction")
            }
        }
    }

    if (showCustomPicker) {
        CustomRangePickerDialog(
            initialRange = customRange,
            onDismiss = {
                showCustomPicker = false
                if (customRange == null) selectedPeriod = PeriodFilter.THIRTY
            },
            onConfirm = { range ->
                customRange = range
                showCustomPicker = false
            }
        )
    }

    if (isCreateOpen) {
        ManualEntryDialog(
            existing = null,
            availableCurrencies = state.supportedCurrencies,
            onDismiss = { isCreateOpen = false },
            onSave = { draft ->
                onSaveEntry(draft)
                isCreateOpen = false
            }
        )
    }

    editingTransaction?.let { transaction ->
        ManualEntryDialog(
            existing = transaction,
            availableCurrencies = state.supportedCurrencies,
            onDismiss = { editingTransaction = null },
            onSave = { draft ->
                onSaveEntry(draft)
                editingTransaction = null
            },
            onDelete = {
                onDeleteTransaction(transaction.id)
                editingTransaction = null
            }
        )
    }
}

@Composable
private fun Spacer8() {
    Box(modifier = Modifier.height(8.dp))
}

@Composable
private fun Spacer14() {
    Box(modifier = Modifier.height(14.dp))
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(top = 8.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            modifier = Modifier.weight(1f),
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search merchant or category") },
            singleLine = true
        )
        IconButton(onClick = onClose) {
            Icon(Icons.Outlined.Close, contentDescription = "Close search")
        }
    }
}

@Composable
private fun BalanceCard(
    currency: String,
    latestBalanceMinor: Long?,
    current: PeriodTotals,
    previous: PeriodTotals,
    showDelta: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF2C2C2A))
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Total balance",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
                Text(
                    text = if (currency.isNotBlank()) latestBalanceMinor?.toMoney(currency) ?: "—"
                    else "—",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BalanceTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.ArrowUpward,
                    iconTint = DeltaGood,
                    label = "Income",
                    value = if (currency.isNotBlank()) current.incomeMinor.toMoney(currency) else "0",
                    delta = if (showDelta) deltaPercent(current.incomeMinor, previous.incomeMinor) else null,
                    higherIsBetter = true
                )
                BalanceTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.ArrowDownward,
                    iconTint = DeltaBad,
                    label = "Spent",
                    value = if (currency.isNotBlank()) current.spentMinor.toMoney(currency) else "0",
                    delta = if (showDelta) deltaPercent(current.spentMinor, previous.spentMinor) else null,
                    higherIsBetter = false
                )
            }
        }
    }
}

@Composable
private fun BalanceTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    delta: Double?,
    higherIsBetter: Boolean
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = label,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }
            Text(
                text = value,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            if (delta != null) {
                val isPositive = delta >= 0
                val good = if (higherIsBetter) isPositive else !isPositive
                Text(
                    text = formatDelta(delta),
                    color = if (good) DeltaGood else DeltaBad,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun SmsPermissionBanner(onRequestSmsPermission: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(top = 10.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFFFF1D6))
            .clickable(onClick = onRequestSmsPermission)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Grant SMS access to import bank transactions",
            color = Color(0xFF7A5400),
            fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "Grant",
            color = Color(0xFF7A5400),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun CurrencyFilterRow(
    currencies: List<String>,
    selectedCurrency: String,
    onSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .padding(top = 12.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        currencies.forEach { currency ->
            Pill(
                label = currency,
                selected = currency == selectedCurrency,
                onClick = { onSelected(currency) }
            )
        }
    }
}

@Composable
private fun PeriodFilterRow(
    selected: PeriodFilter,
    customRange: LongRange?,
    onSelected: (PeriodFilter) -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        PeriodFilter.entries.forEach { filter ->
            val label = if (filter == PeriodFilter.CUSTOM && selected == filter && customRange != null) {
                customRangeLabel(customRange)
            } else filter.label
            Pill(
                label = label,
                selected = filter == selected,
                onClick = { onSelected(filter) }
            )
        }
    }
}

@Composable
private fun Pill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) PrimaryText else Color.Transparent
    val fg = if (selected) Color.White else SecondaryText
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .let { m ->
                if (!selected) m.border(0.5.dp, Divider, RoundedCornerShape(999.dp)) else m
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = fg,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun CategoryBreakdown(
    categoryShares: List<CategoryShare>,
    onOpenStats: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "By category",
                color = PrimaryText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onOpenStats)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                text = "Stats ›",
                color = SecondaryText,
                fontSize = 12.sp
            )
        }
        if (categoryShares.isEmpty()) {
            Text(
                text = "No spending in this period yet.",
                color = SecondaryText,
                fontSize = 12.sp
            )
        } else {
            CategoryProgressBar(categoryShares = categoryShares)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                categoryShares.forEach { share ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(share.color, RoundedCornerShape(2.dp))
                        )
                        Text(
                            text = "${share.label} ${share.percentage}%",
                            color = SecondaryText,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryProgressBar(categoryShares: List<CategoryShare>) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
    ) {
        var currentX = 0f
        categoryShares.forEach { share ->
            val width = size.width * (share.percentage / 100f)
            drawRoundRect(
                color = share.color,
                topLeft = Offset(currentX, 0f),
                size = Size(width.coerceAtLeast(2f), size.height),
                cornerRadius = CornerRadius(0f)
            )
            currentX += width
        }
    }
}

@Composable
private fun RecentHeader(
    selectedSource: SourceFilter,
    selectedSort: SortMode,
    isFilterMenuOpen: Boolean,
    isSortMenuOpen: Boolean,
    onFilterMenuToggle: () -> Unit,
    onSortMenuToggle: () -> Unit,
    onFilterSelected: (SourceFilter) -> Unit,
    onSortSelected: (SortMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Recent",
            color = PrimaryText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box {
                MiniChip(
                    icon = Icons.Outlined.FilterList,
                    label = selectedSource.label,
                    onClick = onFilterMenuToggle
                )
                DropdownMenu(
                    expanded = isFilterMenuOpen,
                    onDismissRequest = onFilterMenuToggle
                ) {
                    SourceFilter.entries.forEach { filter ->
                        DropdownMenuItem(
                            text = { Text(filter.label) },
                            onClick = { onFilterSelected(filter) }
                        )
                    }
                }
            }
            Box {
                MiniChip(
                    icon = Icons.AutoMirrored.Outlined.Sort,
                    label = selectedSort.label,
                    onClick = onSortMenuToggle
                )
                DropdownMenu(
                    expanded = isSortMenuOpen,
                    onDismissRequest = onSortMenuToggle
                ) {
                    SortMode.entries.forEach { sortMode ->
                        DropdownMenuItem(
                            text = { Text(sortMode.label) },
                            onClick = { onSortSelected(sortMode) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(ChipBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SecondaryText,
            modifier = Modifier.size(11.dp)
        )
        Text(
            text = label,
            color = SecondaryText,
            fontSize = 11.sp
        )
    }
}


@Composable
private fun TransactionRow(
    item: TransactionUiModel,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) SelectedBackground else ScreenBackground)
            .pointerInput(item.id) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongPress() }
                )
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryGlyph(category = item.category, isManual = item.isManual)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = item.merchant,
                    color = PrimaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildSubtitle(item),
                    color = SecondaryText,
                    fontSize = 12.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                val isIncome = item.amountMinor < 0
                val absMoney = abs(item.amountMinor).toMoney(item.currency)
                Text(
                    text = if (isIncome) "+$absMoney" else "−$absMoney",
                    color = if (isIncome) IncomeGreen else PrimaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatTime(item.occurredAt),
                    color = TertiaryText,
                    fontSize = 11.sp
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(Divider)
        )
    }
}

@Composable
private fun CategoryGlyph(
    category: Category?,
    isManual: Boolean
) {
    val color = category?.accent ?: if (isManual) Color(0xFF1D9E75) else Color(0xFF534AB7)
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = iconForCategory(category, isManual),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun EmptyFilteredState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "No transactions for this view",
            color = PrimaryText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "Try a different period, source filter, or currency.",
            color = SecondaryText,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun FirstRunCta(
    hasSmsPermission: Boolean,
    onRequestSmsPermission: () -> Unit,
    onSyncSms: () -> Unit,
    onAddManual: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFFFFCF7))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Welcome to Wallet",
            color = PrimaryText,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = if (hasSmsPermission)
                "We're ready to scan your inbox for Alta Bank SMS, or you can add transactions manually."
            else
                "Grant SMS access to auto-import Alta Bank card payments, or add transactions manually.",
            color = SecondaryText,
            fontSize = 13.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (hasSmsPermission) {
                Button(onClick = onSyncSms) { Text("Sync SMS now") }
            } else {
                Button(onClick = onRequestSmsPermission) { Text("Grant SMS access") }
            }
            TextButton(onClick = onAddManual) { Text("Add manually", color = PrimaryText) }
        }
    }
}

@Composable
private fun SelectionActionBar(
    count: Int,
    onClear: () -> Unit,
    onDelete: () -> Unit,
    onCategoryMenuToggle: () -> Unit,
    bulkCategoryMenuOpen: Boolean,
    onCategorySelected: (Category?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(PrimaryText)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onClear) {
                Icon(Icons.Outlined.Close, contentDescription = "Clear selection", tint = Color.White)
            }
            Text(text = "$count selected", color = Color.White, fontSize = 14.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box {
                TextButton(onClick = onCategoryMenuToggle) {
                    Text("Categorise", color = Color.White)
                }
                DropdownMenu(
                    expanded = bulkCategoryMenuOpen,
                    onDismissRequest = onCategoryMenuToggle
                ) {
                    DropdownMenuItem(
                        text = { Text("Clear category") },
                        onClick = { onCategorySelected(null) }
                    )
                    Category.entries.forEach { c ->
                        DropdownMenuItem(
                            text = { Text(c.label) },
                            onClick = { onCategorySelected(c) }
                        )
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete selected", tint = Color.White)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomRangePickerDialog(
    initialRange: LongRange?,
    onDismiss: () -> Unit,
    onConfirm: (LongRange) -> Unit
) {
    val pickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialRange?.first,
        initialSelectedEndDateMillis = initialRange?.last
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val start = pickerState.selectedStartDateMillis
                val end = pickerState.selectedEndDateMillis ?: start
                if (start != null && end != null) onConfirm(start..end)
            }) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    ) {
        DateRangePicker(state = pickerState, modifier = Modifier.height(520.dp))
    }
}

private fun activeRange(
    filter: PeriodFilter,
    customRange: LongRange?,
    nowMs: Long,
    offsetPeriods: Int
): LongRange? {
    return when (filter) {
        PeriodFilter.ALL -> null
        PeriodFilter.CUSTOM -> {
            val range = customRange ?: return null
            if (offsetPeriods == 0) range
            else {
                val span = range.last - range.first
                val start = range.first - (offsetPeriods * (span + 1))
                val end = range.last - (offsetPeriods * (span + 1))
                start..end
            }
        }

        PeriodFilter.THIS_MONTH -> {
            val zone = ZoneId.systemDefault()
            val today = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
            val targetMonth = java.time.YearMonth.from(today).minusMonths(offsetPeriods.toLong())
            val start = targetMonth.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val end = targetMonth.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
            start..end
        }

        else -> {
            val days = filter.days ?: return null
            val span = days * 24L * 60L * 60L * 1000L
            val end = nowMs - offsetPeriods * span
            val start = end - span
            start..end
        }
    }
}

private fun customRangeLabel(range: LongRange): String {
    val start = Instant.ofEpochMilli(range.first).atZone(ZoneId.systemDefault()).toLocalDate()
    val end = Instant.ofEpochMilli(range.last).atZone(ZoneId.systemDefault()).toLocalDate()
    val fmt = DateTimeFormatter.ofPattern("dd MMM")
    return "${start.format(fmt)} – ${end.format(fmt)}"
}

private fun totalsFor(transactions: List<TransactionUiModel>): PeriodTotals {
    val income = transactions.filter { it.amountMinor < 0 }.sumOf { -it.amountMinor }
    val spent = transactions.filter { it.amountMinor > 0 }.sumOf { it.amountMinor }
    return PeriodTotals(incomeMinor = income, spentMinor = spent)
}

private fun deltaPercent(current: Long, previous: Long): Double? {
    if (previous == 0L) return if (current == 0L) 0.0 else null
    return (current - previous) * 100.0 / previous
}

private fun formatDelta(delta: Double): String {
    val sign = if (delta >= 0) "+" else "−"
    return "$sign${"%.0f".format(abs(delta))}% vs prev"
}

private fun buildSubtitle(item: TransactionUiModel): String {
    val categoryLabel = item.category?.label ?: "Uncategorized"
    val sourceLabel = if (item.isManual) "Manual" else "Card"
    return "$categoryLabel · $sourceLabel"
}

private fun categoryShares(transactions: List<TransactionUiModel>): List<CategoryShare> {
    val total = transactions.sumOf { it.amountMinor }
    if (total <= 0L) return emptyList()

    return transactions
        .groupBy { it.category?.group }
        .map { (group, entries) ->
            val amount = entries.sumOf { it.amountMinor }
            val percentage = ((amount.toDouble() / total.toDouble()) * 100).toInt().coerceAtLeast(1)
            CategoryShare(
                label = group?.label ?: "Uncategorized",
                amountMinor = amount,
                percentage = percentage,
                color = group?.accent ?: Color(0xFFB4B2A9)
            )
        }
        .sortedByDescending { it.amountMinor }
        .take(5)
}

private fun buildSections(
    transactions: List<TransactionUiModel>,
    sortMode: SortMode
): List<DateSection> {
    val grouped = transactions.groupBy { localDate(it.occurredAt) }
    val orderedDates = when (sortMode) {
        SortMode.LATEST -> grouped.keys.sortedDescending()
        SortMode.OLDEST -> grouped.keys.sorted()
    }

    return orderedDates.map { date ->
        DateSection(
            title = sectionTitle(date),
            transactions = grouped.getValue(date)
        )
    }
}

private fun localDate(timestamp: Long): LocalDate {
    return Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}

private fun sectionTitle(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> "TODAY"
        today.minusDays(1) -> "YESTERDAY"
        else -> date.format(DateTimeFormatter.ofPattern("dd MMM yyyy")).uppercase()
    }
}

private fun formatTime(timestamp: Long): String {
    return Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("h:mm a"))
}

