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
        private const val KEY_CREDIT_DURATION_DAYS = "key_credit_duration_days"
        private const val KEY_LEGACY_CREDIT_DURATION_MONTHS = "key_credit_duration_months"
        private const val KEY_SHOP = "key_shop"
        private const val KEY_AUTH_SESSION = "key_auth_session"
        private const val KEY_REMEMBERED_EMAIL = "key_remembered_email"
        private const val KEY_REMEMBER_ME = "key_remember_me"
        private const val KEY_NOTIFICATIONS = "key_notifications"
        private const val KEY_SYSTEM_NOTIFICATIONS_ENABLED = "key_system_notifications_enabled"
        private const val KEY_APPROVAL_REQUESTS = "key_approval_requests"
        private const val KEY_CURRENT_USER_ROLE = "key_current_user_role"
        private const val KEY_ACTIVE_EMPLOYEE_ID = "key_active_employee_id"
        private const val KEY_REMINDER_CONFIG = "key_reminder_config"
        private const val KEY_SCHEDULED_REMINDERS = "key_scheduled_reminders"
        private const val KEY_REMINDER_EXECUTION_HISTORY = "key_reminder_execution_history"
        private const val KEY_PENDING_SYNC_OPERATIONS = "key_pending_sync_operations"
    }

    private inline fun editPrefs(action: SharedPreferences.Editor.() -> Unit) {
        val p = prefs ?: return
        val editor = p.edit()
        editor.action()
        editor.apply()
    }

    fun saveProducts(products: List<Product>) {
        val jsonArray = JSONArray()
        products.forEach { jsonArray.put(it.toJson()) }
        editPrefs { putString(KEY_PRODUCTS, jsonArray.toString()) }
    }

    fun loadProducts(defaultProducts: List<Product> = emptyList()): List<Product> {
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
                put("category", notification.category.storageValue)
                put("title", notification.title)
                put("message", notification.message)
                put("productIds", JSONArray(notification.productIds))
                if (notification.creditSaleId != null) put("creditSaleId", notification.creditSaleId)
                if (notification.customerName != null) put("customerName", notification.customerName)
                if (notification.deliveryDetails != null) put("deliveryDetails", notification.deliveryDetails)
                put("createdAt", notification.createdAt)
                put("isRead", notification.isRead)
            })
        }
        editPrefs { putString(KEY_NOTIFICATIONS, json.toString()) }
    }

    fun loadNotifications(): List<AppNotification> = runCatching {
        val jsonString = prefs?.getString(KEY_NOTIFICATIONS, "[]") ?: "[]"
        val json = JSONArray(jsonString)
        (0 until json.length()).map { index ->
            val item = json.getJSONObject(index)
            val ids = item.optJSONArray("productIds") ?: JSONArray()
            AppNotification(
                id = item.getString("id"),
                kind = InventoryAlertKind.fromStorage(item.optString("kind")),
                category = NotificationCategory.fromStorage(item.optString("category")),
                title = item.optString("title"),
                message = item.optString("message"),
                productIds = (0 until ids.length()).map { ids.getString(it) },
                creditSaleId = item.optString("creditSaleId").takeIf { it.isNotBlank() },
                customerName = item.optString("customerName").takeIf { it.isNotBlank() },
                deliveryDetails = item.optString("deliveryDetails").takeIf { it.isNotBlank() },
                createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                isRead = item.optBoolean("isRead", false)
            )
        }
    }.getOrDefault(emptyList())

    // ==========================================
    // REMINDER CONFIGURATION & SCHEDULES
    // ==========================================
    fun saveReminderConfig(config: ReminderConfig) {
        val json = JSONObject().apply {
            put("advanceDaysBeforeDue", config.advanceDaysBeforeDue)
            put("additionalAdvanceDaysBeforeDue", config.additionalAdvanceDaysBeforeDue)
            put("firstOverdueDaysAfterDue", config.firstOverdueDaysAfterDue)
            put("repeatInterval", config.repeatInterval.storageValue)
            put("repeatIntervalDays", config.repeatIntervalDays)
            put("repeatCount", config.repeatCount)
            put("stopCondition", config.stopCondition.storageValue)
            put("defaultChannel", config.defaultChannel.storageValue)
            put("workingHoursStart", config.workingHoursStart)
            put("workingHoursEnd", config.workingHoursEnd)
            put("timeZoneId", config.timeZoneId)
            put("deliveryTimeMode", config.deliveryTimeMode.storageValue)
            put("specificDeliveryHour", config.specificDeliveryHour)
            put("specificDeliveryMinute", config.specificDeliveryMinute)
            put("allowOffHoursSending", config.allowOffHoursSending)
            put("autoSendEnabled", config.autoSendEnabled)
            put("templates", JSONObject().apply {
                put("advanceTemplate", config.templates.advanceTemplate)
                put("dueDateTemplate", config.templates.dueDateTemplate)
                put("firstOverdueTemplate", config.templates.firstOverdueTemplate)
                put("repeatedOverdueTemplate", config.templates.repeatedOverdueTemplate)
            })
        }
        editPrefs { putString(KEY_REMINDER_CONFIG, json.toString()) }
    }

    fun loadReminderConfig(): ReminderConfig = runCatching {
        val jsonString = prefs?.getString(KEY_REMINDER_CONFIG, null) ?: return ReminderConfig()
        val obj = JSONObject(jsonString)
        val tmplObj = obj.optJSONObject("templates")
        val templates = if (tmplObj != null) {
            ReminderTemplates(
                advanceTemplate = tmplObj.optString("advanceTemplate", ReminderTemplates().advanceTemplate),
                dueDateTemplate = tmplObj.optString("dueDateTemplate", ReminderTemplates().dueDateTemplate),
                firstOverdueTemplate = tmplObj.optString("firstOverdueTemplate", ReminderTemplates().firstOverdueTemplate),
                repeatedOverdueTemplate = tmplObj.optString("repeatedOverdueTemplate", ReminderTemplates().repeatedOverdueTemplate)
            )
        } else {
            ReminderTemplates()
        }
        ReminderConfig(
            advanceDaysBeforeDue = obj.optInt("advanceDaysBeforeDue", 3),
            additionalAdvanceDaysBeforeDue = obj.optInt("additionalAdvanceDaysBeforeDue", 7),
            firstOverdueDaysAfterDue = obj.optInt("firstOverdueDaysAfterDue", 3),
            repeatInterval = OverdueRepeatInterval.fromStorage(obj.optString("repeatInterval")),
            repeatIntervalDays = obj.optInt("repeatIntervalDays", 7),
            repeatCount = obj.optInt("repeatCount", 3),
            stopCondition = OverdueStopCondition.fromStorage(obj.optString("stopCondition")),
            defaultChannel = ReminderDeliveryChannel.fromStorage(obj.optString("defaultChannel")),
            workingHoursStart = obj.optInt("workingHoursStart", 8),
            workingHoursEnd = obj.optInt("workingHoursEnd", 17),
            timeZoneId = obj.optString("timeZoneId", "Africa/Nairobi"),
            deliveryTimeMode = ReminderDeliveryTimeMode.fromStorage(obj.optString("deliveryTimeMode")),
            specificDeliveryHour = obj.optInt("specificDeliveryHour", 10),
            specificDeliveryMinute = obj.optInt("specificDeliveryMinute", 0),
            allowOffHoursSending = obj.optBoolean("allowOffHoursSending", false),
            autoSendEnabled = obj.optBoolean("autoSendEnabled", true),
            templates = templates
        )
    }.getOrDefault(ReminderConfig())

    fun saveScheduledReminders(reminders: List<ScheduledReminder>) {
        val jsonArray = JSONArray()
        reminders.forEach { r ->
            jsonArray.put(JSONObject().apply {
                put("id", r.id)
                put("creditId", r.creditId)
                put("customerName", r.customerName)
                put("customerPhone", r.customerPhone)
                put("reminderType", r.reminderType.storageValue)
                put("scheduledTimeMillis", r.scheduledTimeMillis)
                put("remainingBalanceAtScheduling", r.remainingBalanceAtScheduling)
                put("shopId", r.shopId)
                put("status", r.status.storageValue)
                put("failureReason", r.failureReason)
            })
        }
        editPrefs { putString(KEY_SCHEDULED_REMINDERS, jsonArray.toString()) }
    }

    fun loadScheduledReminders(): List<ScheduledReminder> = runCatching {
        val jsonString = prefs?.getString(KEY_SCHEDULED_REMINDERS, "[]") ?: "[]"
        val json = JSONArray(jsonString)
        (0 until json.length()).map { i ->
            val obj = json.getJSONObject(i)
            ScheduledReminder(
                id = obj.getString("id"),
                creditId = obj.getString("creditId"),
                customerName = obj.optString("customerName"),
                customerPhone = obj.optString("customerPhone"),
                reminderType = ReminderEventType.fromStorage(obj.optString("reminderType")),
                scheduledTimeMillis = obj.optLong("scheduledTimeMillis"),
                remainingBalanceAtScheduling = obj.optDouble("remainingBalanceAtScheduling", 0.0),
                shopId = obj.optString("shopId").takeIf { it.isNotBlank() },
                status = DeliveryStatus.fromStorage(obj.optString("status")),
                failureReason = obj.optString("failureReason").takeIf { it.isNotBlank() }
            )
        }
    }.getOrDefault(emptyList())

    fun saveReminderExecutionHistory(history: List<ReminderExecutionRecord>) {
        val jsonArray = JSONArray()
        history.take(200).forEach { rec ->
            jsonArray.put(JSONObject().apply {
                put("id", rec.id)
                put("creditId", rec.creditId)
                put("customerName", rec.customerName)
                put("customerPhone", rec.customerPhone)
                put("reminderType", rec.reminderType.storageValue)
                put("scheduledTimeMillis", rec.scheduledTimeMillis)
                put("executionTimeMillis", rec.executionTimeMillis)
                put("balanceAtExecution", rec.balanceAtExecution)
                put("renderedMessage", rec.renderedMessage)
                put("whatsAppStatus", rec.whatsAppStatus.storageValue)
                put("whatsAppError", rec.whatsAppError)
                put("smsStatus", rec.smsStatus.storageValue)
                put("smsError", rec.smsError)
                put("shopId", rec.shopId)
            })
        }
        editPrefs { putString(KEY_REMINDER_EXECUTION_HISTORY, jsonArray.toString()) }
    }

    fun loadReminderExecutionHistory(): List<ReminderExecutionRecord> = runCatching {
        val jsonString = prefs?.getString(KEY_REMINDER_EXECUTION_HISTORY, "[]") ?: "[]"
        val json = JSONArray(jsonString)
        (0 until json.length()).map { i ->
            val obj = json.getJSONObject(i)
            ReminderExecutionRecord(
                id = obj.getString("id"),
                creditId = obj.getString("creditId"),
                customerName = obj.optString("customerName"),
                customerPhone = obj.optString("customerPhone"),
                reminderType = ReminderEventType.fromStorage(obj.optString("reminderType")),
                scheduledTimeMillis = obj.optLong("scheduledTimeMillis"),
                executionTimeMillis = obj.optLong("executionTimeMillis"),
                balanceAtExecution = obj.optDouble("balanceAtExecution", 0.0),
                renderedMessage = obj.optString("renderedMessage"),
                whatsAppStatus = DeliveryStatus.fromStorage(obj.optString("whatsAppStatus")),
                whatsAppError = obj.optString("whatsAppError").takeIf { it.isNotBlank() },
                smsStatus = DeliveryStatus.fromStorage(obj.optString("smsStatus")),
                smsError = obj.optString("smsError").takeIf { it.isNotBlank() },
                shopId = obj.optString("shopId").takeIf { it.isNotBlank() }
            )
        }
    }.getOrDefault(emptyList())

    // ==========================================
    // OFFLINE SYNC OPERATION QUEUE
    // ==========================================
    fun savePendingSyncOperations(operations: List<SyncOperation>) {
        val jsonArray = JSONArray()
        operations.forEach { op ->
            jsonArray.put(JSONObject().apply {
                put("id", op.id)
                put("entityType", op.entityType)
                put("entityId", op.entityId)
                put("action", op.action)
                put("payloadJson", op.payloadJson)
                put("timestamp", op.timestamp)
                put("attempts", op.attempts)
                put("lastError", op.lastError)
                put("shopId", op.shopId)
            })
        }
        editPrefs { putString(KEY_PENDING_SYNC_OPERATIONS, jsonArray.toString()) }
    }

    fun loadPendingSyncOperations(): List<SyncOperation> = runCatching {
        val jsonString = prefs?.getString(KEY_PENDING_SYNC_OPERATIONS, "[]") ?: "[]"
        val json = JSONArray(jsonString)
        (0 until json.length()).map { i ->
            val obj = json.getJSONObject(i)
            SyncOperation(
                id = obj.getString("id"),
                entityType = obj.getString("entityType"),
                entityId = obj.getString("entityId"),
                action = obj.getString("action"),
                payloadJson = obj.getString("payloadJson"),
                timestamp = obj.optLong("timestamp"),
                attempts = obj.optInt("attempts", 0),
                lastError = obj.optString("lastError").takeIf { it.isNotBlank() },
                shopId = obj.optString("shopId").takeIf { it.isNotBlank() }
            )
        }
    }.getOrDefault(emptyList())

    fun enqueueSyncOperation(op: SyncOperation) {
        val current = loadPendingSyncOperations().toMutableList()
        // Deduplicate operations for same entity and action
        val existingIndex = current.indexOfFirst { it.entityType == op.entityType && it.entityId == op.entityId && it.action == op.action }
        if (existingIndex >= 0) {
            current[existingIndex] = op
        } else {
            current.add(op)
        }
        savePendingSyncOperations(current)
    }

    fun removeSyncOperation(opId: String) {
        val current = loadPendingSyncOperations().filterNot { it.id == opId }
        savePendingSyncOperations(current)
    }

    fun areSystemNotificationsEnabled(): Boolean =
        prefs?.getBoolean(KEY_SYSTEM_NOTIFICATIONS_ENABLED, true) ?: true

    fun setSystemNotificationsEnabled(enabled: Boolean) {
        editPrefs { putBoolean(KEY_SYSTEM_NOTIFICATIONS_ENABLED, enabled) }
    }

    fun saveSales(sales: List<Sale>) {
        val jsonArray = JSONArray()
        sales.forEach { jsonArray.put(it.toJson()) }
        editPrefs { putString(KEY_SALES, jsonArray.toString()) }
    }

    fun loadSales(defaultSales: List<Sale> = emptyList()): List<Sale> {
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
        val jsonArray = JSONArray()
        expenses.forEach { jsonArray.put(it.toJson()) }
        editPrefs { putString(KEY_EXPENSES, jsonArray.toString()) }
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
        val jsonArray = JSONArray()
        credits.forEach { jsonArray.put(it.toJson()) }
        editPrefs { putString(KEY_CUSTOMER_CREDITS, jsonArray.toString()) }
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
        val jsonArray = JSONArray()
        employees.forEach { jsonArray.put(it.toJson()) }
        editPrefs { putString(KEY_EMPLOYEES, jsonArray.toString()) }
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

    fun saveApprovalRequests(requests: List<ApprovalRequest>) {
        val jsonArray = JSONArray()
        requests.forEach { jsonArray.put(it.toJson()) }
        editPrefs { putString(KEY_APPROVAL_REQUESTS, jsonArray.toString()) }
    }

    fun loadApprovalRequests(defaultRequests: List<ApprovalRequest> = emptyList()): List<ApprovalRequest> {
        val jsonString = prefs?.getString(KEY_APPROVAL_REQUESTS, null) ?: return defaultRequests
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<ApprovalRequest>()
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getJSONObject(i).toApprovalRequest())
            }
            list
        } catch (e: Exception) {
            defaultRequests
        }
    }

    fun saveCurrentUserRole(role: String) {
        editPrefs { putString(KEY_CURRENT_USER_ROLE, role) }
    }

    fun loadCurrentUserRole(): String {
        return prefs?.getString(KEY_CURRENT_USER_ROLE, "OWNER") ?: "OWNER"
    }

    fun saveActiveEmployeeId(id: String?) {
        editPrefs {
            if (id == null) remove(KEY_ACTIVE_EMPLOYEE_ID)
            else putString(KEY_ACTIVE_EMPLOYEE_ID, id)
        }
    }

    fun loadActiveEmployeeId(): String? {
        return prefs?.getString(KEY_ACTIVE_EMPLOYEE_ID, null)
    }

    fun saveStockInRecords(records: List<StockInRecord>) {
        val jsonArray = JSONArray()
        records.forEach { jsonArray.put(it.toJson()) }
        editPrefs { putString(KEY_STOCK_IN, jsonArray.toString()) }
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
        editPrefs { putString(KEY_SHOP, shop.toJson().toString()) }
    }

    fun loadShop(defaultShop: Shop): Shop {
        val jsonString = prefs?.getString(KEY_SHOP, null) ?: return defaultShop
        return try {
            JSONObject(jsonString).toShop()
        } catch (e: Exception) {
            defaultShop
        }
    }

    fun saveCreditDurationDays(days: Int) {
        editPrefs { putInt(KEY_CREDIT_DURATION_DAYS, days.coerceAtLeast(1)) }
    }

    fun loadCreditDurationDays(defaultDays: Int = 14): Int {
        val p = prefs ?: return defaultDays
        if (p.contains(KEY_CREDIT_DURATION_DAYS)) {
            return p.getInt(KEY_CREDIT_DURATION_DAYS, defaultDays).coerceAtLeast(1)
        }

        val legacyValue = p.getInt(KEY_LEGACY_CREDIT_DURATION_MONTHS, 0)
        val migratedDays = legacyValue.takeIf { it in setOf(7, 14, 30, 60) } ?: defaultDays
        saveCreditDurationDays(migratedDays)
        return migratedDays
    }

    // ==========================================
    // AUTH SESSION & REMEMBER ME
    // ==========================================
    fun saveAuthSession(session: AuthSession) {
        val json = JSONObject().apply {
            put("accessToken", session.accessToken)
            put("refreshToken", session.refreshToken)
            put("userEmail", session.userEmail)
            put("userId", session.userId)
            put("tokenExpiresAt", session.tokenExpiresAt)
            put("rememberMe", session.rememberMe)
            put("loginTimestamp", session.loginTimestamp)
        }
        editPrefs { putString(KEY_AUTH_SESSION, json.toString()) }
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
        editPrefs {
            remove(KEY_AUTH_SESSION)
            remove(KEY_CURRENT_USER_ROLE)
            remove(KEY_ACTIVE_EMPLOYEE_ID)
        }
    }

    fun isSessionValid(): Boolean {
        val session = loadAuthSession() ?: return false
        return session.isSessionValid()
    }

    fun saveRememberedEmail(email: String) {
        editPrefs { putString(KEY_REMEMBERED_EMAIL, email) }
    }

    fun loadRememberedEmail(): String? {
        return prefs?.getString(KEY_REMEMBERED_EMAIL, null)
    }

    fun clearRememberedEmail() {
        editPrefs { remove(KEY_REMEMBERED_EMAIL) }
    }

    fun saveRememberMePreference(enabled: Boolean) {
        editPrefs { putBoolean(KEY_REMEMBER_ME, enabled) }
    }

    fun loadRememberMePreference(): Boolean {
        return prefs?.getBoolean(KEY_REMEMBER_ME, true) ?: true
    }

    fun clearShopData() {
        editPrefs {
            remove(KEY_SALES)
            remove(KEY_EXPENSES)
            remove(KEY_CUSTOMER_CREDITS)
            remove(KEY_STOCK_IN)
            remove(KEY_EMPLOYEES)
            remove(KEY_PRODUCTS)
            remove(KEY_SHOP)
            remove(KEY_NOTIFICATIONS)
            remove(KEY_APPROVAL_REQUESTS)
            remove(KEY_CURRENT_USER_ROLE)
            remove(KEY_ACTIVE_EMPLOYEE_ID)
        }
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
        put("shopId", shopId)
        wholesalePrice?.let { put("wholesalePrice", it) }
        wholesaleQuantity?.let { put("wholesaleQuantity", it) }
    }
}

