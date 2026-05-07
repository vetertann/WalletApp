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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.ivan.wallet.data.model.CategoryGroup
import com.ivan.wallet.ui.TransactionUiModel
import com.ivan.wallet.ui.WalletUiState
import com.ivan.wallet.util.toMoney
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val PrimaryText = Color(0xFF1F1E1B)
private val SecondaryText = Color(0xFF7A736A)
private val TertiaryText = Color(0xFFA8A097)
private val Surface = Color(0xFFFFFCF7)
private val DividerColor = Color(0xFFE6E0D5)
private val BarColor = Color(0xFF534AB7)
private val BarSelectedColor = Color(0xFF1F1E1B)

private enum class Grain { DAY, WEEK, MONTH }

private enum class StatsRange(
    val label: String,
    val days: Long,
    val grain: Grain
) {
    SEVEN("7d", 7, Grain.DAY),
    THIRTY("30d", 30, Grain.DAY),
    NINETY("90d", 90, Grain.WEEK),
    YEAR("Year", 365, Grain.MONTH)
}

private data class TimeBucket(
    val label: String,
    val total: Long,
    val transactions: List<TransactionUiModel>,
    val rangeDescription: String
)

@Composable
fun StatsScreen(state: WalletUiState) {
    var range by remember { mutableStateOf(StatsRange.THIRTY) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    val currency = state.selectedCurrency ?: state.supportedCurrencies.firstOrNull() ?: "RSD"
    val now = System.currentTimeMillis()
    val expenses = state.transactions.filter { it.currency == currency && it.amountMinor > 0 }
    val buckets = remember(expenses, range, now) { buildBuckets(expenses, range, now) }
    val filtered = buckets.flatMap { it.transactions }

    val totalSpent = buckets.sumOf { it.total }
    val activeDays = filtered.map { localDate(it.occurredAt) }.distinct().size.coerceAtLeast(1)
    val avgPerActiveDay = totalSpent / activeDays
    val avgPerCalendarDay = totalSpent / range.days.coerceAtLeast(1L)

    LaunchedEffect(range) { selectedIndex = null }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatsRange.entries.forEach { option ->
                    Pill(
                        label = option.label,
                        selected = option == range,
                        onClick = { range = option }
                    )
                }
            }
        }

        item {
            HeroNumbers(
                currency = currency,
                totalSpent = totalSpent,
                avgPerActiveDay = avgPerActiveDay,
                avgPerCalendarDay = avgPerCalendarDay
            )
        }

        item {
            SectionTitle("Spend over time")
            Spacer8()
            TrendChart(
                buckets = buckets,
                selectedIndex = selectedIndex,
                onBarTap = { idx -> selectedIndex = if (selectedIndex == idx) null else idx }
            )
        }

        val selected = selectedIndex
        if (selected != null && selected in buckets.indices) {
            item {
                BucketBreakdown(bucket = buckets[selected], currency = currency)
            }
        }

        item {
            SectionTitle("By category")
            Spacer8()
            CategoryListBars(transactions = filtered, currency = currency)
        }

        item {
            SectionTitle("Top merchants")
            Spacer8()
            TopMerchants(transactions = filtered, currency = currency)
        }
    }
}

@Composable
private fun Pill(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) PrimaryText else Color.Transparent
    val fg = if (selected) Color.White else SecondaryText
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .let { m -> if (!selected) m.border(0.5.dp, DividerColor, RoundedCornerShape(999.dp)) else m }
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
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = PrimaryText,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun Spacer8() {
    Box(modifier = Modifier.height(8.dp))
}

@Composable
private fun HeroNumbers(
    currency: String,
    totalSpent: Long,
    avgPerActiveDay: Long,
    avgPerCalendarDay: Long
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF2C2C2A))
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Total spent",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
            Text(
                text = totalSpent.toMoney(currency),
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Medium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniStat(
                    modifier = Modifier.weight(1f),
                    label = "Avg / active day",
                    value = avgPerActiveDay.toMoney(currency)
                )
                MiniStat(
                    modifier = Modifier.weight(1f),
                    label = "Avg / day",
                    value = avgPerCalendarDay.toMoney(currency)
                )
            }
        }
    }
}

