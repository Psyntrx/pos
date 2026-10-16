package com.example.pos.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ReportTimeScope(val displayName: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    CUSTOM("Custom Range")
}

data class TopProductSummary(
    val productId: String,
    val name: String,
    val category: String,
    val unitsSold: Int,
    val revenue: Double
)

data class ReportFinancialSummary(
    val scope: ReportTimeScope,
    val periodLabel: String,
    val dateRangeText: String,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val revenue: Double,
    val cogs: Double,
    val grossProfit: Double,
    val grossProfitMargin: Double, // percentage 0-100
    val totalExpenses: Double,
    val netProfit: Double,
    val netProfitMargin: Double, // percentage -100 to 100
    val transactionCount: Int,
    val averageBasketSize: Double,
    val paymentBreakdown: Map<String, Double>,
    val paymentCounts: Map<String, Int>,
    val expensesByCategory: Map<String, Double>,
    val creditSalesAmount: Double,
    val creditCollections: Double,
    val creditOutstandingBalance: Double,
    val topSellingProducts: List<TopProductSummary>,
    val salesList: List<Sale>,
    val expensesList: List<Expense>,
    val creditsList: List<CustomerCredit>
)

object ReportCalculator {

    fun computeSummary(
        scope: ReportTimeScope,
        periodLabel: String,
        dateRangeText: String,
        startEpochMs: Long,
        endEpochMs: Long,
        allSales: List<Sale>,
        allExpenses: List<Expense>,
        allCredits: List<CustomerCredit>,
        allProducts: List<Product>
    ): ReportFinancialSummary {
        // Filter transactions within [startEpochMs, endEpochMs]
        val filteredSales = allSales.filter { it.timestamp in startEpochMs..endEpochMs }
        val filteredExpenses = allExpenses.filter { it.timestamp in startEpochMs..endEpochMs }
        val filteredCredits = allCredits.filter { it.timestamp in startEpochMs..endEpochMs }

        val revenue = filteredSales.sumOf { it.totalAmount }

        // Compute Cost of Goods Sold (COGS)
        val productMap = allProducts.associateBy { it.id }
        val cogs = filteredSales.sumOf { sale ->
            sale.items.sumOf { item ->
                val prod = productMap[item.product.id] ?: item.product
                val costPerUnit = if (prod.buyingPrice > 0) prod.buyingPrice else prod.sellingPrice * 0.70
                costPerUnit * item.quantity
            }
        }

        val grossProfit = (revenue - cogs).coerceAtLeast(0.0)
        val grossProfitMargin = if (revenue > 0) (grossProfit / revenue) * 100.0 else 0.0

        val totalExpenses = filteredExpenses.sumOf { it.amount }
        val netProfit = grossProfit - totalExpenses
        val netProfitMargin = if (revenue > 0) (netProfit / revenue) * 100.0 else 0.0

        val transactionCount = filteredSales.size
        val avgBasket = if (transactionCount > 0) revenue / transactionCount else 0.0

        // Payment Method breakdown
        val paymentBreakdown = mutableMapOf<String, Double>()
        val paymentCounts = mutableMapOf<String, Int>()
        filteredSales.forEach { sale ->
            val method = if (sale.isCreditSale) "Credit" else sale.paymentMethod.ifBlank { "Cash" }
            paymentBreakdown[method] = (paymentBreakdown[method] ?: 0.0) + sale.totalAmount
            paymentCounts[method] = (paymentCounts[method] ?: 0) + 1
        }

        // Expense by category
        val expensesByCategory = mutableMapOf<String, Double>()
        filteredExpenses.forEach { exp ->
            val cat = exp.category.ifBlank { "General" }
            expensesByCategory[cat] = (expensesByCategory[cat] ?: 0.0) + exp.amount
        }

        // Credit metrics
        val creditSales = filteredSales.filter { it.isCreditSale }
        val creditSalesAmount = creditSales.sumOf { it.totalAmount }
        val creditCollections = creditSales.sumOf { it.paidAmount }
        val creditOutstandingBalance = creditSales.sumOf { it.balance }

        // Top Selling Products aggregation
        val productSalesMap = mutableMapOf<String, Pair<Int, Double>>() // productId -> (unitsSold, totalRevenue)
        val productMetaMap = mutableMapOf<String, Pair<String, String>>() // productId -> (name, category)

        filteredSales.forEach { sale ->
            sale.items.forEach { cartItem ->
                val pid = cartItem.product.id
                val existing = productSalesMap[pid] ?: (0 to 0.0)
                productSalesMap[pid] = (existing.first + cartItem.quantity) to (existing.second + cartItem.totalPrice)
                if (!productMetaMap.containsKey(pid)) {
                    productMetaMap[pid] = cartItem.product.name to cartItem.product.category
                }
            }
        }

        val topProducts = productSalesMap.entries
            .sortedByDescending { it.value.second } // Sort by revenue
            .take(10)
            .map { (pid, salesPair) ->
                val (name, cat) = productMetaMap[pid] ?: ("Product" to "General")
                TopProductSummary(
                    productId = pid,
                    name = name,
                    category = cat,
                    unitsSold = salesPair.first,
                    revenue = salesPair.second
                )
            }

        return ReportFinancialSummary(
            scope = scope,
            periodLabel = periodLabel,
            dateRangeText = dateRangeText,
            startEpochMs = startEpochMs,
            endEpochMs = endEpochMs,
            revenue = revenue,
            cogs = cogs,
            grossProfit = grossProfit,
            grossProfitMargin = grossProfitMargin,
            totalExpenses = totalExpenses,
            netProfit = netProfit,
            netProfitMargin = netProfitMargin,
            transactionCount = transactionCount,
            averageBasketSize = avgBasket,
            paymentBreakdown = paymentBreakdown,
            paymentCounts = paymentCounts,
            expensesByCategory = expensesByCategory,
            creditSalesAmount = creditSalesAmount,
            creditCollections = creditCollections,
            creditOutstandingBalance = creditOutstandingBalance,
            topSellingProducts = topProducts,
            salesList = filteredSales,
            expensesList = filteredExpenses,
            creditsList = filteredCredits
        )
    }

    /**
     * Helper to compute start & end timestamps for Day
     */
    fun getDayRange(calendar: Calendar): Triple<Long, Long, String> {
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis

        val sdf = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
        val text = sdf.format(Date(start))
        return Triple(start, end, text)
    }

    /**
     * Helper to compute start & end timestamps for Week (Monday to Sunday)
     */
    fun getWeekRange(calendar: Calendar): Triple<Long, Long, String> {
        val cal = calendar.clone() as Calendar
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.add(Calendar.DAY_OF_YEAR, 6)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis

        val startFormat = SimpleDateFormat("d MMM", Locale.getDefault())
        val endFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
        val text = "${startFormat.format(Date(start))} – ${endFormat.format(Date(end))}"
        return Triple(start, end, text)
    }

    /**
     * Helper to compute start & end timestamps for Month
     */
    fun getMonthRange(calendar: Calendar): Triple<Long, Long, String> {
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        val lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        cal.set(Calendar.DAY_OF_MONTH, lastDay)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis

        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val text = sdf.format(Date(start))
        return Triple(start, end, text)
    }
}