fun JSONObject.toProduct(): Product {
    val recordId = getString("id")
    val rawSku = optString("sku", "")
    val resolvedSku = if (rawSku.isBlank()) "SKU-${recordId.takeLast(4).uppercase()}" else rawSku
    val parsedWholesalePrice = if (has("wholesalePrice") && !isNull("wholesalePrice")) {
        optDouble("wholesalePrice", Double.NaN).takeIf { !it.isNaN() && it > 0 }
    } else null
    val parsedWholesaleQuantity = if (has("wholesaleQuantity") && !isNull("wholesaleQuantity")) {
        optInt("wholesaleQuantity", -1).takeIf { it > 0 }
    } else null
    return Product(
        id = recordId,
        name = getString("name"),
        category = getString("category"),
        sku = resolvedSku,
        unit = optString("unit", "pcs"),
        buyingPrice = getDouble("buyingPrice"),
        sellingPrice = getDouble("sellingPrice"),
        stockQuantity = getInt("stockQuantity"),
        minStock = optInt("minStock", 10),
        supplier = optString("supplier", ""),
        iconType = optString("iconType", "box"),
        imageUri = optString("imageUri", "").takeIf { it.isNotBlank() && it != "null" },
        shopId = optString("shopId", "").takeIf { it.isNotBlank() },
        wholesalePrice = parsedWholesalePrice,
        wholesaleQuantity = parsedWholesaleQuantity
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
        put("shopId", shopId)
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
        employeeName = optString("employeeName", "Store Owner"),
        isCreditSale = optBoolean("isCreditSale", false),
        paidAmount = optDouble("paidAmount", getDouble("totalAmount")),
        balance = optDouble("balance", 0.0),
        dueDate = optString("dueDate", ""),
        shopId = optString("shopId", "").takeIf { it.isNotBlank() }
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
        put("shopId", shopId)
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
        employeeName = optString("employeeName", ""),
        shopId = optString("shopId", "").takeIf { it.isNotBlank() }
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
        put("shopId", shopId)
        put("timestamp", timestamp)
    }
}

