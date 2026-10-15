package com.example.pos.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class PosStorageManager(context: Context? = null) {
    private val prefs: SharedPreferences? = context?.getSharedPreferences("pos_data_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PRODUCTS = "key_products"
        private const val KEY_SALES = "key_sales"
        private const val KEY_EXPENSES = "key_expenses"
        private const val KEY_CUSTOMER_CREDITS = "key_customer_credits"
        private const val KEY_EMPLOYEES = "key_employees"
        private const val KEY_STOCK_IN = "key_stock_in"
        private const val KEY_SHOP = "key_shop"
        private const val KEY_AUTH_SESSION = "key_auth_session"
        private const val KEY_REMEMBERED_EMAIL = "key_remembered_email"
        private const val KEY_REMEMBER_ME = "key_remember_me"
        private const val KEY_NOTIFICATIONS = "key_notifications"
        private const val KEY_SYSTEM_NOTIFICATIONS_ENABLED = "key_system_notifications_enabled"
    }

    fun saveProducts(products: List<Product>) {
        if (prefs == null) return
        val jsonArray = JSONArray()
        products.forEach { jsonArray.put(it.toJson()) }
        prefs.edit().putString(KEY_PRODUCTS, jsonArray.toString()).apply()
    }

    fun loadProducts(defaultProducts: List<Product>): List<Product> {
        val jsonString = prefs?.getString(KEY_PRODUCTS, null) ?: return defaultProducts
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<Product>()
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getJSONObject(i).toProduct())
            }
            if (list.isEmpty()) defaultProducts else list
        } catch (e: Exception) {
            defaultProducts
        }
    }

    fun saveNotifications(notifications: List<AppNotification>) {
        val json = JSONArray()
        notifications.forEach { notification ->
            json.put(JSONObject().apply {
                put("id", notification.id)
                put("kind", notification.kind.storageValue)
                put("title", notification.title)
                put("message", notification.message)
                put("productIds", JSONArray(notification.productIds))
                put("createdAt", notification.createdAt)
                put("isRead", notification.isRead)
            })
        }
        prefs.edit().putString(KEY_NOTIFICATIONS, json.toString()).apply()
    }

    fun loadNotifications(): List<AppNotification> = runCatching {
        val json = JSONArray(prefs.getString(KEY_NOTIFICATIONS, "[]"))
        (0 until json.length()).map { index ->
            val item = json.getJSONObject(index)
            val ids = item.optJSONArray("productIds") ?: JSONArray()
            AppNotification(
                id = item.getString("id"),
                kind = InventoryAlertKind.fromStorage(item.optString("kind")),
                title = item.optString("title"),
                message = item.optString("message"),
                productIds = (0 until ids.length()).map { ids.getString(it) },
                createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                isRead = item.optBoolean("isRead", false)
            )
        }
    }.getOrDefault(emptyList())

    fun areSystemNotificationsEnabled(): Boolean =
        prefs.getBoolean(KEY_SYSTEM_NOTIFICATIONS_ENABLED, false)

    fun setSystemNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SYSTEM_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    fun saveSales(sales: List<Sale>) {
        if (prefs == null) return
        val jsonArray = JSONArray()
        sales.forEach { jsonArray.put(it.toJson()) }
        prefs.edit().putString(KEY_SALES, jsonArray.toString()).apply()
    }

    fun loadSales(defaultSales: List<Sale>): List<Sale> {
        val jsonString = prefs?.getString(KEY_SALES, null) ?: return defaultSales
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<Sale>()
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getJSONObject(i).toSale())
            }
            list
        } catch (e: Exception) {
            defaultSales
        }
    }

    fun saveExpenses(expenses: List<Expense>) {
        if (prefs == null) return
        val jsonArray = JSONArray()
        expenses.forEach { jsonArray.put(it.toJson()) }
        prefs.edit().putString(KEY_EXPENSES, jsonArray.toString()).apply()
    }

    fun loadExpenses(defaultExpenses: List<Expense>): List<Expense> {
        val jsonString = prefs?.getString(KEY_EXPENSES, null) ?: return defaultExpenses
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<Expense>()
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getJSONObject(i).toExpense())
            }
            list
        } catch (e: Exception) {
            defaultExpenses
        }
    }

    fun saveCustomerCredits(credits: List<CustomerCredit>) {
        if (prefs == null) return
        val jsonArray = JSONArray()
        credits.forEach { jsonArray.put(it.toJson()) }
        prefs.edit().putString(KEY_CUSTOMER_CREDITS, jsonArray.toString()).apply()
    }

    fun loadCustomerCredits(defaultCredits: List<CustomerCredit>): List<CustomerCredit> {
        val jsonString = prefs?.getString(KEY_CUSTOMER_CREDITS, null) ?: return defaultCredits
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<CustomerCredit>()
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getJSONObject(i).toCustomerCredit())
            }
            list
        } catch (e: Exception) {
            defaultCredits
        }
    }

    fun saveEmployees(employees: List<Employee>) {
        if (prefs == null) return
        val jsonArray = JSONArray()
        employees.forEach { jsonArray.put(it.toJson()) }
        prefs.edit().putString(KEY_EMPLOYEES, jsonArray.toString()).apply()
    }

    fun loadEmployees(defaultEmployees: List<Employee>): List<Employee> {
        val jsonString = prefs?.getString(KEY_EMPLOYEES, null) ?: return defaultEmployees
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<Employee>()
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getJSONObject(i).toEmployee())
            }
            list
        } catch (e: Exception) {
            defaultEmployees
        }
    }

    fun saveStockInRecords(records: List<StockInRecord>) {
        if (prefs == null) return
        val jsonArray = JSONArray()
        records.forEach { jsonArray.put(it.toJson()) }
        prefs.edit().putString(KEY_STOCK_IN, jsonArray.toString()).apply()
    }

    fun loadStockInRecords(defaultRecords: List<StockInRecord>): List<StockInRecord> {
        val jsonString = prefs?.getString(KEY_STOCK_IN, null) ?: return defaultRecords
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<StockInRecord>()
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getJSONObject(i).toStockInRecord())
            }
            list
        } catch (e: Exception) {
            defaultRecords
        }
    }

    fun saveShop(shop: Shop) {
        if (prefs == null) return
        prefs.edit().putString(KEY_SHOP, shop.toJson().toString()).apply()
    }

    fun loadShop(defaultShop: Shop): Shop {
        val jsonString = prefs?.getString(KEY_SHOP, null) ?: return defaultShop
        return try {
            JSONObject(jsonString).toShop()
        } catch (e: Exception) {
            defaultShop
        }
    }

    // ==========================================
    // AUTH SESSION & REMEMBER ME
    // ==========================================
    fun saveAuthSession(session: AuthSession) {
        if (prefs == null) return
        val json = JSONObject().apply {
            put("accessToken", session.accessToken)
            put("refreshToken", session.refreshToken)
            put("userEmail", session.userEmail)
            put("userId", session.userId)
            put("tokenExpiresAt", session.tokenExpiresAt)
            put("rememberMe", session.rememberMe)
            put("loginTimestamp", session.loginTimestamp)
        }
        prefs.edit().putString(KEY_AUTH_SESSION, json.toString()).apply()
    }

    fun loadAuthSession(): AuthSession? {
        val jsonString = prefs?.getString(KEY_AUTH_SESSION, null) ?: return null
        return try {
            val obj = JSONObject(jsonString)
            AuthSession(
                accessToken = obj.getString("accessToken"),
                refreshToken = obj.optString("refreshToken", ""),
                userEmail = obj.optString("userEmail", ""),
                userId = obj.optString("userId", ""),
                tokenExpiresAt = obj.optLong("tokenExpiresAt", 0L),
                rememberMe = obj.optBoolean("rememberMe", true),
                loginTimestamp = obj.optLong("loginTimestamp", System.currentTimeMillis())
            )
        } catch (e: Exception) {
            null
        }
    }

    fun updateSessionTokens(accessToken: String, refreshToken: String, expiresAt: Long) {
        val current = loadAuthSession() ?: return
        val updated = current.copy(
            accessToken = accessToken,
            refreshToken = refreshToken.ifBlank { current.refreshToken },
            tokenExpiresAt = expiresAt,
            loginTimestamp = System.currentTimeMillis()
        )
        saveAuthSession(updated)
    }

    fun clearAuthSession() {
        prefs?.edit()?.remove(KEY_AUTH_SESSION)?.apply()
    }

    fun isSessionValid(): Boolean {
        val session = loadAuthSession() ?: return false
        return session.isSessionValid()
    }

    fun saveRememberedEmail(email: String) {
        prefs?.edit()?.putString(KEY_REMEMBERED_EMAIL, email)?.apply()
    }

    fun loadRememberedEmail(): String? {
        return prefs?.getString(KEY_REMEMBERED_EMAIL, null)
    }

    fun clearRememberedEmail() {
        prefs?.edit()?.remove(KEY_REMEMBERED_EMAIL)?.apply()
    }

    fun saveRememberMePreference(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_REMEMBER_ME, enabled)?.apply()
    }

    fun loadRememberMePreference(): Boolean {
        return prefs?.getBoolean(KEY_REMEMBER_ME, true) ?: true
    }
}

