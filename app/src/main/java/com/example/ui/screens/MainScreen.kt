package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.TransactionEntity
import com.example.ui.viewmodel.ExpenseViewModel

enum class MainTab(val title: String) {
    DASHBOARD("Dashboard"),
    TRANSACTIONS("Transactions"),
    ANALYTICS("Analytics"),
    SETTINGS("Settings")
}

enum class SubScreen {
    NONE,
    BUDGETS,
    ACCOUNTS,
    CATEGORIES,
    RECURRING
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.appSettings.collectAsState()
    val categories by viewModel.allCategories.collectAsState()
    val subcategories by viewModel.allSubcategories.collectAsState()
    val accounts by viewModel.allAccounts.collectAsState()

    var isUnlocked by remember { mutableStateOf(!settings.pinEnabled) }
    var currentTab by remember { mutableStateOf(MainTab.DASHBOARD) }
    var currentSubScreen by remember { mutableStateOf(SubScreen.NONE) }

    var showAddEditSheet by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<TransactionEntity?>(null) }

    // If PIN is enabled and not unlocked yet, show PIN lock
    if (settings.pinEnabled && !isUnlocked && settings.pinCode.isNotBlank()) {
        PinLockScreen(
            correctPin = settings.pinCode,
            onUnlocked = { isUnlocked = true }
        )
        return
    }

    // Intercept back presses when on sub-screens
    BackHandler(enabled = currentSubScreen != SubScreen.NONE) {
        currentSubScreen = SubScreen.NONE
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (currentSubScreen != SubScreen.NONE) {
                TopAppBar(
                    title = {
                        Text(
                            text = when (currentSubScreen) {
                                SubScreen.BUDGETS -> "Budgets"
                                SubScreen.ACCOUNTS -> "Accounts"
                                SubScreen.CATEGORIES -> "Categories"
                                SubScreen.RECURRING -> "Recurring"
                                else -> ""
                            },
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { currentSubScreen = SubScreen.NONE }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        bottomBar = {
            if (currentSubScreen == SubScreen.NONE) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                        label = { Text("Dashboard") },
                        selected = currentTab == MainTab.DASHBOARD,
                        onClick = { currentTab = MainTab.DASHBOARD },
                        modifier = Modifier.testTag("nav_dashboard")
                    )

                    NavigationBarItem(
                        icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Transactions") },
                        label = { Text("Transactions") },
                        selected = currentTab == MainTab.TRANSACTIONS,
                        onClick = { currentTab = MainTab.TRANSACTIONS },
                        modifier = Modifier.testTag("nav_transactions")
                    )

                    NavigationBarItem(
                        icon = { Icon(Icons.Default.BarChart, contentDescription = "Analytics") },
                        label = { Text("Analytics") },
                        selected = currentTab == MainTab.ANALYTICS,
                        onClick = { currentTab = MainTab.ANALYTICS },
                        modifier = Modifier.testTag("nav_analytics")
                    )

                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                        selected = currentTab == MainTab.SETTINGS,
                        onClick = { currentTab = MainTab.SETTINGS },
                        modifier = Modifier.testTag("nav_settings")
                    )
                }
            }
        },
        floatingActionButton = {
            if (currentSubScreen == SubScreen.NONE && currentTab != MainTab.SETTINGS) {
                FloatingActionButton(
                    onClick = {
                        transactionToEdit = null
                        showAddEditSheet = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .testTag("main_add_transaction_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Transaction", modifier = Modifier.size(28.dp))
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentSubScreen) {
                SubScreen.BUDGETS -> BudgetsScreen(viewModel = viewModel)
                SubScreen.ACCOUNTS -> AccountsScreen(viewModel = viewModel)
                SubScreen.CATEGORIES -> CategoriesScreen(viewModel = viewModel)
                SubScreen.RECURRING -> RecurringScreen(viewModel = viewModel)
                SubScreen.NONE -> {
                    when (currentTab) {
                        MainTab.DASHBOARD -> DashboardScreen(
                            viewModel = viewModel,
                            onNavigateToTransactions = { currentTab = MainTab.TRANSACTIONS },
                            onOpenAddTransaction = {
                                transactionToEdit = null
                                showAddEditSheet = true
                            },
                            onEditTransaction = { tx ->
                                transactionToEdit = tx
                                showAddEditSheet = true
                            }
                        )

                        MainTab.TRANSACTIONS -> TransactionsScreen(
                            viewModel = viewModel,
                            onEditTransaction = { tx ->
                                transactionToEdit = tx
                                showAddEditSheet = true
                            }
                        )

                        MainTab.ANALYTICS -> AnalyticsScreen(
                            viewModel = viewModel
                        )

                        MainTab.SETTINGS -> SettingsScreen(
                            viewModel = viewModel,
                            onNavigateToBudgets = { currentSubScreen = SubScreen.BUDGETS },
                            onNavigateToAccounts = { currentSubScreen = SubScreen.ACCOUNTS },
                            onNavigateToCategories = { currentSubScreen = SubScreen.CATEGORIES },
                            onNavigateToRecurring = { currentSubScreen = SubScreen.RECURRING }
                        )
                    }
                }
            }
        }

        // Add / Edit Transaction Bottom Sheet
        if (showAddEditSheet) {
            AddEditTransactionBottomSheet(
                initialTransaction = transactionToEdit,
                categories = categories,
                subcategories = subcategories,
                accounts = accounts,
                currency = settings.currency,
                onDismiss = {
                    showAddEditSheet = false
                    transactionToEdit = null
                },
                onSave = { type, amount, categoryId, subcategoryId, accountId, paymentMethod, date, time, notes ->
                    if (transactionToEdit != null) {
                        viewModel.updateTransaction(
                            updated = transactionToEdit!!.copy(
                                type = type,
                                amount = amount,
                                categoryId = categoryId,
                                subcategoryId = subcategoryId,
                                accountId = accountId,
                                paymentMethod = paymentMethod,
                                date = date,
                                time = time,
                                notes = notes
                            ),
                            original = transactionToEdit!!
                        )
                    } else {
                        viewModel.addTransaction(
                            type = type,
                            amount = amount,
                            categoryId = categoryId,
                            subcategoryId = subcategoryId,
                            accountId = accountId,
                            paymentMethod = paymentMethod,
                            date = date,
                            time = time,
                            notes = notes
                        )
                    }
                    showAddEditSheet = false
                    transactionToEdit = null
                }
            )
        }
    }
}