fun JSONObject.toExpense(): Expense {
    val recordId = getString("id")
    val defaultTimestamp = recordId.removePrefix("exp_").toLongOrNull() ?: System.currentTimeMillis()
    return Expense(
        id = recordId,
        title = getString("title"),
        category = getString("category"),
        amount = getDouble("amount"),
        dateFormatted = optString("dateFormatted", ""),
        iconType = optString("iconType", "receipt"),
        shopId = optString("shopId", "").takeIf { it.isNotBlank() },
        timestamp = optLong("timestamp", defaultTimestamp)
    )
}

fun CreditPayment.toJson(): JSONObject {
    return JSONObject().apply {
        put("id", id)
        put("amount", amount)
        put("method", method)
        put("timestamp", timestamp)
        put("balanceAfterPayment", balanceAfterPayment)
        put("referenceCode", referenceCode)
        put("notes", notes)
    }
}

fun JSONObject.toCreditPayment(): CreditPayment {
    return CreditPayment(
        id = optString("id", "payment_${System.currentTimeMillis()}"),
        amount = optDouble("amount", 0.0),
        method = optString("method", "Cash"),
        timestamp = if (has("timestamp")) optLong("timestamp", 0L) else 0L,
        balanceAfterPayment = optDouble("balanceAfterPayment", 0.0),
        referenceCode = optString("referenceCode", ""),
        notes = optString("notes", "")
    )
}

