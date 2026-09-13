package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AccountEntity
import com.example.data.model.AccountType
import com.example.ui.components.AccountCard
import com.example.ui.theme.ExpenseRed
import com.example.ui.viewmodel.ExpenseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    val accounts by viewModel.allAccounts.collectAsState()
    val settings by viewModel.appSettings.collectAsState()

    var showAddEditDialog by remember { mutableStateOf(false) }
    var selectedAccountForEdit by remember { mutableStateOf<AccountEntity?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Column {
                    Text(
                        text = "Accounts Management",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Cash, Bank, Cards, and Digital Wallets",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (accounts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No accounts found. Tap + to add an account!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(accounts, key = { it.id }) { acc ->
                    AccountCard(
                        account = acc,
                        onEdit = {
                            selectedAccountForEdit = acc
                            showAddEditDialog = true
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // FAB to add account
        FloatingActionButton(
            onClick = {
                selectedAccountForEdit = null
                showAddEditDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 24.dp, end = 20.dp)
                .testTag("add_account_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Account")
        }
    }

    // Add / Edit Account Dialog
    if (showAddEditDialog) {
        val isEdit = selectedAccountForEdit != null
        var name by remember { mutableStateOf(selectedAccountForEdit?.name ?: "") }
        var type by remember { mutableStateOf(selectedAccountForEdit?.type ?: "BANK") }
        var balanceStr by remember {
            mutableStateOf(
                selectedAccountForEdit?.initialBalance?.toString() ?: "0.00"
            )
        }
        var error by remember { mutableStateOf<String?>(null) }
        var typeMenuExpanded by remember { mutableStateOf(false) }

        val icon = when (type) {
            "CASH" -> "payments"
            "CREDIT_CARD", "DEBIT_CARD" -> "credit_card"
            "WALLET" -> "account_balance_wallet"
            else -> "account_balance"
        }

        AlertDialog(
            onDismissRequest = { showAddEditDialog = false },
            title = { Text(if (isEdit) "Edit Account" else "Add Account") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Account Name") },
                        placeholder = { Text("e.g. Chase Checking, Cash Wallet") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Type Selector
                    ExposedDropdownMenuBox(
                        expanded = typeMenuExpanded,
                        onExpandedChange = { typeMenuExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = AccountType.values().firstOrNull { it.name == type }?.displayName ?: type,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Account Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = typeMenuExpanded,
                            onDismissRequest = { typeMenuExpanded = false }
                        ) {
                            AccountType.values().forEach { at ->
                                DropdownMenuItem(
                                    text = { Text(at.displayName) },
                                    onClick = {
                                        type = at.name
                                        typeMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = balanceStr,
                        onValueChange = { balanceStr = it },
                        label = { Text("Initial Balance (${settings.currency})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (error != null) {
                        Text(
                            text = error ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val bal = balanceStr.toDoubleOrNull()
                        if (name.isBlank()) {
                            error = "Please enter an account name"
                            return@Button
                        }
                        if (bal == null) {
                            error = "Please enter a valid balance"
                            return@Button
                        }

                        if (isEdit) {
                            val existing = selectedAccountForEdit!!
                            viewModel.updateAccount(
                                existing.copy(
                                    name = name,
                                    type = type,
                                    icon = icon,
                                    initialBalance = bal
                                )
                            )
                        } else {
                            viewModel.addAccount(
                                name = name,
                                type = type,
                                initialBalance = bal,
                                currency = settings.currency,
                                icon = icon
                            )
                        }
                        showAddEditDialog = false
                    }
                ) {
                    Text(if (isEdit) "Save" else "Create")
                }
            },
            dismissButton = {
                Row {
                    if (isEdit) {
                        TextButton(
                            onClick = {
                                viewModel.deleteAccount(selectedAccountForEdit!!)
                                showAddEditDialog = false
                            }
                        ) {
                            Text("Delete", color = ExpenseRed)
                        }
                    }
                    TextButton(onClick = { showAddEditDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}
