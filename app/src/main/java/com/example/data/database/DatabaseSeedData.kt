package com.example.data.database

import com.example.data.model.AccountEntity
import com.example.data.model.AppSettingsEntity
import com.example.data.model.BudgetEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.RecurringTransactionEntity
import com.example.data.model.SubcategoryEntity
import com.example.data.model.TransactionEntity
import java.util.Calendar

object DatabaseSeedData {
    suspend fun seedInitialDataIfEmpty(db: AppDatabase) {
        val categoryCount = db.categoryDao().getCategoryCount()
        if (categoryCount > 0) return

        // 1. Initial Settings
        db.appSettingsDao().insertOrUpdate(
            AppSettingsEntity(
                id = 1,
                currency = "$",
                theme = "SYSTEM",
                notificationsEnabled = true,
                biometricEnabled = false,
                pinEnabled = false,
                pinCode = "",
                startDayOfMonth = 1
            )
        )

        // 2. Default Expense Categories
        val foodId = db.categoryDao().insertCategory(
            CategoryEntity(name = "Food & Dining", type = "EXPENSE", icon = "restaurant", color = 0xFFEF4444)
        )
        val transportId = db.categoryDao().insertCategory(
            CategoryEntity(name = "Transport", type = "EXPENSE", icon = "directions_car", color = 0xFFF59E0B)
        )
        val shoppingId = db.categoryDao().insertCategory(
            CategoryEntity(name = "Shopping", type = "EXPENSE", icon = "shopping_bag", color = 0xFF8B5CF6)
        )
        val entertainmentId = db.categoryDao().insertCategory(
            CategoryEntity(name = "Entertainment", type = "EXPENSE", icon = "movie", color = 0xFFEC4899)
        )
        val billsId = db.categoryDao().insertCategory(
            CategoryEntity(name = "Bills & Utilities", type = "EXPENSE", icon = "receipt", color = 0xFF3B82F6)
        )
        val healthId = db.categoryDao().insertCategory(
            CategoryEntity(name = "Healthcare", type = "EXPENSE", icon = "medical_services", color = 0xFF10B981)
        )
        val educationId = db.categoryDao().insertCategory(
            CategoryEntity(name = "Education", type = "EXPENSE", icon = "school", color = 0xFF06B6D4)
        )
        val travelId = db.categoryDao().insertCategory(
            CategoryEntity(name = "Travel", type = "EXPENSE", icon = "flight", color = 0xFFF97316)
        )
        val rentId = db.categoryDao().insertCategory(
            CategoryEntity(name = "Rent & Housing", type = "EXPENSE", icon = "home", color = 0xFF6366F1)
        )
        val otherExpId = db.categoryDao().insertCategory(
            CategoryEntity(name = "Other Expenses", type = "EXPENSE", icon = "category", color = 0xFF64748B)
        )

        // 3. Default Income Categories
        val salaryId = db.categoryDao().insertCategory(
            CategoryEntity(name = "Salary", type = "INCOME", icon = "work", color = 0xFF10B981)
        )
        val freelanceId = db.categoryDao().insertCategory(
            CategoryEntity(name = "Freelance", type = "INCOME", icon = "laptop", color = 0xFF14B8A6)
        )
        val businessId = db.categoryDao().insertCategory(
            CategoryEntity(name = "Business", type = "INCOME", icon = "store", color = 0xFF3B82F6)
        )
        val investmentId = db.categoryDao().insertCategory(
            CategoryEntity(name = "Investment", type = "INCOME", icon = "trending_up", color = 0xFF8B5CF6)
        )
        val giftId = db.categoryDao().insertCategory(
            CategoryEntity(name = "Gift", type = "INCOME", icon = "card_giftcard", color = 0xFFF43F5E)
        )
        val otherIncId = db.categoryDao().insertCategory(
            CategoryEntity(name = "Other Income", type = "INCOME", icon = "payments", color = 0xFF64748B)
        )

        // 4. Subcategories
        val subGroceries = db.categoryDao().insertSubcategory(
            SubcategoryEntity(categoryId = foodId, name = "Groceries", icon = "shopping_basket")
        )
        val subDining = db.categoryDao().insertSubcategory(
            SubcategoryEntity(categoryId = foodId, name = "Dining Out", icon = "restaurant")
        )
        val subCoffee = db.categoryDao().insertSubcategory(
            SubcategoryEntity(categoryId = foodId, name = "Coffee & Snacks", icon = "local_cafe")
        )
        val subFuel = db.categoryDao().insertSubcategory(
            SubcategoryEntity(categoryId = transportId, name = "Fuel", icon = "local_gas_station")
        )
        val subPublic = db.categoryDao().insertSubcategory(
            SubcategoryEntity(categoryId = transportId, name = "Transit Pass", icon = "directions_bus")
        )
        val subElectricity = db.categoryDao().insertSubcategory(
            SubcategoryEntity(categoryId = billsId, name = "Electricity", icon = "bolt")
        )
        val subInternet = db.categoryDao().insertSubcategory(
            SubcategoryEntity(categoryId = billsId, name = "High Speed Internet", icon = "wifi")
        )

        // 5. Default Clean Accounts with 0.0 balance
        db.accountDao().insertAccount(
            AccountEntity(
                name = "Cash",
                type = "CASH",
                initialBalance = 0.0,
                currentBalance = 0.0,
                currency = "$",
                icon = "payments"
            )
        )
        db.accountDao().insertAccount(
            AccountEntity(
                name = "Bank Account",
                type = "BANK",
                initialBalance = 0.0,
                currentBalance = 0.0,
                currency = "$",
                icon = "account_balance"
            )
        )

        // No hardcoded budgets, recurring transactions, or dummy transactions.
        // Everything will be dynamically added and managed by the user.
    }
}