// Extension JSON Converters
fun Product.toJson(): JSONObject {
    return JSONObject().apply {
        put("id", id)
        put("name", name)
        put("category", category)
        put("sku", sku)
        put("unit", unit)
        put("buyingPrice", buyingPrice)
        put("sellingPrice", sellingPrice)
        put("stockQuantity", stockQuantity)
        put("minStock", minStock)
        put("supplier", supplier)
        put("iconType", iconType)
        put("imageUri", imageUri)
    }
}

fun JSONObject.toProduct(): Product {
    return Product(
        id = getString("id"),
        name = getString("name"),
        category = getString("category"),
        sku = getString("sku"),
        unit = optString("unit", "pcs"),
        buyingPrice = getDouble("buyingPrice"),
        sellingPrice = getDouble("sellingPrice"),
        stockQuantity = getInt("stockQuantity"),
        minStock = optInt("minStock", 10),
        supplier = optString("supplier", ""),
        iconType = optString("iconType", "box"),
        imageUri = optString("imageUri", "").takeIf { it.isNotBlank() && it != "null" }
    )
}

fun CartItem.toJson(): JSONObject {
    return JSONObject().apply {
        put("product", product.toJson())
        put("quantity", quantity)
    }
}

fun JSONObject.toCartItem(): CartItem {
    return CartItem(
        product = getJSONObject("product").toProduct(),
        quantity = getInt("quantity")
    )
}