fun CustomerCredit.toJson(): JSONObject {
    val paymentHistoryArray = JSONArray()
    paymentHistory.forEach { paymentHistoryArray.put(it.toJson()) }
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
        put("createdAt", createdAt)
        put("settledAt", settledAt ?: "")
        put("paymentHistory", paymentHistoryArray)
        put("shopId", shopId)
        put("timestamp", timestamp)
    }
}

fun JSONObject.toCustomerCredit(): CustomerCredit {
    val recordId = getString("id")
    val defaultTimestamp = recordId.removePrefix("c_").toLongOrNull() ?: System.currentTimeMillis()
    val paymentHistoryArray = optJSONArray("paymentHistory")
    val paymentHistory = mutableListOf<CreditPayment>()
    if (paymentHistoryArray != null) {
        for (i in 0 until paymentHistoryArray.length()) {
            paymentHistory.add(paymentHistoryArray.getJSONObject(i).toCreditPayment())
        }
    }

    val createdAtVal = if (has("createdAt")) optLong("createdAt", defaultTimestamp) else defaultTimestamp

    return CustomerCredit(
        id = recordId,
        customerName = getString("customerName"),
        customerPhone = optString("customerPhone", ""),
        totalOutstanding = getDouble("totalOutstanding"),
        productsSummary = optString("productsSummary", ""),
        amountPaid = optDouble("amountPaid", 0.0),
        balance = getDouble("balance"),
        dueDate = optString("dueDate", ""),
        status = optString("status", "Pending"),
        paymentHistory = paymentHistory,
        createdAt = createdAtVal,
        settledAt = if (has("settledAt")) optString("settledAt", "").takeIf { it.isNotBlank() } else null,
        shopId = optString("shopId", "").takeIf { it.isNotBlank() },
        timestamp = optLong("timestamp", createdAtVal)
    )
}

