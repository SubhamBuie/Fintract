package com.example.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import com.example.data.model.RecurringFrequency
import com.example.data.model.RecurringTransactionEntity
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    val recurringList by viewModel.allRecurring.collectAsState()
    val categories by viewModel.allCategories.collectAsState()
    val accounts by viewModel.allAccounts.collectAsState()
    val settings by viewModel.appSettings.collectAsState()

    var showAddEditDialog by remember { mutableStateOf(false) }
    var selectedRecurringForEdit by remember { mutableStateOf<RecurringTransactionEntity?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Column {
                    Text(
                        text = "Recurring Transactions",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Automated rent, subscriptions, salary, and bills",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (recurringList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No recurring transactions set. Tap + to add!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(recurringList, key = { it.id }) { r ->
                    val cat = categories.firstOrNull { it.id == r.categoryId }
                    val acc = accounts.firstOrNull { it.id == r.accountId }
                    val isExp = r.type == "EXPENSE"
                    val nextExecDateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(r.nextExecutionDate))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedRecurringForEdit = r
                                showAddEditDialog = true
                            },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = r.notes.ifBlank { cat?.name ?: "Recurring" },
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${cat?.name ?: ""} • ${acc?.name ?: ""} • ${r.frequency.lowercase().replaceFirstChar { it.uppercase() }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Next: $nextExecDateStr",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = String.format(Locale.getDefault(), "%s%s%,.2f", if (isExp) "-" else "+", settings.currency, r.amount),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isExp) ExpenseRed else IncomeGreen
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Switch(
                                    checked = r.isActive,
                                    onCheckedChange = { active ->
                                        viewModel.updateRecurring(r.copy(isActive = active))
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        FloatingActionButton(
            onClick = {
                selectedRecurringForEdit = null
                showAddEditDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 24.dp, end = 20.dp)
                .testTag("add_recurring_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Recurring")
        }
    }

    // Add / Edit Recurring Dialog
    if (showAddEditDialog) {
        val isEdit = selectedRecurringForEdit != null
        var notes by remember { mutableStateOf(selectedRecurringForEdit?.notes ?: "") }
        var type by remember { mutableStateOf(selectedRecurringForEdit?.type ?: "EXPENSE") }
        var amountStr by remember { mutableStateOf(selectedRecurringForEdit?.amount?.toString() ?: "") }
        var frequency by remember { mutableStateOf(selectedRecurringForEdit?.frequency ?: "MONTHLY") }
        var selectedCatId by remember {
            mutableStateOf(selectedRecurringForEdit?.categoryId ?: categories.firstOrNull()?.id ?: 1L)
        }
        var selectedAccId by remember {
            mutableStateOf(selectedRecurringForEdit?.accountId ?: accounts.firstOrNull()?.id ?: 1L)
        }
        var error by remember { mutableStateOf<String?>(null) }

        var freqMenuExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddEditDialog = false },
            title = { Text(if (isEdit) "Edit Recurring" else "New Recurring") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Description") },
                        placeholder = { Text("e.g. Netflix, Rent, Gym") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Amount (${settings.currency})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Frequency
                    ExposedDropdownMenuBox(
                        expanded = freqMenuExpanded,
                        onExpandedChange = { freqMenuExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = RecurringFrequency.values().firstOrNull { it.name == frequency }?.displayName ?: frequency,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Frequency") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = freqMenuExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = freqMenuExpanded,
                            onDismissRequest = { freqMenuExpanded = false }
                        ) {
                            RecurringFrequency.values().forEach { rf ->
                                DropdownMenuItem(
                                    text = { Text(rf.displayName) },
                                    onClick = {
                                        frequency = rf.name
                                        freqMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

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
                        val amount = amountStr.toDoubleOrNull()
                        if (amount == null || amount <= 0) {
                            error = "Please enter valid amount"
                            return@Button
                        }
                        if (isEdit) {
                            val existing = selectedRecurringForEdit!!
                            viewModel.updateRecurring(
                                existing.copy(
                                    notes = notes,
                                    amount = amount,
                                    frequency = frequency
                                )
                            )
                        } else {
                            val cal = Calendar.getInstance()
                            when (frequency) {
                                "DAILY" -> cal.add(Calendar.DAY_OF_YEAR, 1)
                                "WEEKLY" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                                "MONTHLY" -> cal.add(Calendar.MONTH, 1)
                                "YEARLY" -> cal.add(Calendar.YEAR, 1)
                            }
                            viewModel.addRecurring(
                                type = type,
                                amount = amount,
                                categoryId = selectedCatId,
                                subcategoryId = null,
                                accountId = selectedAccId,
                                paymentMethod = "BANK_TRANSFER",
                                notes = notes,
                                frequency = frequency,
                                interval = 1,
                                nextExecutionDate = cal.timeInMillis
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
                                viewModel.deleteRecurring(selectedRecurringForEdit!!)
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
