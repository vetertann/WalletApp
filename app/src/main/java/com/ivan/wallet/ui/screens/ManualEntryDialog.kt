package com.ivan.wallet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivan.wallet.data.model.Category
import com.ivan.wallet.data.model.CategoryGroup
import com.ivan.wallet.ui.ManualEntryDraft
import com.ivan.wallet.ui.TransactionUiModel
import com.ivan.wallet.ui.WalletViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntryDialog(
    existing: TransactionUiModel?,
    availableCurrencies: List<String>,
    onDismiss: () -> Unit,
    onSave: (ManualEntryDraft) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val isEditing = existing != null
    var merchant by remember { mutableStateOf(existing?.merchant ?: "") }
    var amount by remember {
        mutableStateOf(
            existing?.let {
                val minor = abs(it.amountMinor)
                "%.2f".format(minor / 100.0)
            } ?: ""
        )
    }
    var isIncome by remember { mutableStateOf((existing?.amountMinor ?: 0L) < 0L) }
    var currency by remember(availableCurrencies) {
        mutableStateOf(
            existing?.currency ?: when {
                "RSD" in availableCurrencies -> "RSD"
                availableCurrencies.isNotEmpty() -> availableCurrencies.first()
                else -> "RSD"
            }
        )
    }
    var isCurrencyMenuOpen by remember { mutableStateOf(false) }

    val initialDate = remember {
        existing?.let {
            Instant.ofEpochMilli(it.occurredAt).atZone(ZoneId.systemDefault()).toLocalDate()
        } ?: LocalDate.now()
    }
    val initialTime = remember {
        existing?.let {
            Instant.ofEpochMilli(it.occurredAt).atZone(ZoneId.systemDefault()).toLocalTime()
                .withSecond(0).withNano(0)
        } ?: LocalTime.now().withSecond(0).withNano(0)
    }
    var date by remember { mutableStateOf(initialDate) }
    var time by remember { mutableStateOf(initialTime) }
    var category by remember { mutableStateOf<Category?>(existing?.category) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Keep isIncome in sync with the chosen category. Picking an income-group
    // category forces the toggle on; flipping the toggle clears a now-mismatched
    // category so the user reselects from the right group.
    LaunchedEffect(category) {
        if (category?.isIncome == true && !isIncome) isIncome = true
        if (category?.isIncome == false && isIncome) isIncome = false
    }
    LaunchedEffect(isIncome) {
        val current = category
        if (current != null && current.isIncome != isIncome) {
            category = null
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        date = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showTimePicker) {
        val pickerState = rememberTimePickerState(
            initialHour = time.hour,
            initialMinute = time.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    time = LocalTime.of(pickerState.hour, pickerState.minute)
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            title = { Text("Pick time") },
            text = { TimePicker(state = pickerState) }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (isEditing) "Edit transaction" else "Add transaction",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isEditing) "Edits apply only to this entry. Category changes also update the merchant rule."
                    else "Manual entries use the same reports and category learning as SMS imports."
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Income", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (isIncome) "Counts toward Income tile"
                            else "Counts toward Spent tile",
                            color = Color(0xFF6D6D6D)
                        )
                    }
                    Switch(checked = isIncome, onCheckedChange = { isIncome = it })
                }
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Merchant") },
                    singleLine = true
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    singleLine = true
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = currency,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Currency") },
                        singleLine = true
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { isCurrencyMenuOpen = true }
                    )
                    DropdownMenu(
                        expanded = isCurrencyMenuOpen,
                        onDismissRequest = { isCurrencyMenuOpen = false }
                    ) {
                        availableCurrencies.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    currency = option
                                    isCurrencyMenuOpen = false
                                }
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PickerField(
                        modifier = Modifier.weight(1f),
                        label = "Date",
                        value = date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                        onClick = { showDatePicker = true }
                    )
                    PickerField(
                        modifier = Modifier.weight(1f),
                        label = "Time",
                        value = time.format(DateTimeFormatter.ofPattern("HH:mm")),
                        onClick = { showTimePicker = true }
                    )
                }
                Text(text = "Category", fontWeight = FontWeight.SemiBold)
                CategoryPicker(
                    selected = category,
                    onSelect = { category = it },
                    visibleGroups = if (isIncome) listOf(CategoryGroup.INCOME)
                    else CategoryGroup.entries.filter { it != CategoryGroup.INCOME },
                    allowNone = !isIncome
                )
                if (isEditing && onDelete != null) {
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (confirmDelete) onDelete() else confirmDelete = true
                        }
                    ) {
                        Text(
                            text = if (confirmDelete) "Tap again to confirm delete" else "Delete transaction",
                            color = Color(0xFFB3261E)
                        )
                    }
                }
                errorMessage?.let { message ->
                    Text(text = message, color = Color(0xFFB3261E))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val result = WalletViewModel.buildManualEntryDraft(
                        id = existing?.id,
                        merchant = merchant,
                        amount = amount,
                        currency = currency,
                        date = date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                        time = time.format(DateTimeFormatter.ofPattern("HH:mm")),
                        category = category,
                        isIncome = isIncome,
                        allowedCurrencies = availableCurrencies
                    )

                    result
                        .onSuccess {
                            errorMessage = null
                            onSave(it)
                        }
                        .onFailure {
                            errorMessage = it.message ?: "Entry is invalid."
                        }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PickerField(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Box(modifier = modifier) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            singleLine = true
        )
        // Overlay captures taps reliably; the read-only field beneath would
        // otherwise swallow the click via its inner BasicTextField.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(onClick = onClick)
        )
    }
}


