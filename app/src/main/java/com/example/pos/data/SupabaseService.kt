package com.example.pos.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SupabaseService(
    private val baseUrl: String = SupabaseConfig.URL,
    private val apiKey: String = SupabaseConfig.API_KEY
) {
    @Volatile
    private var accessToken: String? = null

    @Volatile
    private var refreshToken: String? = null

    @Volatile
    private var tokenExpiresAt: Long = 0L

    @Volatile
    private var currentUserId: String? = null

    var onSessionRefreshedListener: ((AuthSession) -> Unit)? = null

    fun setTokens(access: String?, refresh: String? = null, expiresAt: Long = 0L, userId: String? = null) {
        this.accessToken = access
        this.refreshToken = refresh
        this.tokenExpiresAt = expiresAt
        if (!userId.isNullOrBlank()) {
            this.currentUserId = userId
        }
    }

    fun setAccessToken(token: String?) {
        this.accessToken = token
    }

    fun getAccessToken(): String? = accessToken

    fun getRefreshToken(): String? = refreshToken

    fun getTokenExpiresAt(): Long = tokenExpiresAt

    fun getUserId(): String? = currentUserId

    fun extractUserId(responseBody: String): String {
        return try {
            val obj = JSONObject(responseBody)
            val userObj = obj.optJSONObject("user")
            userObj?.optString("id")?.takeIf { it.isNotBlank() }
                ?: obj.optString("id").takeIf { it.isNotBlank() }
                ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun buildRequest(
        path: String,
        method: String = "GET",
        bodyString: String? = null,
        extraHeaders: Map<String, String> = emptyMap()
    ): Request {
        val url = if (path.startsWith("http")) path else "$baseUrl$path"
        val builder = Request.Builder()
            .url(url)
            .addHeader("apikey", apiKey)
            .addHeader("Authorization", "Bearer ${accessToken ?: apiKey}")

        extraHeaders.forEach { (k, v) -> builder.addHeader(k, v) }

        when (method.uppercase()) {
            "GET" -> builder.get()
            "POST" -> {
                val body = (bodyString ?: "{}").toRequestBody(jsonMediaType)
                builder.post(body)
            }
            "PATCH" -> {
                val body = (bodyString ?: "{}").toRequestBody(jsonMediaType)
                builder.patch(body)
            }
            "DELETE" -> builder.delete()
        }
        return builder.build()
    }

    fun parseAuthSession(responseBody: String, rememberMe: Boolean = true): AuthSession? {
        return try {
            val obj = JSONObject(responseBody)
            val token = obj.optString("access_token")
            val refresh = obj.optString("refresh_token")
            val expiresIn = obj.optLong("expires_in", 3600L)
            val userObj = obj.optJSONObject("user")
            val email = userObj?.optString("email") ?: obj.optString("email", "")
            val userId = userObj?.optString("id") ?: obj.optString("id", "")
            if (userId.isNotBlank()) {
                currentUserId = userId
            }
            if (token.isBlank()) return null
            val expiresAt = System.currentTimeMillis() + (expiresIn * 1000L)

            AuthSession(
                accessToken = token,
                refreshToken = refresh,
                userEmail = email,
                userId = userId,
                tokenExpiresAt = expiresAt,
                rememberMe = rememberMe,
                loginTimestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            null
        }
    }

    // ==========================================
    // AUTHENTICATION
    // ==========================================
    suspend fun signUp(
        email: String,
        pass: String,
        userData: Map<String, Any?> = emptyMap()
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("email", email)
                put("password", pass)
                if (userData.isNotEmpty()) {
                    val dataObj = JSONObject()
                    userData.forEach { (k, v) ->
                        if (v != null) dataObj.put(k, v)
                    }
                    put("data", dataObj)
                }
            }
            val request = buildRequest(
                path = "/auth/v1/signup",
                method = "POST",
                bodyString = json.toString()
            )
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val uid = extractUserId(body)
                    if (uid.isNotBlank()) {
                        currentUserId = uid
                    }
                    val session = parseAuthSession(body, true)
                    if (session != null) {
                        setTokens(session.accessToken, session.refreshToken, session.tokenExpiresAt, session.userId)
                    } else {
                        accessToken = JSONObject(body).optString("access_token").takeIf { it.isNotBlank() }
                    }
                    Result.success(body)
                } else {
                    val msg = parseErrorMessage(body, "Signup failed (${response.code})")
                    Result.failure(Exception(msg))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, pass: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("email", email)
                put("password", pass)
            }
            val request = buildRequest(
                path = "/auth/v1/token?grant_type=password",
                method = "POST",
                bodyString = json.toString()
            )
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val session = parseAuthSession(body, true)
                    if (session != null) {
                        setTokens(session.accessToken, session.refreshToken, session.tokenExpiresAt, session.userId)
                    } else {
                        accessToken = JSONObject(body).optString("access_token").takeIf { it.isNotBlank() }
                    }
                    Result.success(body)
                } else {
                    val msg = parseErrorMessage(body, "Login failed (${response.code})")
                    Result.failure(Exception(msg))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshSession(refreshTokenStr: String, rememberMe: Boolean = true): Result<AuthSession> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("refresh_token", refreshTokenStr)
            }
            val request = buildRequest(
                path = "/auth/v1/token?grant_type=refresh_token",
                method = "POST",
                bodyString = json.toString()
            )
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val session = parseAuthSession(body, rememberMe)
                    if (session != null) {
                        setTokens(session.accessToken, session.refreshToken, session.tokenExpiresAt)
                        onSessionRefreshedListener?.invoke(session)
                        Result.success(session)
                    } else {
                        Result.failure(Exception("Failed to parse refreshed token"))
                    }
                } else {
                    val msg = parseErrorMessage(body, "Token refresh failed (${response.code})")
                    Result.failure(Exception(msg))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun clearSession() {
        accessToken = null
        refreshToken = null
        tokenExpiresAt = 0L
    }

    suspend fun resetPasswordForEmail(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("email", email)
            }
            val request = buildRequest(
                path = "/auth/v1/recover",
                method = "POST",
                bodyString = json.toString()
            )
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    val body = response.body?.string().orEmpty()
                    val msg = parseErrorMessage(body, "Reset failed (${response.code})")
                    Result.failure(Exception(msg))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // SHOP
    // ==========================================
    suspend fun getShop(
        userEmail: String? = null,
        shopId: String? = null,
        email: String? = null
    ): Result<Shop?> = withContext(Dispatchers.IO) {
        try {
            val resolvedEmail = userEmail?.takeIf { it.isNotBlank() } ?: email?.takeIf { it.isNotBlank() }
            val queryPath = when {
                !shopId.isNullOrBlank() -> "/rest/v1/shops?id=eq.$shopId&select=*&limit=1"
                !resolvedEmail.isNullOrBlank() -> "/rest/v1/shops?email=eq.$resolvedEmail&select=*&limit=1"
                else -> return@withContext Result.success(null)
            }
            val req = buildRequest(queryPath)
            client.newCall(req).execute().use { resp ->
                val str = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) return@withContext Result.failure(Exception("Failed to fetch shop: ${resp.code}"))
                val arr = JSONArray(str)
                if (arr.length() == 0) {
                    return@withContext Result.success(null)
                }
                val obj = arr.getJSONObject(0)
                Result.success(parseShopJson(obj))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseShopJson(obj: JSONObject): Shop {
        return Shop(
            id = obj.optString("id", ""),
            name = obj.optString("name", ""),
            ownerName = obj.optString("owner_name", ""),
            email = obj.optString("email", ""),
            phone = obj.optString("phone", ""),
            category = obj.optString("category", ""),
            address = obj.optString("address", ""),
            currency = obj.optString("currency", "KSh")
        )
    }

    suspend fun saveShop(shop: Shop): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // First check if a shop with this email already exists
            val existing = if (shop.email.isNotBlank()) getShop(shop.email).getOrNull() else null
            val targetId = existing?.id?.takeIf { it.isNotBlank() && it != "shop_1" }
                ?: shop.id.takeIf { it.isNotBlank() && it != "shop_1" }
                ?: currentUserId?.takeIf { it.isNotBlank() }
                ?: "shop_${shop.email.substringBefore('@').ifBlank { "main" }}"

            val json = JSONObject().apply {
                put("id", targetId)
                put("name", shop.name)
                put("owner_name", shop.ownerName)
                put("email", shop.email)
                put("phone", shop.phone)
                put("category", shop.category)
                put("address", shop.address)
                put("currency", shop.currency)
            }

            if (existing != null) {
                // Update the existing row so no duplicate row is ever inserted
                val patchReq = buildRequest(
                    path = "/rest/v1/shops?id=eq.${existing.id}",
                    method = "PATCH",
                    bodyString = json.toString()
                )
                client.newCall(patchReq).execute().use { patchResp ->
                    if (patchResp.isSuccessful) return@withContext Result.success(Unit)
                }
            }

            val req = buildRequest(
                path = "/rest/v1/shops",
                method = "POST",
                bodyString = json.toString(),
                extraHeaders = mapOf("Prefer" to "resolution=merge-duplicates")
            )
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) return@withContext Result.success(Unit)
            }

            val patchPath = if (shop.email.isNotBlank()) {
                "/rest/v1/shops?email=eq.${shop.email}"
            } else {
                "/rest/v1/shops?id=eq.$targetId"
            }
            val patchReq = buildRequest(
                path = patchPath,
                method = "PATCH",
                bodyString = json.toString()
            )
            client.newCall(patchReq).execute().use { patchResp ->
                if (patchResp.isSuccessful) Result.success(Unit)
                else Result.failure(Exception("Failed to update shop (${patchResp.code})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // PRODUCTS
    // ==========================================
    suspend fun getProducts(shopId: String? = null): Result<List<Product>> = withContext(Dispatchers.IO) {
        try {
            if (shopId.isNullOrBlank()) {
                return@withContext Result.success(emptyList())
            }
            val filter = "shop_id=eq.$shopId&"
            val req = buildRequest("/rest/v1/products?${filter}select=*&order=created_at.asc")
            client.newCall(req).execute().use { resp ->
                val str = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) return@withContext Result.failure(Exception("Failed to fetch products: ${resp.code}"))
                val arr = JSONArray(str)
                val list = mutableListOf<Product>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val wholesalePrice = obj.optDouble("wholesale_price", Double.NaN).takeIf { !it.isNaN() && it > 0 }
                    val wholesaleQuantity = obj.optInt("wholesale_quantity", -1).takeIf { it > 0 }
                    list.add(
                        Product(
                            id = obj.optString("id"),
                            name = obj.optString("name"),
                            category = obj.optString("category"),
                            sku = obj.optString("sku"),
                            unit = obj.optString("unit", "pcs"),
                            buyingPrice = obj.optDouble("buying_price", 0.0),
                            sellingPrice = obj.optDouble("selling_price", 0.0),
                            stockQuantity = obj.optInt("stock_quantity", 0),
                            minStock = obj.optInt("min_stock", 10),
                            supplier = obj.optString("supplier", ""),
                            iconType = obj.optString("icon_type", "box"),
                            imageUri = obj.optString("image_uri", "").takeIf { it.isNotBlank() && it != "null" },
                            shopId = obj.optString("shop_id", "").takeIf { it.isNotBlank() },
                            wholesalePrice = wholesalePrice,
                            wholesaleQuantity = wholesaleQuantity
                        )
                    )
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun upsertProduct(product: Product, shopId: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val resolvedShopId = shopId ?: product.shopId
            val json = JSONObject().apply {
                put("id", product.id)
                put("name", product.name)
                put("category", product.category)
                // Note: 'sku', 'image_uri', 'wholesale_price', and 'wholesale_quantity' are retained in local offline storage
                // because the remote Supabase 'products' table schema lacks these columns; sending them returns HTTP 400 (PGRST204).
                put("unit", product.unit)
                put("buying_price", product.buyingPrice)
                put("selling_price", product.sellingPrice)
                put("stock_quantity", product.stockQuantity)
                put("min_stock", product.minStock)
                put("supplier", product.supplier)
                put("icon_type", product.iconType)
                if (!resolvedShopId.isNullOrBlank()) {
                    put("shop_id", resolvedShopId)
                }
            }
            val req = buildRequest(
                path = "/rest/v1/products?on_conflict=id",
                method = "POST",
                bodyString = json.toString(),
                extraHeaders = mapOf("Prefer" to "resolution=merge-duplicates")
            )
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) Result.success(Unit)
                else Result.failure(Exception("Failed to save product (${resp.code}): ${resp.body?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(productId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val req = buildRequest(
                path = "/rest/v1/products?id=eq.$productId",
                method = "DELETE"
            )
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) Result.success(Unit)
                else Result.failure(Exception("Failed to delete product (${resp.code})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProductStock(productId: String, newStock: Int, shopId: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("stock_quantity", newStock)
            }
            val path = if (!shopId.isNullOrBlank()) {
                "/rest/v1/products?id=eq.$productId&shop_id=eq.$shopId"
            } else {
                "/rest/v1/products?id=eq.$productId"
            }
            val req = buildRequest(
                path = path,
                method = "PATCH",
                bodyString = json.toString()
            )
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) Result.success(Unit)
                else Result.failure(Exception("Failed to update product stock (${resp.code})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // SALES
    // ==========================================
    suspend fun getSales(shopId: String? = null): Result<List<Sale>> = withContext(Dispatchers.IO) {
        try {
            if (shopId.isNullOrBlank()) {
                return@withContext Result.success(emptyList())
            }
            val filter = "shop_id=eq.$shopId&"
            val req = buildRequest("/rest/v1/sales?${filter}select=*&order=created_at.desc")
            client.newCall(req).execute().use { resp ->
                val str = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) return@withContext Result.failure(Exception("Failed to fetch sales: ${resp.code}"))
                val arr = JSONArray(str)
                val list = mutableListOf<Sale>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val itemsArr = obj.optJSONArray("items") ?: JSONArray()
                    val itemsList = mutableListOf<CartItem>()
                    for (j in 0 until itemsArr.length()) {
                        val itemObj = itemsArr.getJSONObject(j)
                        val prodObj = itemObj.optJSONObject("product")
                        if (prodObj != null) {
                            val prod = Product(
                                id = prodObj.optString("id"),
                                name = prodObj.optString("name"),
                                category = prodObj.optString("category"),
                                sku = prodObj.optString("sku"),
                                unit = prodObj.optString("unit", "pcs"),
                                buyingPrice = prodObj.optDouble("buying_price", 0.0),
                                sellingPrice = prodObj.optDouble("selling_price", 0.0),
                                stockQuantity = prodObj.optInt("stock_quantity", 0),
                                minStock = prodObj.optInt("min_stock", 10),
                                supplier = prodObj.optString("supplier", ""),
                                iconType = prodObj.optString("icon_type", "box"),
                                shopId = prodObj.optString("shop_id", "").takeIf { it.isNotBlank() }
                            )
                            itemsList.add(CartItem(prod, itemObj.optInt("quantity", 1)))
                        }
                    }
                    list.add(
                        Sale(
                            id = obj.optString("id"),
                            items = itemsList,
                            totalAmount = obj.optDouble("total_amount", 0.0),
                            paymentMethod = obj.optString("payment_method", "Cash"),
                            customerName = obj.optString("customer_name", ""),
                            dateFormatted = obj.optString("date_formatted", ""),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            employeeName = obj.optString("employee_name", "Store Owner"),
                            isCreditSale = obj.optBoolean("is_credit_sale", false),
                            paidAmount = obj.optDouble("paid_amount", 0.0),
                            balance = obj.optDouble("balance", 0.0),
                            dueDate = obj.optString("due_date", ""),
                            shopId = obj.optString("shop_id", "").takeIf { it.isNotBlank() }
                        )
                    )
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertSale(sale: Sale, shopId: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val resolvedShopId = shopId ?: sale.shopId
            val itemsJson = JSONArray()
            for (ci in sale.items) {
                val itemObj = JSONObject().apply {
                    put("quantity", ci.quantity)
                    put("product", JSONObject().apply {
                        put("id", ci.product.id)
                        put("name", ci.product.name)
                        put("category", ci.product.category)
                        put("sku", ci.product.sku)
                        put("unit", ci.product.unit)
                        put("buying_price", ci.product.buyingPrice)
                        put("selling_price", ci.product.sellingPrice)
                        put("stock_quantity", ci.product.stockQuantity)
                        put("min_stock", ci.product.minStock)
                        put("supplier", ci.product.supplier)
                        put("icon_type", ci.product.iconType)
                        if (!ci.product.shopId.isNullOrBlank()) {
                            put("shop_id", ci.product.shopId)
                        }
                    })
                }
                itemsJson.put(itemObj)
            }

            val json = JSONObject().apply {
                put("id", sale.id)
                put("total_amount", sale.totalAmount)
                put("payment_method", sale.paymentMethod)
                put("customer_name", sale.customerName)
                put("date_formatted", sale.dateFormatted)
                put("timestamp", sale.timestamp)
                put("employee_name", sale.employeeName)
                put("is_credit_sale", sale.isCreditSale)
                put("paid_amount", sale.paidAmount)
                put("balance", sale.balance)
                put("due_date", sale.dueDate)
                put("items", itemsJson)
                if (!resolvedShopId.isNullOrBlank()) {
                    put("shop_id", resolvedShopId)
                }
            }
            val req = buildRequest(
                path = "/rest/v1/sales",
                method = "POST",
                bodyString = json.toString()
            )
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) Result.success(Unit)
                else Result.failure(Exception("Failed to save sale (${resp.code})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun upsertSale(sale: Sale, shopId: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val resolvedShopId = shopId
            val itemsJson = JSONArray()
            for (ci in sale.items) {
                val itemObj = JSONObject().apply {
                    put("quantity", ci.quantity)
                    put("product", JSONObject().apply {
                        put("id", ci.product.id)
                        put("name", ci.product.name)
                        put("category", ci.product.category)
                        put("sku", ci.product.sku)
                        put("unit", ci.product.unit)
                        put("buying_price", ci.product.buyingPrice)
                        put("selling_price", ci.product.sellingPrice)
                        put("stock_quantity", ci.product.stockQuantity)
                        put("min_stock", ci.product.minStock)
                        put("supplier", ci.product.supplier)
                        put("icon_type", ci.product.iconType)
                    })
                }
                itemsJson.put(itemObj)
            }

            val json = JSONObject().apply {
                put("id", sale.id)
                put("total_amount", sale.totalAmount)
                put("payment_method", sale.paymentMethod)
                put("customer_name", sale.customerName)
                put("date_formatted", sale.dateFormatted)
                put("timestamp", sale.timestamp)
                put("employee_name", sale.employeeName)
                put("is_credit_sale", sale.isCreditSale)
                put("paid_amount", sale.paidAmount)
                put("balance", sale.balance)
                put("due_date", sale.dueDate)
                put("items", itemsJson)
                if (!resolvedShopId.isNullOrBlank()) {
                    put("shop_id", resolvedShopId)
                }
            }
            val req = buildRequest(
                path = "/rest/v1/sales",
                method = "POST",
                bodyString = json.toString(),
                extraHeaders = mapOf("Prefer" to "resolution=merge-duplicates")
            )
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) Result.success(Unit)
                else Result.failure(Exception("Failed to upsert sale (${resp.code})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSale(saleId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val req = buildRequest(
                path = "/rest/v1/sales?id=eq.$saleId",
                method = "DELETE"
            )
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) Result.success(Unit)
                else Result.failure(Exception("Failed to delete sale (${resp.code})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // STOCK IN RECORDS
    // ==========================================
    suspend fun getStockInRecords(shopId: String? = null): Result<List<StockInRecord>> = withContext(Dispatchers.IO) {
        try {
            if (shopId.isNullOrBlank()) {
                return@withContext Result.success(emptyList())
            }
            val filter = "shop_id=eq.$shopId&"
            val req = buildRequest("/rest/v1/stock_in_records?${filter}select=*&order=created_at.desc")
            client.newCall(req).execute().use { resp ->
                val str = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) return@withContext Result.failure(Exception("Failed to fetch stock records"))
                val arr = JSONArray(str)
                val list = mutableListOf<StockInRecord>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        StockInRecord(
                            id = obj.optString("id"),
                            productId = obj.optString("product_id"),
                            productName = obj.optString("product_name"),
                            quantityAdded = obj.optInt("quantity_added"),
                            buyingPrice = obj.optDouble("buying_price"),
                            supplier = obj.optString("supplier"),
                            dateFormatted = obj.optString("date_formatted"),
                            employeeName = obj.optString("employee_name"),
                            shopId = obj.optString("shop_id", "").takeIf { it.isNotBlank() }
                        )
                    )
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertStockInRecord(record: StockInRecord, shopId: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val resolvedShopId = shopId ?: record.shopId
            val json = JSONObject().apply {
                put("id", record.id)
                put("product_id", record.productId)
                put("product_name", record.productName)
                put("quantity_added", record.quantityAdded)
                put("buying_price", record.buyingPrice)
                put("supplier", record.supplier)
                put("date_formatted", record.dateFormatted)
                put("employee_name", record.employeeName)
                if (!resolvedShopId.isNullOrBlank()) {
                    put("shop_id", resolvedShopId)
                }
            }
            val req = buildRequest(
                path = "/rest/v1/stock_in_records",
                method = "POST",
                bodyString = json.toString()
            )
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) Result.success(Unit)
                else Result.failure(Exception("Failed to save stock in record (${resp.code})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteStockInRecord(recordId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val req = buildRequest(
                path = "/rest/v1/stock_in_records?id=eq.$recordId",
                method = "DELETE"
            )
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) Result.success(Unit)
                else Result.failure(Exception("Failed to delete stock in record (${resp.code})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // EXPENSES
    // ==========================================
    suspend fun getExpenses(shopId: String? = null): Result<List<Expense>> = withContext(Dispatchers.IO) {
        try {
            if (shopId.isNullOrBlank()) {
                return@withContext Result.success(emptyList())
            }
            val filter = "shop_id=eq.$shopId&"
            val req = buildRequest("/rest/v1/expenses?${filter}select=*&order=created_at.desc")
            client.newCall(req).execute().use { resp ->
                val str = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) return@withContext Result.failure(Exception("Failed to fetch expenses"))
                val arr = JSONArray(str)
                val list = mutableListOf<Expense>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        Expense(
                            id = obj.optString("id"),
                            title = obj.optString("title"),
                            category = obj.optString("category"),
                            amount = obj.optDouble("amount"),
                            dateFormatted = obj.optString("date_formatted"),
                            iconType = obj.optString("icon_type", "receipt"),
                            shopId = obj.optString("shop_id", "").takeIf { it.isNotBlank() }
                        )
                    )
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertExpense(expense: Expense, shopId: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val resolvedShopId = shopId ?: expense.shopId
            val json = JSONObject().apply {
                put("id", expense.id)
                put("title", expense.title)
                put("category", expense.category)
                put("amount", expense.amount)
                put("date_formatted", expense.dateFormatted)
                put("icon_type", expense.iconType)
                if (!resolvedShopId.isNullOrBlank()) {
                    put("shop_id", resolvedShopId)
                }
            }
            val req = buildRequest(
                path = "/rest/v1/expenses",
                method = "POST",
                bodyString = json.toString()
            )
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) Result.success(Unit)
                else Result.failure(Exception("Failed to save expense (${resp.code})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun upsertExpense(expense: Expense, shopId: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val resolvedShopId = shopId ?: expense.shopId
            val json = JSONObject().apply {
                put("id", expense.id)
                put("title", expense.title)
                put("category", expense.category)
                put("amount", expense.amount)
                put("date_formatted", expense.dateFormatted)
                put("icon_type", expense.iconType)
                if (!resolvedShopId.isNullOrBlank()) {
                    put("shop_id", resolvedShopId)
                }
            }
            val req = buildRequest(
                path = "/rest/v1/expenses",
                method = "POST",
                bodyString = json.toString(),
                extraHeaders = mapOf("Prefer" to "resolution=merge-duplicates")
            )
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) Result.success(Unit)
                else Result.failure(Exception("Failed to update expense (${resp.code})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteExpense(expenseId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val req = buildRequest(
                path = "/rest/v1/expenses?id=eq.$expenseId",
                method = "DELETE"
            )
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) Result.success(Unit)
                else Result.failure(Exception("Failed to delete expense (${resp.code})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // CUSTOMER CREDITS
    // ==========================================
    suspend fun getCustomerCredits(shopId: String? = null): Result<List<CustomerCredit>> = withContext(Dispatchers.IO) {
        try {
            if (shopId.isNullOrBlank()) {
                return@withContext Result.success(emptyList())
            }
            val filter = "shop_id=eq.$shopId&"
            val req = buildRequest("/rest/v1/customer_credits?${filter}select=*&order=created_at.desc")
            client.newCall(req).execute().use { resp ->
                val str = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) return@withContext Result.failure(Exception("Failed to fetch credits"))
                val arr = JSONArray(str)
                val list = mutableListOf<CustomerCredit>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        CustomerCredit(
                            id = obj.optString("id"),
                            customerName = obj.optString("customer_name"),
                            customerPhone = obj.optString("customer_phone"),
                            totalOutstanding = obj.optDouble("total_outstanding"),
                            productsSummary = obj.optString("products_summary"),
                            amountPaid = obj.optDouble("amount_paid"),
                            balance = obj.optDouble("balance"),
                            dueDate = obj.optString("due_date"),
                            status = obj.optString("status", "Pending"),
                            shopId = obj.optString("shop_id", "").takeIf { it.isNotBlank() }
                        )
                    )
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun upsertCustomerCredit(credit: CustomerCredit, shopId: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val resolvedShopId = shopId ?: credit.shopId
            val json = JSONObject().apply {
                put("id", credit.id)
                put("customer_name", credit.customerName)
                put("customer_phone", credit.customerPhone)
                put("total_outstanding", credit.totalOutstanding)
                put("products_summary", credit.productsSummary)
                put("amount_paid", credit.amountPaid)
                put("balance", credit.balance)
                put("due_date", credit.dueDate)
                put("status", credit.status)
                if (!resolvedShopId.isNullOrBlank()) {
                    put("shop_id", resolvedShopId)
                }
            }
            val req = buildRequest(
                path = "/rest/v1/customer_credits",
                method = "POST",
                bodyString = json.toString(),
                extraHeaders = mapOf("Prefer" to "resolution=merge-duplicates")
            )
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) Result.success(Unit)
                else Result.failure(Exception("Failed to save credit (${resp.code})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // EMPLOYEES
    // ==========================================
    fun parseEmployeeJson(obj: JSONObject): Employee {
        val isOwner = obj.optBoolean("is_owner", false)
        val rawRole = obj.optString("role", if (isOwner) "Owner" else "Cashier")

        var displayRole = rawRole
        var status = if (isOwner) "ACTIVE" else "ACTIVE"
        var perms: List<String> = if (isOwner) EmployeePermissions.ALL_PERMISSIONS else listOf(
            EmployeePermissions.PERM_RECORD_SALES,
            EmployeePermissions.PERM_STOCK_IN
        )
        var authUid: String? = null

        if (rawRole.contains("||")) {
            val parts = rawRole.split("||")
            displayRole = parts[0]
            parts.forEach { part ->
                when {
                    part.startsWith("status:") -> status = part.removePrefix("status:")
                    part.startsWith("perms:") -> {
                        val pStr = part.removePrefix("perms:")
                        if (pStr.isNotBlank()) perms = pStr.split(",").map { it.trim() }
                    }
                    part.startsWith("authUid:") -> authUid = part.removePrefix("authUid:").takeIf { it.isNotBlank() }
                }
            }
        }

        return Employee(
            id = obj.optString("id"),
            name = obj.optString("name"),
            role = displayRole,
            phone = obj.optString("phone"),
            email = obj.optString("email"),
            isOwner = isOwner,
            shopId = obj.optString("shop_id", "").takeIf { it.isNotBlank() },
            status = status,
            permissions = perms,
            authUserId = authUid
        )
    }

    suspend fun getEmployeeByEmail(email: String): Result<Employee?> = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = email.trim()
            if (cleanEmail.isBlank()) return@withContext Result.success(null)
            val req = buildRequest("/rest/v1/employees?email=eq.$cleanEmail&select=*&limit=1")
            client.newCall(req).execute().use { resp ->
                val str = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) return@withContext Result.failure(Exception("Failed to fetch employee"))
                val arr = JSONArray(str)
                if (arr.length() == 0) return@withContext Result.success(null)
                Result.success(parseEmployeeJson(arr.getJSONObject(0)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getEmployees(shopId: String? = null): Result<List<Employee>> = withContext(Dispatchers.IO) {
        try {
            if (shopId.isNullOrBlank()) {
                return@withContext Result.success(emptyList())
            }
            val filter = "shop_id=eq.$shopId&"
            val req = buildRequest("/rest/v1/employees?${filter}select=*&order=created_at.asc")
            client.newCall(req).execute().use { resp ->
                val str = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) return@withContext Result.failure(Exception("Failed to fetch employees"))
                val arr = JSONArray(str)
                val list = mutableListOf<Employee>()
                for (i in 0 until arr.length()) {
                    list.add(parseEmployeeJson(arr.getJSONObject(i)))
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun upsertEmployee(employee: Employee, shopId: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val resolvedShopId = shopId ?: employee.shopId
            val encodedRole = buildString {
                append(employee.role.substringBefore("||"))
                append("||status:").append(employee.status)
                append("||perms:").append(employee.permissions.joinToString(","))
                if (!employee.authUserId.isNullOrBlank()) {
                    append("||authUid:").append(employee.authUserId)
                }
            }

            val json = JSONObject().apply {
                put("id", employee.id)
                put("name", employee.name)
                put("role", encodedRole)
                put("phone", employee.phone)
                put("email", employee.email)
                put("is_owner", employee.isOwner)
                if (!resolvedShopId.isNullOrBlank()) {
                    put("shop_id", resolvedShopId)
                }
            }
            val req = buildRequest(
                path = "/rest/v1/employees",
                method = "POST",
                bodyString = json.toString(),
                extraHeaders = mapOf("Prefer" to "resolution=merge-duplicates")
            )
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) Result.success(Unit)
                else Result.failure(Exception("Failed to save employee (${resp.code})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteEmployee(employeeId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val req = buildRequest(
                path = "/rest/v1/employees?id=eq.$employeeId",
                method = "DELETE"
            )
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) Result.success(Unit)
                else Result.failure(Exception("Failed to delete employee (${resp.code})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun dispatchEdgeFunction(functionName: String, payload: Map<String, Any>): Result<String?> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject()
            payload.forEach { (k, v) ->
                when (v) {
                    is Map<*, *> -> {
                        val inner = JSONObject()
                        v.forEach { (ik, iv) -> inner.put(ik.toString(), iv) }
                        json.put(k, inner)
                    }
                    else -> json.put(k, v)
                }
            }
            val req = buildRequest(
                path = "/functions/v1/$functionName",
                method = "POST",
                bodyString = json.toString()
            )
            client.newCall(req).execute().use { resp ->
                val bodyStr = resp.body?.string().orEmpty()
                if (resp.isSuccessful) {
                    // A successful HTTP response means the function accepted the request;
                    // it does not prove provider delivery. Never invent a provider message ID.
                    val msgId = runCatching {
                        JSONObject(bodyStr).optString("message_id").takeIf { it.isNotBlank() }
                    }.getOrNull()
                    Result.success(msgId)
                } else {
                    val detail = parseErrorMessage(bodyStr, "")
                    Result.failure(EdgeFunctionHttpException(
                        statusCode = resp.code,
                        message = edgeFunctionHttpErrorMessage(functionName, resp.code, detail)
                    ))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseErrorMessage(responseBody: String, fallback: String): String {
        return try {
            val obj = JSONObject(responseBody)
            obj.optString("msg", obj.optString("message", obj.optString("error_description", fallback)))
        } catch (e: Exception) {
            fallback
        }
    }
}

internal class EdgeFunctionHttpException(
    val statusCode: Int,
    message: String
) : Exception(message)

internal fun edgeFunctionHttpErrorMessage(functionName: String, statusCode: Int, detail: String = ""): String {
    val function = functionName.replace(Regex("[^a-zA-Z0-9_-]"), "")
    val reason = when (statusCode) {
        404 -> "function endpoint was not found in the configured Supabase project"
        401 -> "request was not authenticated"
        403 -> "request was not authorized by the function"
        429 -> "function or provider rate limit was reached"
        in 500..599 -> "Supabase function returned a server error"
        else -> "Supabase function rejected the request"
    }
    val safeDetail = detail.trim().replace(Regex("(?i)bearer\\s+[^\\s,]+"), "Bearer [redacted]")
        .replace(Regex("(?i)(access[_ -]?token|api[_ -]?key|secret)\\s*[:=]\\s*[^\\s,]+"), "\$1=[redacted]")
        .take(300)
    return buildString {
        append("$function $reason (HTTP $statusCode).")
        if (safeDetail.isNotBlank()) append(" ").append(safeDetail)
    }
}
