package com.example.pos.data

import kotlinx.serialization.Serializable

@Serializable
data class Shop(
    val id: String = "",
    val name: String = "",
    val ownerName: String = "",
    val email: String = "",
    val phone: String = "",
    val category: String = "",
    val address: String = "",
    val currency: String = "KSh",
    val dailyTarget: Double = 30000.0
)

@Serializable
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
    val iconType: String = "box",
    val imageUri: String? = null,
    val shopId: String? = null,
    val wholesalePrice: Double? = null,
    val wholesaleQuantity: Int? = null
) {
    val isLowStock: Boolean get() = stockQuantity <= minStock
    val retailPrice: Double get() = sellingPrice
    val hasWholesalePricing: Boolean get() = (wholesalePrice ?: 0.0) > 0.0 && (wholesaleQuantity ?: 0) > 0

    fun isWholesaleFor(quantity: Int): Boolean =
        hasWholesalePricing && quantity >= (wholesaleQuantity ?: Int.MAX_VALUE)

    fun priceForQuantity(quantity: Int): Double =
        if (isWholesaleFor(quantity)) (wholesalePrice ?: sellingPrice) else sellingPrice
}

enum class InventoryAlertKind(val storageValue: String, val label: String) {
    LOW_STOCK("low_stock", "Low stock"),
    OUT_OF_STOCK("out_of_stock", "Out of stock");

    companion object {
        fun fromStorage(value: String?): InventoryAlertKind =
            entries.firstOrNull { it.storageValue == value } ?: LOW_STOCK
    }
}

data class AppNotification(
    val id: String,
    val kind: InventoryAlertKind,
    val title: String,
    val message: String,
    val productIds: List<String>,
    val createdAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

@Serializable
data class CartItem(
    val product: Product,
    val quantity: Int
) {
    val isWholesale: Boolean get() = product.isWholesaleFor(quantity)
    val unitPrice: Double get() = product.priceForQuantity(quantity)
    val totalPrice: Double get() = unitPrice * quantity
    val regularTotalPrice: Double get() = product.sellingPrice * quantity
    val wholesaleSavings: Double get() = if (isWholesale) (regularTotalPrice - totalPrice).coerceAtLeast(0.0) else 0.0
}

@Serializable
data class Sale(
    val id: String,
    val items: List<CartItem>,
    val totalAmount: Double,
    val paymentMethod: String,
    val customerName: String = "",
    val dateFormatted: String = "Today, 10:42 AM",
    val timestamp: Long = System.currentTimeMillis(),
    val employeeName: String = "Store Owner",
    val isCreditSale: Boolean = false,
    val paidAmount: Double = totalAmount,
    val balance: Double = 0.0,
    val dueDate: String = "",
    val shopId: String? = null
)

@Serializable
data class StockInRecord(
    val id: String,
    val productId: String,
    val productName: String,
    val quantityAdded: Int,
    val buyingPrice: Double,
    val supplier: String,
    val dateFormatted: String,
    val employeeName: String,
    val shopId: String? = null
)

@Serializable
data class Expense(
    val id: String,
    val title: String,
    val category: String,
    val amount: Double,
    val dateFormatted: String,
    val iconType: String = "receipt",
    val shopId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class CreditPayment(
    val id: String,
    val amount: Double,
    val method: String,
    val timestamp: Long = System.currentTimeMillis(),
    val balanceAfterPayment: Double = 0.0,
    val referenceCode: String = "",
    val notes: String = ""
)

@Serializable
data class CustomerCredit(
    val id: String,
    val customerName: String,
    val customerPhone: String,
    val totalOutstanding: Double,
    val productsSummary: String,
    val amountPaid: Double,
    val balance: Double,
    val dueDate: String,
    val status: String = "Pending",
    val paymentHistory: List<CreditPayment> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val settledAt: String? = null,
    val shopId: String? = null,
    val timestamp: Long = createdAt
)

object EmployeePermissions {
    const val PERM_RECORD_SALES = "record_sales"
    const val PERM_STOCK_IN = "stock_in"
    const val PERM_NEW_PRODUCT = "new_product"
    const val PERM_CREDIT_SALES = "credit_sales"
    const val PERM_RECORD_EXPENSES = "record_expenses"

    val ALL_PERMISSIONS = listOf(
        PERM_RECORD_SALES,
        PERM_STOCK_IN,
        PERM_NEW_PRODUCT,
        PERM_CREDIT_SALES,
        PERM_RECORD_EXPENSES
    )
}

@Serializable
data class Employee(
    val id: String,
    val name: String,
    val role: String,
    val phone: String,
    val email: String,
    val isOwner: Boolean = false,
    val shopId: String? = null,
    val status: String = if (isOwner) "ACTIVE" else "PENDING",
    val permissions: List<String> = if (isOwner) EmployeePermissions.ALL_PERMISSIONS else listOf(
        EmployeePermissions.PERM_RECORD_SALES,
        EmployeePermissions.PERM_STOCK_IN
    ),
    val authUserId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class ApprovalRequest(
    val id: String,
    val type: String, // "NEW_PRODUCT", "STOCK_IN", "CREDIT_SALE", "EXPENSE", "SALE_EDIT"
    val requesterId: String,
    val requesterName: String,
    val title: String,
    val description: String,
    val payloadJson: String,
    val status: String = "PENDING", // "PENDING", "APPROVED", "REJECTED"
    val createdAt: Long = System.currentTimeMillis(),
    val reviewedAt: Long? = null,
    val reviewedBy: String? = null,
    val shopId: String? = null
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
