package com.example.pos.data

data class Shop(
    val id: String = "shop_1",
    val name: String = "Faith's Store",
    val ownerName: String = "Faith",
    val email: String = "faith@store.com",
    val phone: String = "+254 712 345 678",
    val category: String = "Retail & Grocery",
    val address: String = "Plot 42, Biashara Street, Nairobi",
    val currency: String = "KSh"
)

data class Product(
    val id: String,
    val name: String,
    val category: String,
    val sku: String,
    val unit: String = "pcs",
    val buyingPrice: Double,
    val sellingPrice: Double,
    val stockQuantity: Int,
    val minStock: Int = 10,
    val supplier: String = "",
    val iconType: String = "box"
) {
    val isLowStock: Boolean get() = stockQuantity <= minStock
}

data class CartItem(
    val product: Product,
    val quantity: Int
) {
    val totalPrice: Double get() = product.sellingPrice * quantity
}

data class Sale(
    val id: String,
    val items: List<CartItem>,
    val totalAmount: Double,
    val paymentMethod: String, // Cash, M-Pesa, Card, Other
    val customerName: String = "",
    val dateFormatted: String = "Today, 10:42 AM",
    val timestamp: Long = System.currentTimeMillis(),
    val employeeName: String = "Faith",
    val isCreditSale: Boolean = false,
    val paidAmount: Double = totalAmount,
    val balance: Double = 0.0,
    val dueDate: String = ""
)

data class StockInRecord(
    val id: String,
    val productId: String,
    val productName: String,
    val quantityAdded: Int,
    val buyingPrice: Double,
    val supplier: String,
    val dateFormatted: String,
    val employeeName: String
)

data class Expense(
    val id: String,
    val title: String,
    val category: String,
    val amount: Double,
    val dateFormatted: String,
    val iconType: String = "receipt"
)

data class CustomerCredit(
    val id: String,
    val customerName: String,
    val customerPhone: String,
    val totalOutstanding: Double,
    val productsSummary: String,
    val amountPaid: Double,
    val balance: Double,
    val dueDate: String,
    val status: String = "Pending"
)

data class Employee(
    val id: String,
    val name: String,
    val role: String, // Owner, Manager, Sales Clerk
    val phone: String,
    val email: String,
    val isOwner: Boolean = false
)

data class AuthSession(
    val accessToken: String,
    val refreshToken: String = "",
    val userEmail: String = "",
    val userId: String = "",
    val tokenExpiresAt: Long = 0L,
    val rememberMe: Boolean = true,
    val loginTimestamp: Long = System.currentTimeMillis()
) {
    /**
     * Checks if this session is still considered active.
     * With Remember Me enabled: valid for up to 30 days.
     * Without Remember Me: valid for 24 hours of persistent login (prevents re-login on app switch).
     */
    fun isSessionValid(currentTime: Long = System.currentTimeMillis()): Boolean {
        if (accessToken.isBlank()) return false
        val maxDurationMillis = if (rememberMe) {
            30L * 24 * 60 * 60 * 1000L // 30 days
        } else {
            24L * 60 * 60 * 1000L // 24 hours
        }
        return (currentTime - loginTimestamp) < maxDurationMillis
    }

    /**
     * Checks if the access token specifically has expired or is nearing expiry.
     */
    fun isAccessTokenExpired(currentTime: Long = System.currentTimeMillis()): Boolean {
        if (tokenExpiresAt <= 0L) return false
        return currentTime >= (tokenExpiresAt - 60_000L)
    }
}

