package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.AccountEntity
import com.example.data.model.AppSettingsEntity
import com.example.data.model.BudgetEntity
import com.example.data.model.BudgetPeriod
import com.example.data.model.BudgetWithSpending
import com.example.data.model.CategoryEntity
import com.example.data.model.DateRangeFilter
import com.example.data.model.RecurringTransactionEntity
import com.example.data.model.SubcategoryEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.model.TransactionWithDetails
import com.example.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CategorySpending(
    val categoryId: Long,
    val categoryName: String,
    val color: Long,
    val icon: String,
    val amount: Double,
    val percentage: Float
)

data class DailySpending(
    val dateLabel: String,
    val dayOfMonth: Int,
    val amount: Double,
    val timestamp: Long
)

data class MonthlyComparison(
    val monthLabel: String,
    val income: Double,
    val expense: Double
)

enum class TransactionSortOption(val displayName: String) {
    DATE_DESC("Newest First"),
    DATE_ASC("Oldest First"),
    AMOUNT_DESC("Highest Amount"),
    AMOUNT_ASC("Lowest Amount")
}

data class TransactionFilterState(
    val query: String = "",
    val type: TransactionType? = null,
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    val sortOption: TransactionSortOption = TransactionSortOption.DATE_DESC
)

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ExpenseRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = ExpenseRepository(db)
        viewModelScope.launch {
            repository.initializeDatabase()
        }
    }

    val allTransactions: StateFlow<List<TransactionWithDetails>> =
        repository.allTransactions.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val allCategories: StateFlow<List<CategoryEntity>> =
        repository.allCategories.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val allSubcategories: StateFlow<List<SubcategoryEntity>> =
        repository.allSubcategories.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val allAccounts: StateFlow<List<AccountEntity>> =
        repository.allAccounts.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val allBudgets: StateFlow<List<BudgetEntity>> =
        repository.allBudgets.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val allRecurring: StateFlow<List<RecurringTransactionEntity>> =
        repository.allRecurring.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val appSettings: StateFlow<AppSettingsEntity> =
        repository.appSettings
            .combine(MutableStateFlow(AppSettingsEntity())) { settings, defaultVal ->
                settings ?: defaultVal
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                AppSettingsEntity()
            )

    // Filter states
    private val _dateRangeFilter = MutableStateFlow(DateRangeFilter.THIS_MONTH)
    val dateRangeFilter: StateFlow<DateRangeFilter> = _dateRangeFilter.asStateFlow()

    private val _customDateRange = MutableStateFlow<Pair<Long, Long>?>(null)
    val customDateRange: StateFlow<Pair<Long, Long>?> = _customDateRange.asStateFlow()

    private val _transactionFilters = MutableStateFlow(TransactionFilterState())
    val transactionFilters: StateFlow<TransactionFilterState> = _transactionFilters.asStateFlow()

    // App Security Lock State
    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    init {
        viewModelScope.launch {
            appSettings.collect { settings ->
                if (settings.pinEnabled && settings.pinCode.isNotBlank()) {
                    _isLocked.value = true
                }
            }
        }
    }

    fun unlockWithPin(pin: String): Boolean {
        val currentPin = appSettings.value.pinCode
        if (pin == currentPin || !appSettings.value.pinEnabled) {
            _isLocked.value = false
            return true
        }
        return false
    }

    fun lockApp() {
        if (appSettings.value.pinEnabled) {
            _isLocked.value = true
        }
    }

    fun setDateRangeFilter(filter: DateRangeFilter, customRange: Pair<Long, Long>? = null) {
        _dateRangeFilter.value = filter
        if (customRange != null) {
            _customDateRange.value = customRange
        }
    }

    fun updateTransactionFilters(update: (TransactionFilterState) -> TransactionFilterState) {
        _transactionFilters.value = update(_transactionFilters.value)
    }

    fun resetTransactionFilters() {
        _transactionFilters.value = TransactionFilterState()
    }

    // Helper: calculate start and end bounds for date range
    fun getDateRangeBounds(filter: DateRangeFilter): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        val now = System.currentTimeMillis()
        return when (filter) {
            DateRangeFilter.TODAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                Pair(start, cal.timeInMillis)
            }
            DateRangeFilter.THIS_WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.add(Calendar.DAY_OF_WEEK, 6)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                Pair(start, cal.timeInMillis)
            }
            DateRangeFilter.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                Pair(start, cal.timeInMillis)
            }
            DateRangeFilter.THIS_YEAR -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.MONTH, Calendar.DECEMBER)
                cal.set(Calendar.DAY_OF_MONTH, 31)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                Pair(start, cal.timeInMillis)
            }
            DateRangeFilter.ALL_TIME -> Pair(0L, Long.MAX_VALUE)
            DateRangeFilter.CUSTOM -> _customDateRange.value ?: Pair(0L, now)
        }
    }

    // Filtered transactions for Transactions Tab
    val filteredTransactions: StateFlow<List<TransactionWithDetails>> = combine(
        allTransactions,
        _transactionFilters,
        _dateRangeFilter,
        _customDateRange
    ) { txList, filter, rangeFilter, _ ->
        val (start, end) = getDateRangeBounds(rangeFilter)
        var list = txList.filter { it.transaction.date in start..end }

        // Query filter (searches notes, category name, account name, amount)
        if (filter.query.isNotBlank()) {
            val q = filter.query.trim().lowercase(Locale.getDefault())
            list = list.filter {
                it.transaction.notes.lowercase(Locale.getDefault()).contains(q) ||
                        (it.category?.name?.lowercase(Locale.getDefault())?.contains(q) == true) ||
                        (it.account?.name?.lowercase(Locale.getDefault())?.contains(q) == true) ||
                        it.transaction.amount.toString().contains(q)
            }
        }

        // Type filter
        if (filter.type != null) {
            val typeStr = filter.type.name
            list = list.filter { it.transaction.type == typeStr }
        }

        // Category filter
        if (filter.categoryId != null) {
            list = list.filter { it.transaction.categoryId == filter.categoryId }
        }

        // Account filter
        if (filter.accountId != null) {
            list = list.filter { it.transaction.accountId == filter.accountId }
        }

        // Min & Max Amount
        if (filter.minAmount != null) {
            list = list.filter { it.transaction.amount >= filter.minAmount }
        }
        if (filter.maxAmount != null) {
            list = list.filter { it.transaction.amount <= filter.maxAmount }
        }

        // Sort
        when (filter.sortOption) {
            TransactionSortOption.DATE_DESC -> list.sortedWith(compareByDescending<TransactionWithDetails> { it.transaction.date }.thenByDescending { it.transaction.createdAt })
            TransactionSortOption.DATE_ASC -> list.sortedWith(compareBy<TransactionWithDetails> { it.transaction.date }.thenBy { it.transaction.createdAt })
            TransactionSortOption.AMOUNT_DESC -> list.sortedByDescending { it.transaction.amount }
            TransactionSortOption.AMOUNT_ASC -> list.sortedBy { it.transaction.amount }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dashboard & Analytics Metrics
    val totalBalance: StateFlow<Double> = allAccounts.combine(MutableStateFlow(0.0)) { accounts, _ ->
        accounts.sumOf { it.currentBalance }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val currentMonthIncome: StateFlow<Double> = allTransactions.combine(MutableStateFlow(0.0)) { txList, _ ->
        val (start, end) = getDateRangeBounds(DateRangeFilter.THIS_MONTH)
        txList.filter { it.transaction.type == "INCOME" && it.transaction.date in start..end }
            .sumOf { it.transaction.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val currentMonthExpense: StateFlow<Double> = allTransactions.combine(MutableStateFlow(0.0)) { txList, _ ->
        val (start, end) = getDateRangeBounds(DateRangeFilter.THIS_MONTH)
        txList.filter { it.transaction.type == "EXPENSE" && it.transaction.date in start..end }
            .sumOf { it.transaction.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val rangeIncome: StateFlow<Double> = combine(allTransactions, _dateRangeFilter, _customDateRange) { txList, rangeFilter, _ ->
        val (start, end) = getDateRangeBounds(rangeFilter)
        txList.filter { it.transaction.type == "INCOME" && it.transaction.date in start..end }
            .sumOf { it.transaction.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val rangeExpense: StateFlow<Double> = combine(allTransactions, _dateRangeFilter, _customDateRange) { txList, rangeFilter, _ ->
        val (start, end) = getDateRangeBounds(rangeFilter)
        txList.filter { it.transaction.type == "EXPENSE" && it.transaction.date in start..end }
            .sumOf { it.transaction.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val categorySpendingList: StateFlow<List<CategorySpending>> = combine(
        allTransactions,
        _dateRangeFilter,
        _customDateRange
    ) { txList, rangeFilter, _ ->
        val (start, end) = getDateRangeBounds(rangeFilter)
        val expenseTx = txList.filter { it.transaction.type == "EXPENSE" && it.transaction.date in start..end }
        val totalSpent = expenseTx.sumOf { it.transaction.amount }

        if (totalSpent == 0.0) return@combine emptyList()

        val grouped = expenseTx.groupBy { it.transaction.categoryId }
        grouped.map { (catId, items) ->
            val sum = items.sumOf { it.transaction.amount }
            val cat = items.firstOrNull()?.category
            CategorySpending(
                categoryId = catId,
                categoryName = cat?.name ?: "Uncategorized",
                color = cat?.color ?: 0xFF64748BL,
                icon = cat?.icon ?: "category",
                amount = sum,
                percentage = ((sum / totalSpent) * 100).toFloat()
            )
        }.sortedByDescending { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyComparisonList: StateFlow<List<MonthlyComparison>> = allTransactions.combine(MutableStateFlow(0)) { txList, _ ->
        val cal = Calendar.getInstance()
        val format = SimpleDateFormat("MMM", Locale.getDefault())
        val list = mutableListOf<MonthlyComparison>()

        for (i in 5 downTo 0) {
            val targetCal = Calendar.getInstance()
            targetCal.add(Calendar.MONTH, -i)
            targetCal.set(Calendar.DAY_OF_MONTH, 1)
            targetCal.set(Calendar.HOUR_OF_DAY, 0)
            targetCal.set(Calendar.MINUTE, 0)
            targetCal.set(Calendar.SECOND, 0)
            val monthStart = targetCal.timeInMillis
            targetCal.set(Calendar.DAY_OF_MONTH, targetCal.getActualMaximum(Calendar.DAY_OF_MONTH))
            targetCal.set(Calendar.HOUR_OF_DAY, 23)
            targetCal.set(Calendar.MINUTE, 59)
            targetCal.set(Calendar.SECOND, 59)
            val monthEnd = targetCal.timeInMillis

            val monthTxs = txList.filter { it.transaction.date in monthStart..monthEnd }
            val inc = monthTxs.filter { it.transaction.type == "INCOME" }.sumOf { it.transaction.amount }
            val exp = monthTxs.filter { it.transaction.type == "EXPENSE" }.sumOf { it.transaction.amount }

            list.add(MonthlyComparison(format.format(Date(monthStart)), inc, exp))
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailySpendingTrend: StateFlow<List<DailySpending>> = combine(
        allTransactions,
        _dateRangeFilter,
        _customDateRange
    ) { txList, rangeFilter, _ ->
        val (start, end) = getDateRangeBounds(rangeFilter)
        val expenseTx = txList.filter { it.transaction.type == "EXPENSE" && it.transaction.date in start..end }

        // Group by day
        val cal = Calendar.getInstance()
        val format = SimpleDateFormat("dd MMM", Locale.getDefault())
        val map = mutableMapOf<String, Triple<Int, Double, Long>>()

        for (tx in expenseTx) {
            cal.timeInMillis = tx.transaction.date
            val label = format.format(Date(tx.transaction.date))
            val day = cal.get(Calendar.DAY_OF_MONTH)
            val existing = map[label]
            if (existing == null) {
                map[label] = Triple(day, tx.transaction.amount, tx.transaction.date)
            } else {
                map[label] = Triple(day, existing.second + tx.transaction.amount, existing.third)
            }
        }

        map.map { (label, data) ->
            DailySpending(label, data.first, data.second, data.third)
        }.sortedBy { it.timestamp }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgetsWithSpending: StateFlow<List<BudgetWithSpending>> = combine(
        allBudgets,
        allTransactions,
        allCategories
    ) { budgets, txList, categories ->
        val (monthStart, monthEnd) = getDateRangeBounds(DateRangeFilter.THIS_MONTH)

        budgets.map { b ->
            val spent = if (b.categoryId != null) {
                txList.filter {
                    it.transaction.type == "EXPENSE" &&
                            it.transaction.categoryId == b.categoryId &&
                            it.transaction.date in b.startDate..b.endDate
                }.sumOf { it.transaction.amount }
            } else {
                // Overall budget
                txList.filter {
                    it.transaction.type == "EXPENSE" &&
                            it.transaction.date in b.startDate..b.endDate
                }.sumOf { it.transaction.amount }
            }

            val category = categories.firstOrNull { it.id == b.categoryId }
            val remaining = (b.amount - spent).coerceAtLeast(0.0)
            val percentage = if (b.amount > 0) (spent / b.amount) * 100.0 else 0.0

            BudgetWithSpending(
                budget = b,
                categoryName = category?.name ?: "Overall",
                spentAmount = spent,
                remainingAmount = remaining,
                percentageUsed = percentage
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Database Actions
    fun addTransaction(
        type: String,
        amount: Double,
        categoryId: Long,
        subcategoryId: Long?,
        accountId: Long,
        paymentMethod: String,
        date: Long,
        time: String,
        notes: String
    ) {
        viewModelScope.launch {
            repository.addTransaction(
                TransactionEntity(
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
            )
        }
    }

    fun updateTransaction(
        updated: TransactionEntity,
        original: TransactionEntity = updated
    ) {
        viewModelScope.launch {
            repository.updateTransaction(updated, original)
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun duplicateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.duplicateTransaction(transaction)
        }
    }

    fun addCategory(name: String, type: String, icon: String, color: Long) {
        viewModelScope.launch {
            repository.addCategory(
                CategoryEntity(name = name, type = type, icon = icon, color = color)
            )
        }
    }

    fun updateCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.updateCategory(category)
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    fun addAccount(name: String, type: String, initialBalance: Double, currency: String, icon: String) {
        viewModelScope.launch {
            repository.addAccount(
                AccountEntity(
                    name = name,
                    type = type,
                    initialBalance = initialBalance,
                    currentBalance = initialBalance,
                    currency = currency,
                    icon = icon
                )
            )
        }
    }

    fun updateAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.updateAccount(account)
        }
    }

    fun deleteAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.deleteAccount(account)
        }
    }

    fun addBudget(name: String, categoryId: Long?, amount: Double, period: String) {
        viewModelScope.launch {
            val (start, end) = when (period) {
                "WEEKLY" -> getDateRangeBounds(DateRangeFilter.THIS_WEEK)
                "YEARLY" -> getDateRangeBounds(DateRangeFilter.THIS_YEAR)
                else -> getDateRangeBounds(DateRangeFilter.THIS_MONTH)
            }
            repository.addBudget(
                BudgetEntity(
                    name = name,
                    categoryId = categoryId,
                    amount = amount,
                    period = period,
                    startDate = start,
                    endDate = end
                )
            )
        }
    }

    fun updateBudget(budget: BudgetEntity) {
        viewModelScope.launch {
            repository.updateBudget(budget)
        }
    }

    fun deleteBudget(budget: BudgetEntity) {
        viewModelScope.launch {
            repository.deleteBudget(budget)
        }
    }

    fun addRecurring(
        type: String,
        amount: Double,
        categoryId: Long,
        subcategoryId: Long?,
        accountId: Long,
        paymentMethod: String,
        notes: String,
        frequency: String,
        interval: Int,
        nextExecutionDate: Long
    ) {
        viewModelScope.launch {
            repository.addRecurring(
                RecurringTransactionEntity(
                    type = type,
                    amount = amount,
                    categoryId = categoryId,
                    subcategoryId = subcategoryId,
                    accountId = accountId,
                    paymentMethod = paymentMethod,
                    notes = notes,
                    frequency = frequency,
                    interval = interval,
                    nextExecutionDate = nextExecutionDate,
                    isActive = true
                )
            )
        }
    }

    fun updateRecurring(recurring: RecurringTransactionEntity) {
        viewModelScope.launch {
            repository.updateRecurring(recurring)
        }
    }

    fun deleteRecurring(recurring: RecurringTransactionEntity) {
        viewModelScope.launch {
            repository.deleteRecurring(recurring)
        }
    }

    fun updateSettings(settings: AppSettingsEntity) {
        viewModelScope.launch {
            repository.updateSettings(settings)
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            repository.resetAllData()
        }
    }

    suspend fun getCsvExport(): String {
        return repository.exportTransactionsToCsv(allTransactions.value)
    }

    suspend fun getJsonBackup(): String {
        return repository.createJsonBackup(allTransactions.value)
    }
}