fun Employee.toJson(): JSONObject {
    val permsArray = JSONArray()
    permissions.forEach { permsArray.put(it) }
    return JSONObject().apply {
        put("id", id)
        put("name", name)
        put("role", role)
        put("phone", phone)
        put("email", email)
        put("isOwner", isOwner)
        put("shopId", shopId)
        put("status", status)
        put("permissions", permsArray)
        put("authUserId", authUserId)
        put("createdAt", createdAt)
    }
}

fun JSONObject.toEmployee(): Employee {
    val isOwner = optBoolean("isOwner", false)
    val permsList = mutableListOf<String>()
    val permsArray = optJSONArray("permissions")
    if (permsArray != null) {
        for (i in 0 until permsArray.length()) {
            permsList.add(permsArray.getString(i))
        }
    } else {
        if (isOwner) {
            permsList.addAll(EmployeePermissions.ALL_PERMISSIONS)
        } else {
            permsList.add(EmployeePermissions.PERM_RECORD_SALES)
            permsList.add(EmployeePermissions.PERM_STOCK_IN)
        }
    }

    return Employee(
        id = getString("id"),
        name = getString("name"),
        role = optString("role", if (isOwner) "Owner" else "Cashier"),
        phone = optString("phone", ""),
        email = optString("email", ""),
        isOwner = isOwner,
        shopId = optString("shopId", "").takeIf { it.isNotBlank() },
        status = optString("status", if (isOwner) "ACTIVE" else "PENDING"),
        permissions = permsList,
        authUserId = optString("authUserId", "").takeIf { it.isNotBlank() },
        createdAt = optLong("createdAt", System.currentTimeMillis())
    )
}