@Composable
private fun MiniStat(modifier: Modifier = Modifier, label: String, value: String) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = label, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
            Text(text = value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun TrendChart(
    buckets: List<TimeBucket>,
    selectedIndex: Int?,
    onBarTap: (Int) -> Unit
) {
    val maxValue = (buckets.maxOfOrNull { it.total } ?: 0L).coerceAtLeast(1L)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .padding(14.dp)
    ) {
        if (buckets.all { it.total == 0L }) {
            Text(
                text = "No spending in this range yet.",
                color = SecondaryText,
                fontSize = 12.sp
            )
            return@Column
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            buckets.forEachIndexed { i, b ->
                BarBox(
                    modifier = Modifier.weight(1f),
                    ratio = b.total.toFloat() / maxValue.toFloat(),
                    isSelected = i == selectedIndex,
                    isEmpty = b.total == 0L,
                    onClick = { onBarTap(i) }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            buckets.forEachIndexed { i, b ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (b.label.isNotEmpty()) {
                        Text(
                            // Allow the label to overflow the narrow column;
                            // we only render labels every Nth cell so siblings
                            // are empty and there's room to spill into.
                            modifier = Modifier.wrapContentWidth(unbounded = true),
                            text = b.label,
                            color = if (i == selectedIndex) PrimaryText else TertiaryText,
                            fontSize = 10.sp,
                            fontWeight = if (i == selectedIndex) FontWeight.Medium else FontWeight.Normal,
                            softWrap = false,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BarBox(
    modifier: Modifier = Modifier,
    ratio: Float,
    isSelected: Boolean,
    isEmpty: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .fillMaxHeight(ratio.coerceIn(0.02f, 1f))
                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                .background(
                    when {
                        isSelected -> BarSelectedColor
                        isEmpty -> DividerColor
                        else -> BarColor
                    }
                )
        )
    }
}

@Composable
private fun BucketBreakdown(bucket: TimeBucket, currency: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = bucket.rangeDescription,
                    color = PrimaryText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${bucket.transactions.size} transactions",
                    color = SecondaryText,
                    fontSize = 11.sp
                )
            }
            Text(
                text = bucket.total.toMoney(currency),
                color = PrimaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (bucket.transactions.isEmpty()) {
            Text(
                text = "No spending in this period.",
                color = SecondaryText,
                fontSize = 12.sp
            )
            return@Column
        }

        val groups = bucket.transactions
            .groupBy { it.category?.group }
            .map { (g, txs) ->
                GroupSlice(
                    group = g,
                    amount = txs.sumOf { it.amountMinor },
                    items = txs
                )
            }
            .sortedByDescending { it.amount }
        val total = bucket.total.coerceAtLeast(1L)

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            groups.forEach { slice ->
                val color = slice.group?.accent ?: Color(0xFFB4B2A9)
                val label = slice.group?.label ?: "Uncategorized"
                val ratio = slice.amount.toFloat() / total.toFloat()
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(color, RoundedCornerShape(2.dp))
                            )
                            Text(text = label, color = PrimaryText, fontSize = 12.sp)
                        }
                        Text(
                            text = "${slice.amount.toMoney(currency)} · ${(ratio * 100).toInt()}%",
                            color = SecondaryText,
                            fontSize = 11.sp
                        )
                    }
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(DividerColor)
                    ) {
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(0f, 0f),
                            size = Size(size.width * ratio.coerceAtMost(1f), size.height),
                            cornerRadius = CornerRadius(0f)
                        )
                    }
                }
            }
        }

        // Top merchants within this bucket
        val topMerchants = bucket.transactions
            .groupBy { it.normalizedMerchant to it.merchant }
            .map { (key, txs) -> Triple(key.second, txs.size, txs.sumOf { it.amountMinor }) }
            .sortedByDescending { it.third }
            .take(3)

        if (topMerchants.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Top merchants",
                color = SecondaryText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                topMerchants.forEach { (merchant, count, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$merchant · ${count}×",
                            color = PrimaryText,
                            fontSize = 12.sp
                        )
                        Text(
                            text = value.toMoney(currency),
                            color = PrimaryText,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryListBars(transactions: List<TransactionUiModel>, currency: String) {
    var expandedGroup by remember { mutableStateOf<CategoryGroup?>(null) }

    val groupBuckets = transactions
        .groupBy { it.category?.group }
        .map { (group, txs) ->
            GroupBucket(
                group = group,
                amount = txs.sumOf { it.amountMinor },
                items = txs
            )
        }
        .sortedByDescending { it.amount }
    val total = groupBuckets.sumOf { it.amount }.coerceAtLeast(1L)

    if (groupBuckets.isEmpty()) {
        Text(text = "No data.", color = SecondaryText, fontSize = 12.sp)
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        groupBuckets.forEach { bucket ->
            val color = bucket.group?.accent ?: Color(0xFFB4B2A9)
            val label = bucket.group?.label ?: "Uncategorized"
            val ratio = bucket.amount.toFloat() / total.toFloat()
            val isExpanded = expandedGroup == bucket.group

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedGroup = if (isExpanded) null else bucket.group },
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(color, RoundedCornerShape(2.dp))
                        )
                        Text(
                            text = label,
                            color = PrimaryText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (isExpanded) "▾" else "›",
                            color = TertiaryText,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = bucket.amount.toMoney(currency),
                        color = PrimaryText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(DividerColor)
                ) {
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(0f, 0f),
                        size = Size(size.width * ratio, size.height),
                        cornerRadius = CornerRadius(0f)
                    )
                }
            }

            if (isExpanded) {
                CategoryChildren(
                    transactions = bucket.items,
                    parentTotal = bucket.amount,
                    currency = currency
                )
            }
        }
    }
}

