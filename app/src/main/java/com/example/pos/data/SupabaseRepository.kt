package com.example.pos.data

import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SupabaseRepository {

    suspend fun fetchProducts(): List<Product>? = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured) return@withContext null
        try {
            SupabaseConfig.client.from("products").select().decodeList<Product>()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun upsertProduct(product: Product): Boolean = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured) return@withContext false
        try {
            SupabaseConfig.client.from("products").upsert(product)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun updateProductStock(productId: String, newStock: Int): Boolean = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured) return@withContext false
        try {
            SupabaseConfig.client.from("products")
                .update({ set("stockQuantity", newStock) }) {
                    filter { eq("id", productId) }
                }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun insertSale(sale: Sale): Boolean = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured) return@withContext false
        try {
            SupabaseConfig.client.from("sales").insert(sale)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun insertStockInRecord(record: StockInRecord): Boolean = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured) return@withContext false
        try {
            SupabaseConfig.client.from("stock_in_records").insert(record)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun insertExpense(expense: Expense): Boolean = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured) return@withContext false
        try {
            SupabaseConfig.client.from("expenses").insert(expense)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun fetchSales(): List<Sale>? = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured) return@withContext null
        try {
            SupabaseConfig.client.from("sales").select().decodeList<Sale>()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
