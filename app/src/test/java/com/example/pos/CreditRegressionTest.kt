package com.example.pos

import com.example.pos.data.CreditLedgerUtils
import com.example.pos.data.ReminderSchedulerEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CreditRegressionTest {

    @Test
    fun createCreditWithIsoDateSucceedsAndNormalizesDueDate() {
        val credit = CreditLedgerUtils.createCredit(
            id = "c_iso_test",
            customerName = "Jane Doe",
            customerPhone = "0712345678",
            originalAmount = 3500.0,
            amountPaid = 500.0,
            productsSummary = "2 Sacks Maize",
            dueDate = "2026-11-15"
        )

        assertNotNull("Credit with ISO date must not be null", credit)
        assertEquals(3500.0, credit!!.totalOutstanding, 0.0)
        assertEquals(500.0, credit.amountPaid, 0.0)
        assertEquals(3000.0, credit.balance, 0.0)
        assertEquals("Nov 15, 2026", credit.dueDate)
        assertEquals("+254712345678", credit.customerPhone)
    }

    @Test
    fun createCreditWithFormattedDateSucceeds() {
        val credit = CreditLedgerUtils.createCredit(
            id = "c_standard_test",
            customerName = "John Doe",
            customerPhone = "+254700000000",
            originalAmount = 1500.0,
            amountPaid = 0.0,
            productsSummary = "1 Box Cooking Oil",
            dueDate = "Nov 15, 2026"
        )

        assertNotNull(credit)
        assertEquals(1500.0, credit!!.totalOutstanding, 0.0)
        assertEquals(0.0, credit.amountPaid, 0.0)
        assertEquals(1500.0, credit.balance, 0.0)
        assertEquals("Nov 15, 2026", credit.dueDate)
    }

    @Test
    fun directCreditAmountCalculationAndRepaymentIntegrity() {
        val credit = CreditLedgerUtils.createCredit(
            id = "c_manual_sale",
            customerName = "David Store",
            customerPhone = "0112345678",
            originalAmount = 5000.0,
            amountPaid = 1000.0,
            productsSummary = "Direct credit sale",
            dueDate = "Dec 1, 2026"
        )

        assertNotNull(credit)
        assertEquals(4000.0, credit!!.balance, 0.0)

        // Apply a subsequent repayment
        val updated = CreditLedgerUtils.applyRepayment(
            credit = credit,
            paymentAmount = 2000.0,
            method = "M-Pesa",
            referenceCode = "QWE123RTY",
            notes = "Partial clearing"
        )

        assertEquals(2000.0, updated.balance, 0.0)
        assertEquals(3000.0, updated.amountPaid, 0.0)
        assertEquals("Pending", updated.status)
        assertEquals(2, updated.paymentHistory.size)

        // Clear remaining balance
        val settled = CreditLedgerUtils.applyRepayment(
            credit = updated,
            paymentAmount = 2000.0,
            method = "Cash"
        )

        assertEquals(0.0, settled.balance, 0.0)
        assertEquals(5000.0, settled.amountPaid, 0.0)
        assertEquals("Settled", settled.status)
    }

    @Test
    fun overdueSeverityResolutionAcrossDueDates() {
        val baseDate = LocalDate.of(2026, 10, 20)

        // Future due date -> UPCOMING
        val futureSev = ReminderSchedulerEngine.resolveOverdueSeverity(
            dueDateStr = "Oct 25, 2026",
            balance = 500.0,
            currentDate = baseDate
        )
        assertEquals(ReminderSchedulerEngine.OverdueSeverity.UPCOMING, futureSev)

        // Due today -> DUE_TODAY
        val todaySev = ReminderSchedulerEngine.resolveOverdueSeverity(
            dueDateStr = "Oct 20, 2026",
            balance = 500.0,
            currentDate = baseDate
        )
        assertEquals(ReminderSchedulerEngine.OverdueSeverity.DUE_TODAY, todaySev)

        // 3 days past due -> NEWLY_OVERDUE
        val overdueSev = ReminderSchedulerEngine.resolveOverdueSeverity(
            dueDateStr = "Oct 17, 2026",
            balance = 500.0,
            currentDate = baseDate
        )
        assertEquals(ReminderSchedulerEngine.OverdueSeverity.NEWLY_OVERDUE, overdueSev)

        // 15 days past due -> SERIOUSLY_OVERDUE
        val seriousSev = ReminderSchedulerEngine.resolveOverdueSeverity(
            dueDateStr = "Oct 5, 2026",
            balance = 500.0,
            currentDate = baseDate
        )
        assertEquals(ReminderSchedulerEngine.OverdueSeverity.SERIOUSLY_OVERDUE, seriousSev)

        // Fully settled account has no overdue severity
        val settledSev = ReminderSchedulerEngine.resolveOverdueSeverity(
            dueDateStr = "Oct 5, 2026",
            balance = 0.0,
            currentDate = baseDate
        )
        assertEquals(ReminderSchedulerEngine.OverdueSeverity.UPCOMING, settledSev)
    }

    @Test
    fun reminderMessageBuilderFormatsProperStoreContext() {
        val msg = CreditLedgerUtils.buildReminderMessage(
            customerName = "Alice",
            balance = 1200.0,
            dueDate = "Nov 1, 2026",
            shopName = "Quick Mart",
            currentDate = LocalDate.of(2026, 10, 25)
        )

        assertTrue(msg.contains("Alice"))
        assertTrue(msg.contains("Quick Mart"))
        assertTrue(msg.contains("1,200.00"))
        assertTrue(msg.contains("Nov 1, 2026"))
    }
}