@Composable
private fun CategoryChildren(
    transactions: List<TransactionUiModel>,
    parentTotal: Long,
    currency: String
) {
    val children = transactions
        .groupBy { it.category }
        .map { (category, txs) -> ChildBucket(category = category, amount = txs.sumOf { it.amountMinor }) }
        .sortedByDescending { it.amount }
    val safeParent = parentTotal.coerceAtLeast(1L)

    Column(
        modifier = Modifier
            .padding(start = 18.dp, top = 4.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        children.forEach { child ->
            val color = child.category?.accent ?: Color(0xFFB4B2A9)
            val label = child.category?.label ?: "Uncategorized"
            val ratio = child.amount.toFloat() / safeParent.toFloat()
            val pct = (ratio * 100).toInt().coerceAtLeast(1)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(color, RoundedCornerShape(2.dp))
                        )
                        Text(text = label, color = SecondaryText, fontSize = 12.sp)
                    }
                    Text(
                        text = "${child.amount.toMoney(currency)} · $pct%",
                        color = SecondaryText,
                        fontSize = 11.sp
                    )
                }
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(DividerColor)
                ) {
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(0f, 0f),
                        size = Size(size.width * ratio.coerceAtMost(1f), size.height),
                        cornerRadius = CornerRadius(0f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TopMerchants(transactions: List<TransactionUiModel>, currency: String) {
    val top = transactions
        .groupBy { it.normalizedMerchant to it.merchant }
        .map { (key, txs) -> Triple(key.second, txs.size, txs.sumOf { it.amountMinor }) }
        .sortedByDescending { it.third }
        .take(5)

    if (top.isEmpty()) {
        Text(text = "No merchants in this range.", color = SecondaryText, fontSize = 12.sp)
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        top.forEach { (merchant, count, value) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = merchant, color = PrimaryText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text(text = "$count payments", color = TertiaryText, fontSize = 11.sp)
                }
                Text(text = value.toMoney(currency), color = PrimaryText, fontSize = 13.sp)
            }
        }
    }
}

private data class GroupBucket(
    val group: CategoryGroup?,
    val amount: Long,
    val items: List<TransactionUiModel>
)

private data class GroupSlice(
    val group: CategoryGroup?,
    val amount: Long,
    val items: List<TransactionUiModel>
)

private data class ChildBucket(
    val category: Category?,
    val amount: Long
)

private fun buildBuckets(
    transactions: List<TransactionUiModel>,
    range: StatsRange,
    nowMs: Long
): List<TimeBucket> {
    val zone = ZoneId.systemDefault()
    val today = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
    val dayFormatter = DateTimeFormatter.ofPattern("EEEE, d MMM")
    val rangeFormatter = DateTimeFormatter.ofPattern("d MMM")
    val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

    return when (range.grain) {
        Grain.DAY -> {
            val days = range.days.toInt()
            (0 until days).map { offset ->
                val date = today.minusDays((days - 1 - offset).toLong())
                val txs = transactions.filter { localDate(it.occurredAt) == date }
                TimeBucket(
                    label = labelForDay(range, date, offset, days),
                    total = txs.sumOf { it.amountMinor },
                    transactions = txs,
                    rangeDescription = date.format(dayFormatter)
                )
            }
        }

        Grain.WEEK -> {
            val weeks = ((range.days + 6) / 7).toInt()
            (0 until weeks).map { offset ->
                val weekEnd = today.minusDays(((weeks - 1 - offset) * 7).toLong())
                val weekStart = weekEnd.minusDays(6)
                val txs = transactions.filter {
                    val d = localDate(it.occurredAt)
                    !d.isBefore(weekStart) && !d.isAfter(weekEnd)
                }
                TimeBucket(
                    label = if (offset % 2 == 0 || offset == weeks - 1) {
                        weekStart.format(rangeFormatter)
                    } else "",
                    total = txs.sumOf { it.amountMinor },
                    transactions = txs,
                    rangeDescription = "${weekStart.format(rangeFormatter)} – ${weekEnd.format(rangeFormatter)}"
                )
            }
        }

        Grain.MONTH -> {
            val firstMonth = YearMonth.from(today).minusMonths(11)
            (0 until 12).map { offset ->
                val month = firstMonth.plusMonths(offset.toLong())
                val txs = transactions.filter {
                    YearMonth.from(localDate(it.occurredAt)) == month
                }
                TimeBucket(
                    label = month.format(DateTimeFormatter.ofPattern("MMM")),
                    total = txs.sumOf { it.amountMinor },
                    transactions = txs,
                    rangeDescription = month.format(monthFormatter)
                )
            }
        }
    }
}

private fun labelForDay(
    range: StatsRange,
    date: LocalDate,
    index: Int,
    total: Int
): String {
    return when (range) {
        StatsRange.SEVEN -> date.format(DateTimeFormatter.ofPattern("EEE"))
        StatsRange.THIRTY -> {
            val isLast = index == total - 1
            if (index % 5 == 0 || isLast) date.dayOfMonth.toString() else ""
        }
        else -> ""
    }
}

private fun localDate(timestamp: Long): LocalDate {
    return Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
}