fun ApprovalRequest.toJson(): JSONObject {
    return JSONObject().apply {
        put("id", id)
        put("type", type)
        put("requesterId", requesterId)
        put("requesterName", requesterName)
        put("title", title)
        put("description", description)
        put("payloadJson", payloadJson)
        put("status", status)
        put("createdAt", createdAt)
        put("reviewedAt", reviewedAt)
        put("reviewedBy", reviewedBy)
        put("shopId", shopId)
    }
}

fun JSONObject.toApprovalRequest(): ApprovalRequest {
    return ApprovalRequest(
        id = getString("id"),
        type = getString("type"),
        requesterId = getString("requesterId"),
        requesterName = getString("requesterName"),
        title = getString("title"),
        description = getString("description"),
        payloadJson = getString("payloadJson"),
        status = optString("status", "PENDING"),
        createdAt = optLong("createdAt", System.currentTimeMillis()),
        reviewedAt = if (has("reviewedAt") && !isNull("reviewedAt")) getLong("reviewedAt") else null,
        reviewedBy = optString("reviewedBy", "").takeIf { it.isNotBlank() },
        shopId = optString("shopId", "").takeIf { it.isNotBlank() }
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
        put("dailyTarget", dailyTarget)
    }
}

fun JSONObject.toShop(): Shop {
    return Shop(
        id = optString("id", ""),
        name = optString("name", ""),
        ownerName = optString("ownerName", ""),
        email = optString("email", ""),
        phone = optString("phone", ""),
        category = optString("category", ""),
        address = optString("address", ""),
        currency = optString("currency", "KSh"),
        dailyTarget = optDouble("dailyTarget", 30000.0)
    )
}
