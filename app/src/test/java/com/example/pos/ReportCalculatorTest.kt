package com.example.pos

import com.example.pos.data.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportCalculatorTest {

    @Test
    fun computeSummaryAccuratelyCalculatesMetricsWithinDateRange() {
        val now = 1000000L
        val past = 500000L
        val future = 2000000L

        val product1 = Product("p1", "Unga", "Groceries", "UG1", "pcs", 100.0, 150.0, 20, 5)
        val product2 = Product("p2", "Milk", "Beverages", "MK1", "pkt", 50.0, 70.0, 15, 5)

        val sales = listOf(
            Sale(
                id = "s1",
                items = listOf(CartItem(product1, 2)), // Revenue 300, COGS 200, Profit 100
                totalAmount = 300.0,
                paymentMethod = "Cash",
                timestamp = now
            ),
            Sale(
                id = "s2",
                items = listOf(CartItem(product2, 3)), // Revenue 210, COGS 150, Profit 60
                totalAmount = 210.0,
                paymentMethod = "M-Pesa",
                timestamp = now + 100
            ),
            // Out of range sale
            Sale(
                id = "s3",
                items = listOf(CartItem(product1, 10)),
                totalAmount = 1500.0,
                paymentMethod = "Cash",
                timestamp = past - 1000
            )
        )

        val expenses = listOf(
            Expense(
                id = "e1",
                title = "Delivery",
                category = "Transport",
                amount = 60.0,
                dateFormatted = "Today",
                timestamp = now + 50
            ),
            // Out of range expense
            Expense(
                id = "e2",
                title = "Old Rent",
                category = "Rent",
                amount = 5000.0,
                dateFormatted = "Past",
                timestamp = past - 2000
            )
        )

        val credits = listOf(
            CustomerCredit(
                id = "c1",
                customerName = "John",
                customerPhone = "123",
                totalOutstanding = 300.0,
                productsSummary = "Unga",
                amountPaid = 100.0,
                balance = 200.0,
                dueDate = "Next Week",
                timestamp = now
            )
        )

        val summary = ReportCalculator.computeSummary(
            scope = ReportTimeScope.DAILY,
            periodLabel = "Daily",
            dateRangeText = "Today",
            startEpochMs = now - 1000,
            endEpochMs = now + 1000,
            allSales = sales,
            allExpenses = expenses,
            allCredits = credits,
            allProducts = listOf(product1, product2)
        )

        // Revenue = 300 + 210 = 510
        assertEquals(510.0, summary.revenue, 0.01)
        // COGS = 200 + 150 = 350
        assertEquals(350.0, summary.cogs, 0.01)
        // Gross Profit = 510 - 350 = 160
        assertEquals(160.0, summary.grossProfit, 0.01)
        // Total Expenses = 60
        assertEquals(60.0, summary.totalExpenses, 0.01)
        // Net Profit = 160 - 60 = 100
        assertEquals(100.0, summary.netProfit, 0.01)

        // Transactions = 2
        assertEquals(2, summary.transactionCount)
        assertEquals(255.0, summary.averageBasketSize, 0.01)

        // Payment breakdown
        assertEquals(300.0, summary.paymentBreakdown["Cash"] ?: 0.0, 0.01)
        assertEquals(210.0, summary.paymentBreakdown["M-Pesa"] ?: 0.0, 0.01)

        // Top products
        assertEquals(2, summary.topSellingProducts.size)
        assertEquals("p1", summary.topSellingProducts[0].productId)
        assertEquals(300.0, summary.topSellingProducts[0].revenue, 0.01)
        assertEquals(2, summary.topSellingProducts[0].unitsSold)
    }
}