fun Sale.toJson(): JSONObject {
    return JSONObject().apply {
        put("id", id)
        val itemsArray = JSONArray()
        items.forEach { itemsArray.put(it.toJson()) }
        put("items", itemsArray)
        put("totalAmount", totalAmount)
        put("paymentMethod", paymentMethod)
        put("customerName", customerName)
        put("dateFormatted", dateFormatted)
        put("timestamp", timestamp)
        put("employeeName", employeeName)
        put("isCreditSale", isCreditSale)
        put("paidAmount", paidAmount)
        put("balance", balance)
        put("dueDate", dueDate)
    }
}

fun JSONObject.toSale(): Sale {
    val itemsArray = getJSONArray("items")
    val itemsList = mutableListOf<CartItem>()
    for (i in 0 until itemsArray.length()) {
        itemsList.add(itemsArray.getJSONObject(i).toCartItem())
    }
    return Sale(
        id = getString("id"),
        items = itemsList,
        totalAmount = getDouble("totalAmount"),
        paymentMethod = getString("paymentMethod"),
        customerName = optString("customerName", ""),
        dateFormatted = optString("dateFormatted", "Today"),
        timestamp = optLong("timestamp", System.currentTimeMillis()),
        employeeName = optString("employeeName", "Faith"),
        isCreditSale = optBoolean("isCreditSale", false),
        paidAmount = optDouble("paidAmount", getDouble("totalAmount")),
        balance = optDouble("balance", 0.0),
        dueDate = optString("dueDate", "")
    )
}

