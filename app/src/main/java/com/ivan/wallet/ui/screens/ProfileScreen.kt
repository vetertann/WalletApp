package com.ivan.wallet.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivan.wallet.data.model.Category
import com.ivan.wallet.ui.RuleUiModel
import com.ivan.wallet.ui.WalletUiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val PrimaryText = Color(0xFF1F1E1B)
private val SecondaryText = Color(0xFF7A736A)
private val Surface = Color(0xFFFFFCF7)
private val DividerColor = Color(0xFFE6E0D5)

@Composable
fun ProfileScreen(
    state: WalletUiState,
    onRequestSmsPermission: () -> Unit,
    onImportSms: () -> Unit,
    onAddCurrency: (String) -> Unit,
    onDeleteCurrency: (String) -> Unit,
    onDeleteRule: (Long) -> Unit,
    onAddRule: (String, Category) -> Unit,
    onExportCsv: (Uri) -> Unit
) {
    var newCurrency by remember { mutableStateOf("") }
    var ruleDialog by remember { mutableStateOf(false) }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) onExportCsv(uri)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(title = "SMS import") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Import all bank SMS from sender Alta_Banka. Re-running is safe — duplicates are skipped.",
                        color = SecondaryText,
                        fontSize = 12.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (state.hasSmsPermission) {
                            Button(onClick = onImportSms) { Text("Import all bank SMS") }
                        } else {
                            Button(onClick = onRequestSmsPermission) { Text("Grant SMS access") }
                        }
                    }
                    state.lastImportSummary?.let {
                        Text(it, color = Color(0xFF8C2F39), fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            Card(title = "Export") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Save all transactions as a CSV — useful for backups or spreadsheets.",
                        color = SecondaryText,
                        fontSize = 12.sp
                    )
                    Button(onClick = {
                        val name = "wallet-${LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)}.csv"
                        csvLauncher.launch(name)
                    }) {
                        Text("Export CSV")
                    }
                    state.exportMessage?.let {
                        Text(it, color = SecondaryText, fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            Card(title = "Currencies") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Add or remove currencies for manual entries.",
                        color = SecondaryText,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = newCurrency,
                        onValueChange = { newCurrency = it.uppercase() },
                        label = { Text("Add currency code") },
                        supportingText = { Text("3 letters, e.g. GBP") },
                        singleLine = true
                    )
                    Button(onClick = {
                        onAddCurrency(newCurrency)
                        newCurrency = ""
                    }) { Text("Add currency") }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        state.supportedCurrencies.forEach { code ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = code, fontWeight = FontWeight.SemiBold)
                                TextButton(onClick = { onDeleteCurrency(code) }) {
                                    Text("Remove")
                                }
                            }
                        }
                    }
                    state.currencySettingsMessage?.let {
                        Text(it, color = Color(0xFF8C2F39), fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            Card(title = "Category rules") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Auto-categorise merchants by substring match.",
                        color = SecondaryText,
                        fontSize = 12.sp
                    )
                    TextButton(onClick = { ruleDialog = true }) {
                        Text("Add rule", color = PrimaryText)
                    }
                    if (state.rules.isEmpty()) {
                        Text(
                            text = "No rules yet. Tap a transaction's category in the list to teach one.",
                            color = SecondaryText,
                            fontSize = 12.sp
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            state.rules.forEach { rule ->
                                RuleRow(rule = rule, onDelete = { onDeleteRule(rule.id) })
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(title = "About") {
                Text(
                    text = "Wallet stores everything locally. SMS messages never leave the device.",
                    color = SecondaryText,
                    fontSize = 12.sp
                )
            }
        }
    }

    if (ruleDialog) {
        AddRuleDialog(
            onDismiss = { ruleDialog = false },
            onSave = { key, cat ->
                onAddRule(key, cat)
                ruleDialog = false
            }
        )
    }
}

@Composable
private fun Card(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = title, color = PrimaryText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        content()
    }
}

@Composable
private fun RuleRow(rule: RuleUiModel, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.padding(end = 8.dp)) {
            Text(rule.matchKey, color = PrimaryText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(rule.category.label, color = rule.category.accent, fontSize = 12.sp)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Outlined.Delete, contentDescription = "Delete rule", tint = SecondaryText)
        }
    }
}

@Composable
private fun AddRuleDialog(
    onDismiss: () -> Unit,
    onSave: (String, Category) -> Unit
) {
    var matchKey by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<Category?>(null) }
    var isMenuOpen by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add category rule", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = matchKey,
                    onValueChange = { matchKey = it },
                    label = { Text("Match (substring)") },
                    supportingText = { Text("e.g. WOLT — case insensitive") },
                    singleLine = true
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isMenuOpen = true },
                        value = category?.label ?: "Pick a category",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        singleLine = true
                    )
                    DropdownMenu(
                        expanded = isMenuOpen,
                        onDismissRequest = { isMenuOpen = false }
                    ) {
                        Category.entries.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c.label) },
                                onClick = {
                                    category = c
                                    isMenuOpen = false
                                }
                            )
                        }
                    }
                }
                error?.let { Text(it, color = Color(0xFF8C2F39), fontSize = 12.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cat = category
                if (matchKey.isBlank()) error = "Enter a substring."
                else if (cat == null) error = "Pick a category."
                else onSave(matchKey, cat)
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
