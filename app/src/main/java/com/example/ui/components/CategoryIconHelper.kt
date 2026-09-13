package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIconHelper {
    fun getIcon(name: String): ImageVector {
        return when (name.lowercase()) {
            "restaurant", "fastfood", "dining" -> Icons.Default.Restaurant
            "directions_car", "transport", "car" -> Icons.Default.DirectionsCar
            "shopping_bag", "shopping" -> Icons.Default.ShoppingBag
            "shopping_basket", "groceries" -> Icons.Default.ShoppingBasket
            "movie", "entertainment" -> Icons.Default.Movie
            "receipt", "bills" -> Icons.Default.Receipt
            "medical_services", "health" -> Icons.Default.MedicalServices
            "school", "education" -> Icons.Default.School
            "flight", "travel" -> Icons.Default.Flight
            "home", "rent" -> Icons.Default.Home
            "work", "salary" -> Icons.Default.Work
            "laptop", "freelance" -> Icons.Default.Laptop
            "store", "business" -> Icons.Default.Store
            "trending_up", "investment" -> Icons.Default.TrendingUp
            "card_giftcard", "gift" -> Icons.Default.CardGiftcard
            "payments", "cash" -> Icons.Default.Payments
            "account_balance", "bank" -> Icons.Default.AccountBalance
            "credit_card" -> Icons.Default.CreditCard
            "account_balance_wallet", "wallet" -> Icons.Default.AccountBalanceWallet
            "local_cafe", "coffee" -> Icons.Default.LocalCafe
            "local_gas_station", "fuel" -> Icons.Default.LocalGasStation
            "directions_bus", "bus" -> Icons.Default.DirectionsBus
            "bolt", "electricity" -> Icons.Default.Bolt
            "wifi", "internet" -> Icons.Default.Wifi
            else -> Icons.Default.Category
        }
    }

    val availableCategoryIcons = listOf(
        "restaurant", "shopping_bag", "directions_car", "movie", "receipt",
        "medical_services", "school", "flight", "home", "work",
        "laptop", "store", "trending_up", "card_giftcard", "payments",
        "local_cafe", "local_gas_station", "bolt", "wifi", "category"
    )

    val availableColors = listOf(
        0xFF10B981L, // Emerald
        0xFFEF4444L, // Rose / Red
        0xFFF59E0BL, // Amber
        0xFF3B82F6L, // Blue
        0xFF8B5CF6L, // Purple
        0xFFEC4899L, // Pink
        0xFF06B6D4L, // Cyan
        0xFF14B8A6L, // Teal
        0xFFF97316L, // Orange
        0xFF6366F1L, // Indigo
        0xFF64748BL  // Slate
    )
}
