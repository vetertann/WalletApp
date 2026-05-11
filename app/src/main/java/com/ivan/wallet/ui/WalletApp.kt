package com.ivan.wallet.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivan.wallet.ui.screens.BudgetsScreen
import com.ivan.wallet.ui.screens.ProfileScreen
import com.ivan.wallet.ui.screens.StatsScreen
import com.ivan.wallet.ui.screens.TransactionsScreen
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private enum class WalletTab(val title: String) {
    HOME("Home"),
    STATS("Stats"),
    BUDGETS("Budgets"),
    PROFILE("Profile")
}

private val WalletBackground = Color(0xFFF5F2EC)
private val WalletSurface = Color(0xFFFFFCF7)
private val WalletPrimaryText = Color(0xFF1F1E1B)
private val WalletSecondaryText = Color(0xFF7A736A)
private val WalletTertiaryText = Color(0xFFA8A097)

@Composable
fun WalletApp(
    viewModel: WalletViewModel,
    onRequestSmsPermission: () -> Unit,
    onImportSms: () -> Unit,
    onExportCsv: (Uri) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(WalletTab.HOME) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchOpen by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = WalletBackground,
        bottomBar = {
            NavigationBar(
                containerColor = WalletSurface,
                tonalElevation = 0.dp
            ) {
                val itemColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = WalletPrimaryText,
                    selectedTextColor = WalletPrimaryText,
                    unselectedIconColor = WalletTertiaryText,
                    unselectedTextColor = WalletTertiaryText,
                    indicatorColor = Color.Transparent
                )
                NavigationBarItem(
                    selected = selectedTab == WalletTab.HOME,
                    onClick = { selectedTab = WalletTab.HOME },
                    icon = { Icon(Icons.Outlined.Home, contentDescription = null) },
                    label = { Text(WalletTab.HOME.title) },
                    colors = itemColors
                )
                NavigationBarItem(
                    selected = selectedTab == WalletTab.STATS,
                    onClick = { selectedTab = WalletTab.STATS },
                    icon = { Icon(Icons.Outlined.Assessment, contentDescription = null) },
                    label = { Text(WalletTab.STATS.title) },
                    colors = itemColors
                )
                NavigationBarItem(
                    selected = selectedTab == WalletTab.BUDGETS,
                    onClick = { selectedTab = WalletTab.BUDGETS },
                    icon = { Icon(Icons.Outlined.PieChart, contentDescription = null) },
                    label = { Text(WalletTab.BUDGETS.title) },
                    colors = itemColors
                )
                NavigationBarItem(
                    selected = selectedTab == WalletTab.PROFILE,
                    onClick = { selectedTab = WalletTab.PROFILE },
                    icon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                    label = { Text(WalletTab.PROFILE.title) },
                    colors = itemColors
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(WalletBackground)
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                AppTopBar(
                    onSearchClick = {
                        if (selectedTab != WalletTab.HOME) selectedTab = WalletTab.HOME
                        isSearchOpen = true
                    }
                )
                when (selectedTab) {
                    WalletTab.HOME -> TransactionsScreen(
                        state = state,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        isSearchOpen = isSearchOpen,
                        onSearchOpenChange = { isSearchOpen = it },
                        onRequestSmsPermission = onRequestSmsPermission,
                        onSyncSms = onImportSms,
                        onSaveEntry = viewModel::saveEntry,
                        onDeleteTransaction = viewModel::deleteTransaction,
                        onDeleteTransactions = viewModel::deleteTransactions,
                        onAssignCategoryToMany = viewModel::assignCategoryToTransactions,
                        onOpenStats = { selectedTab = WalletTab.STATS }
                    )

                    WalletTab.STATS -> StatsScreen(state = state)

                    WalletTab.BUDGETS -> BudgetsScreen(
                        state = state,
                        onSaveBudget = viewModel::saveBudget,
                        onDeleteBudget = viewModel::deleteBudget
                    )

                    WalletTab.PROFILE -> ProfileScreen(
                        state = state,
                        onRequestSmsPermission = onRequestSmsPermission,
                        onImportSms = onImportSms,
                        onAddCurrency = viewModel::addSupportedCurrency,
                        onDeleteCurrency = viewModel::deleteSupportedCurrency,
                        onDeleteRule = viewModel::deleteRule,
                        onAddRule = viewModel::upsertRule,
                        onExportCsv = onExportCsv
                    )
                }
            }
        }
    }
}

@Composable
private fun AppTopBar(onSearchClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Hello",
                color = WalletSecondaryText,
                fontSize = 12.sp
            )
            Text(
                text = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM")),
                color = WalletPrimaryText,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
        }
        IconButton(onClick = onSearchClick) {
            Icon(
                Icons.Outlined.Search,
                contentDescription = "Search",
                tint = WalletPrimaryText
            )
        }
    }
}
