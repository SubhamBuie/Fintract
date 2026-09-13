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
import com.example.data.model.BudgetEntity
import com.example.data.model.BudgetWithSpending
import com.example.ui.components.BudgetCard
import com.example.ui.theme.ExpenseRed
import com.example.ui.viewmodel.ExpenseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    val budgetsWithSpending by viewModel.budgetsWithSpending.collectAsState()
    val categories by viewModel.allCategories.collectAsState()
    val settings by viewModel.appSettings.collectAsState()

    var showAddEditDialog by remember { mutableStateOf(false) }
    var selectedBudgetForEdit by remember { mutableStateOf<BudgetEntity?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Budgets & Limits",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Set thresholds and track spending limits",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (budgetsWithSpending.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No budgets set. Tap + to add a budget!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(budgetsWithSpending, key = { it.budget.id }) { item ->
                    BudgetCard(
                        item = item,
                        currency = settings.currency,
                        onEdit = {
                            selectedBudgetForEdit = item.budget
                            showAddEditDialog = true
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // FAB to add budget
        FloatingActionButton(
            onClick = {
                selectedBudgetForEdit = null
                showAddEditDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 24.dp, end = 20.dp)
                .testTag("add_budget_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Budget")
        }
    }

    // Add / Edit Budget Dialog
    if (showAddEditDialog) {
        val isEdit = selectedBudgetForEdit != null
        var name by remember { mutableStateOf(selectedBudgetForEdit?.name ?: "") }
        var amountStr by remember { mutableStateOf(selectedBudgetForEdit?.amount?.toString() ?: "") }
        var period by remember { mutableStateOf(selectedBudgetForEdit?.period ?: "MONTHLY") }
        var selectedCatId by remember { mutableStateOf(selectedBudgetForEdit?.categoryId) }
        var error by remember { mutableStateOf<String?>(null) }

        var periodMenuExpanded by remember { mutableStateOf(false) }
        var categoryMenuExpanded by remember { mutableStateOf(false) }

        val expenseCategories = categories.filter { it.type == "EXPENSE" }

        AlertDialog(
            onDismissRequest = { showAddEditDialog = false },
            title = { Text(if (isEdit) "Edit Budget" else "Create Budget") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Budget Name") },
                        placeholder = { Text("e.g. Dining Out, Monthly Cap") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Amount Limit (${settings.currency})") },
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Category Selector (null = Overall)
                    ExposedDropdownMenuBox(
                        expanded = categoryMenuExpanded,
                        onExpandedChange = { categoryMenuExpanded = it }
                    ) {
                        val currentCatName = if (selectedCatId == null) "Overall Budget (All Categories)" else expenseCategories.firstOrNull { it.id == selectedCatId }?.name ?: "Selected Category"
                        OutlinedTextField(
                            value = currentCatName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = categoryMenuExpanded,
                            onDismissRequest = { categoryMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Overall Budget (All Categories)") },
                                onClick = {
                                    selectedCatId = null
                                    categoryMenuExpanded = false
                                }
                            )
                            expenseCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    onClick = {
                                        selectedCatId = cat.id
                                        categoryMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Period Selector
                    ExposedDropdownMenuBox(
                        expanded = periodMenuExpanded,
                        onExpandedChange = { periodMenuExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = period.lowercase().replaceFirstChar { it.uppercase() },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Period") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = periodMenuExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = periodMenuExpanded,
                            onDismissRequest = { periodMenuExpanded = false }
                        ) {
                            listOf("WEEKLY", "MONTHLY", "YEARLY").forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p.lowercase().replaceFirstChar { it.uppercase() }) },
                                    onClick = {
                                        period = p
                                        periodMenuExpanded = false
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
                        if (name.isBlank()) {
                            error = "Please enter a name"
                            return@Button
                        }
                        if (amount == null || amount <= 0) {
                            error = "Please enter a valid amount limit"
                            return@Button
                        }

                        if (isEdit) {
                            val existing = selectedBudgetForEdit!!
                            viewModel.updateBudget(
                                existing.copy(
                                    name = name,
                                    amount = amount,
                                    categoryId = selectedCatId,
                                    period = period
                                )
                            )
                        } else {
                            viewModel.addBudget(name, selectedCatId, amount, period)
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
                                viewModel.deleteBudget(selectedBudgetForEdit!!)
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