fun StockInRecord.toJson(): JSONObject {
    return JSONObject().apply {
        put("id", id)
        put("productId", productId)
        put("productName", productName)
        put("quantityAdded", quantityAdded)
        put("buyingPrice", buyingPrice)
        put("supplier", supplier)
        put("dateFormatted", dateFormatted)
        put("employeeName", employeeName)
    }
}

fun JSONObject.toStockInRecord(): StockInRecord {
    return StockInRecord(
        id = getString("id"),
        productId = getString("productId"),
        productName = getString("productName"),
        quantityAdded = getInt("quantityAdded"),
        buyingPrice = getDouble("buyingPrice"),
        supplier = optString("supplier", ""),
        dateFormatted = optString("dateFormatted", ""),
        employeeName = optString("employeeName", "")
    )
}

fun Expense.toJson(): JSONObject {
    return JSONObject().apply {
        put("id", id)
        put("title", title)
        put("category", category)
        put("amount", amount)
        put("dateFormatted", dateFormatted)
        put("iconType", iconType)
    }
}

fun JSONObject.toExpense(): Expense {
    return Expense(
        id = getString("id"),
        title = getString("title"),
        category = getString("category"),
        amount = getDouble("amount"),
        dateFormatted = optString("dateFormatted", ""),
        iconType = optString("iconType", "receipt")
    )
}

fun CustomerCredit.toJson(): JSONObject {
    return JSONObject().apply {
        put("id", id)
        put("customerName", customerName)
        put("customerPhone", customerPhone)
        put("totalOutstanding", totalOutstanding)
        put("productsSummary", productsSummary)
        put("amountPaid", amountPaid)
        put("balance", balance)
        put("dueDate", dueDate)
        put("status", status)
    }
}

fun JSONObject.toCustomerCredit(): CustomerCredit {
    return CustomerCredit(
        id = getString("id"),
        customerName = getString("customerName"),
        customerPhone = optString("customerPhone", ""),
        totalOutstanding = getDouble("totalOutstanding"),
        productsSummary = optString("productsSummary", ""),
        amountPaid = optDouble("amountPaid", 0.0),
        balance = getDouble("balance"),
        dueDate = optString("dueDate", ""),
        status = optString("status", "Pending")
    )
}

fun Employee.toJson(): JSONObject {
    return JSONObject().apply {
        put("id", id)
        put("name", name)
        put("role", role)
        put("phone", phone)
        put("email", email)
        put("isOwner", isOwner)
    }
}

fun JSONObject.toEmployee(): Employee {
    return Employee(
        id = getString("id"),
        name = getString("name"),
        role = getString("role"),
        phone = optString("phone", ""),
        email = optString("email", ""),
        isOwner = optBoolean("isOwner", false)
    )
}

fun Shop.toJson(): JSONObject {
    return JSONObject().apply {
        put("id", id)
        put("name", name)
        put("ownerName", ownerName)
        put("email", email)
        put("phone", phone)
        put("category", category)
        put("address", address)
        put("currency", currency)
    }
}

fun JSONObject.toShop(): Shop {
    return Shop(
        id = optString("id", "shop_1"),
        name = optString("name", "Faith's Store"),
        ownerName = optString("ownerName", "Faith"),
        email = optString("email", "faith@store.com"),
        phone = optString("phone", "+254 712 345 678"),
        category = optString("category", "Retail & Grocery"),
        address = optString("address", "Plot 42, Biashara Street, Nairobi"),
        currency = optString("currency", "KSh")
    )
}
