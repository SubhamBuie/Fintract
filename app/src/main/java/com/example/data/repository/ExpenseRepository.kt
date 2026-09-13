package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.database.AppDatabase
import com.example.data.database.DatabaseSeedData
import com.example.data.model.AccountEntity
import com.example.data.model.AppSettingsEntity
import com.example.data.model.BudgetEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.RecurringFrequency
import com.example.data.model.RecurringTransactionEntity
import com.example.data.model.SubcategoryEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionWithDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ExpenseRepository(private val db: AppDatabase) {

    val allTransactions: Flow<List<TransactionWithDetails>> =
        db.transactionDao().getAllTransactionsWithDetails()

    val allCategories: Flow<List<CategoryEntity>> =
        db.categoryDao().getAllCategories()

    val allSubcategories: Flow<List<SubcategoryEntity>> =
        db.categoryDao().getAllSubcategories()

    val allAccounts: Flow<List<AccountEntity>> =
        db.accountDao().getAllAccounts()

    val allBudgets: Flow<List<BudgetEntity>> =
        db.budgetDao().getAllBudgets()

    val allRecurring: Flow<List<RecurringTransactionEntity>> =
        db.recurringTransactionDao().getAllRecurring()

    val appSettings: Flow<AppSettingsEntity?> =
        db.appSettingsDao().getSettings()

    suspend fun initializeDatabase() = withContext(Dispatchers.IO) {
        DatabaseSeedData.seedInitialDataIfEmpty(db)
        processRecurringTransactions()
    }

    suspend fun addTransaction(transaction: TransactionEntity): Long = withContext(Dispatchers.IO) {
        db.withTransaction {
            val id = db.transactionDao().insertTransaction(transaction)
            // Update Account Balance
            val account = db.accountDao().getAccountById(transaction.accountId)
            if (account != null) {
                val newBalance = if (transaction.type == "INCOME") {
                    account.currentBalance + transaction.amount
                } else {
                    account.currentBalance - transaction.amount
                }
                db.accountDao().updateAccountBalance(account.id, newBalance)
            }
            id
        }
    }

    suspend fun updateTransaction(
        updatedTransaction: TransactionEntity,
        previousTransaction: TransactionEntity
    ) = withContext(Dispatchers.IO) {
        db.withTransaction {
            // Revert old transaction effect on old account
            val oldAccount = db.accountDao().getAccountById(previousTransaction.accountId)
            if (oldAccount != null) {
                val revertedBalance = if (previousTransaction.type == "INCOME") {
                    oldAccount.currentBalance - previousTransaction.amount
                } else {
                    oldAccount.currentBalance + previousTransaction.amount
                }
                db.accountDao().updateAccountBalance(oldAccount.id, revertedBalance)
            }

            // Apply new transaction effect on target account
            val newAccount = db.accountDao().getAccountById(updatedTransaction.accountId)
            if (newAccount != null) {
                val appliedBalance = if (updatedTransaction.type == "INCOME") {
                    newAccount.currentBalance + updatedTransaction.amount
                } else {
                    newAccount.currentBalance - updatedTransaction.amount
                }
                db.accountDao().updateAccountBalance(newAccount.id, appliedBalance)
            }

            db.transactionDao().updateTransaction(updatedTransaction)
        }
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
        db.withTransaction {
            // Revert account balance effect
            val account = db.accountDao().getAccountById(transaction.accountId)
            if (account != null) {
                val newBalance = if (transaction.type == "INCOME") {
                    account.currentBalance - transaction.amount
                } else {
                    account.currentBalance + transaction.amount
                }
                db.accountDao().updateAccountBalance(account.id, newBalance)
            }
            db.transactionDao().deleteTransaction(transaction)
        }
    }

    suspend fun duplicateTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
        val copy = transaction.copy(
            id = 0,
            date = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            notes = if (transaction.notes.isBlank()) "Copy" else "${transaction.notes} (Copy)"
        )
        addTransaction(copy)
    }

    // Category management
    suspend fun addCategory(category: CategoryEntity): Long = withContext(Dispatchers.IO) {
        db.categoryDao().insertCategory(category)
    }

    suspend fun updateCategory(category: CategoryEntity) = withContext(Dispatchers.IO) {
        db.categoryDao().updateCategory(category)
    }

    suspend fun deleteCategory(category: CategoryEntity) = withContext(Dispatchers.IO) {
        db.categoryDao().deleteCategory(category)
    }

    suspend fun addSubcategory(subcategory: SubcategoryEntity) = withContext(Dispatchers.IO) {
        db.categoryDao().insertSubcategory(subcategory)
    }

    suspend fun deleteSubcategory(subcategory: SubcategoryEntity) = withContext(Dispatchers.IO) {
        db.categoryDao().deleteSubcategory(subcategory)
    }

    // Account management
    suspend fun addAccount(account: AccountEntity): Long = withContext(Dispatchers.IO) {
        db.accountDao().insertAccount(account)
    }

    suspend fun updateAccount(account: AccountEntity) = withContext(Dispatchers.IO) {
        db.accountDao().updateAccount(account)
    }

    suspend fun deleteAccount(account: AccountEntity) = withContext(Dispatchers.IO) {
        db.accountDao().deleteAccount(account)
    }

    // Budget management
    suspend fun addBudget(budget: BudgetEntity): Long = withContext(Dispatchers.IO) {
        db.budgetDao().insertBudget(budget)
    }

    suspend fun updateBudget(budget: BudgetEntity) = withContext(Dispatchers.IO) {
        db.budgetDao().updateBudget(budget)
    }

    suspend fun deleteBudget(budget: BudgetEntity) = withContext(Dispatchers.IO) {
        db.budgetDao().deleteBudget(budget)
    }

    // Recurring transactions
    suspend fun addRecurring(recurring: RecurringTransactionEntity): Long = withContext(Dispatchers.IO) {
        db.recurringTransactionDao().insertRecurring(recurring)
    }

    suspend fun updateRecurring(recurring: RecurringTransactionEntity) = withContext(Dispatchers.IO) {
        db.recurringTransactionDao().updateRecurring(recurring)
    }

    suspend fun deleteRecurring(recurring: RecurringTransactionEntity) = withContext(Dispatchers.IO) {
        db.recurringTransactionDao().deleteRecurring(recurring)
    }

    suspend fun processRecurringTransactions() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val dueList = db.recurringTransactionDao().getActiveDueRecurring(now)
        for (recurring in dueList) {
            db.withTransaction {
                // Insert executed transaction
                val tx = TransactionEntity(
                    type = recurring.type,
                    amount = recurring.amount,
                    categoryId = recurring.categoryId,
                    subcategoryId = recurring.subcategoryId,
                    accountId = recurring.accountId,
                    paymentMethod = recurring.paymentMethod,
                    date = recurring.nextExecutionDate,
                    time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(recurring.nextExecutionDate)),
                    notes = "[Auto-Recurring] ${recurring.notes}",
                    createdAt = now,
                    updatedAt = now
                )
                db.transactionDao().insertTransaction(tx)

                // Update account
                val acc = db.accountDao().getAccountById(recurring.accountId)
                if (acc != null) {
                    val updatedBal = if (recurring.type == "INCOME") {
                        acc.currentBalance + recurring.amount
                    } else {
                        acc.currentBalance - recurring.amount
                    }
                    db.accountDao().updateAccountBalance(acc.id, updatedBal)
                }

                // Compute next execution date
                val cal = Calendar.getInstance()
                cal.timeInMillis = recurring.nextExecutionDate
                when (recurring.frequency) {
                    "DAILY" -> cal.add(Calendar.DAY_OF_YEAR, recurring.interval)
                    "WEEKLY" -> cal.add(Calendar.WEEK_OF_YEAR, recurring.interval)
                    "MONTHLY" -> cal.add(Calendar.MONTH, recurring.interval)
                    "YEARLY" -> cal.add(Calendar.YEAR, recurring.interval)
                    else -> cal.add(Calendar.MONTH, 1)
                }

                db.recurringTransactionDao().updateRecurring(
                    recurring.copy(nextExecutionDate = cal.timeInMillis)
                )
            }
        }
    }

    // App Settings
    suspend fun updateSettings(settings: AppSettingsEntity) = withContext(Dispatchers.IO) {
        db.appSettingsDao().insertOrUpdate(settings)
    }

    suspend fun resetAllData() = withContext(Dispatchers.IO) {
        db.withTransaction {
            db.clearAllTables()
            DatabaseSeedData.seedInitialDataIfEmpty(db)
        }
    }

    // CSV Export
    suspend fun exportTransactionsToCsv(transactions: List<TransactionWithDetails>): String = withContext(Dispatchers.Default) {
        val sb = StringBuilder()
        sb.append("ID,Type,Amount,Category,Subcategory,Account,Payment Method,Date,Time,Notes\n")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        for (item in transactions) {
            val t = item.transaction
            val dateStr = dateFormat.format(Date(t.date))
            val safeNotes = t.notes.replace("\"", "\"\"")
            sb.append("${t.id},${t.type},${t.amount},\"${item.category?.name ?: ""}\",\"${item.subcategory?.name ?: ""}\",\"${item.account?.name ?: ""}\",${t.paymentMethod},${dateStr},${t.time},\"${safeNotes}\"\n")
        }
        sb.toString()
    }

    // Backup & Restore (JSON)
    suspend fun createJsonBackup(transactions: List<TransactionWithDetails>): String = withContext(Dispatchers.Default) {
        val root = JSONObject()
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())
        val array = JSONArray()
        for (item in transactions) {
            val obj = JSONObject()
            obj.put("id", item.transaction.id)
            obj.put("type", item.transaction.type)
            obj.put("amount", item.transaction.amount)
            obj.put("categoryId", item.transaction.categoryId)
            obj.put("categoryName", item.category?.name ?: "")
            obj.put("accountId", item.transaction.accountId)
            obj.put("accountName", item.account?.name ?: "")
            obj.put("paymentMethod", item.transaction.paymentMethod)
            obj.put("date", item.transaction.date)
            obj.put("time", item.transaction.time)
            obj.put("notes", item.transaction.notes)
            array.put(obj)
        }
        root.put("transactions", array)
        root.toString(2)
    }
}
