package com.example.pos

import com.example.pos.data.CreditLedgerUtils
import com.example.pos.data.CartItem
import com.example.pos.data.CustomerCredit
import com.example.pos.data.Expense
import com.example.pos.data.Product
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CreditLedgerUtilsTest {

    @Test
    fun dueDateChoicesUseDaysAndDefaultToFourteenDays() {
        val today = LocalDate.of(2026, 10, 15)
        assertEquals(14, CreditLedgerUtils.defaultCreditDurationDays())
        assertEquals("Oct 22, 2026", CreditLedgerUtils.calculateDueDate(7, today))
        assertEquals("Oct 29, 2026", CreditLedgerUtils.calculateDueDate(14, today))
        assertEquals("Nov 14, 2026", CreditLedgerUtils.calculateDueDate(30, today))
        assertEquals("Dec 14, 2026", CreditLedgerUtils.calculateDueDate(60, today))
        assertEquals("Nov 5, 2026", CreditLedgerUtils.calculateDueDate(21, today))
    }

    @Test
    fun customDueDateParsesAndFormatsWithoutTimezoneShift() {
        val date = LocalDate.of(2026, 10, 29)
        val savedDate = CreditLedgerUtils.formatDueDate(date)

        assertEquals(date, CreditLedgerUtils.parseDueDate(savedDate))
        assertEquals(date, CreditLedgerUtils.parseDueDate("2026-10-29"))
        assertEquals(null, CreditLedgerUtils.parseDueDate("invalid-date-string"))
    }

    @Test
    fun createCreditWithoutPaymentKeepsFullSaleOutstandingAndNoPaymentHistory() {
        val credit = createCredit(originalAmount = 1250.0, amountPaid = 0.0)

        assertEquals(1250.0, credit.totalOutstanding, 0.0)
        assertEquals(0.0, credit.amountPaid, 0.0)
        assertEquals(1250.0, credit.balance, 0.0)
        assertEquals("Pending", credit.status)
        assertTrue(credit.paymentHistory.isEmpty())
    }

    @Test
    fun createCreditWithPartialPaymentRecordsOpeningPaymentOnce() {
        val credit = createCredit(originalAmount = 1250.0, amountPaid = 250.0, method = "M-Pesa")

        assertEquals(1250.0, credit.totalOutstanding, 0.0)
        assertEquals(250.0, credit.amountPaid, 0.0)
        assertEquals(1000.0, credit.balance, 0.0)
        assertEquals("Pending", credit.status)
        assertEquals(1, credit.paymentHistory.size)
        assertEquals(250.0, credit.paymentHistory.single().amount, 0.0)
        assertEquals(1000.0, credit.paymentHistory.single().balanceAfterPayment, 0.0)
        assertEquals("M-Pesa", credit.paymentHistory.single().method)
    }

    @Test
    fun createFullyPaidCreditIsSettledWithZeroBalance() {
        val credit = createCredit(originalAmount = 1250.0, amountPaid = 1250.0)

        assertEquals(1250.0, credit.totalOutstanding, 0.0)
        assertEquals(1250.0, credit.amountPaid, 0.0)
        assertEquals(0.0, credit.balance, 0.0)
        assertEquals("Settled", credit.status)
        assertEquals(1, credit.paymentHistory.size)
    }

    @Test
    fun createCreditRejectsInvalidAmountsAndDueDates() {
        assertEquals(null, createCreditOrNull(originalAmount = -1.0, amountPaid = 0.0))
        assertEquals(null, createCreditOrNull(originalAmount = 100.0, amountPaid = 101.0))
        assertEquals(null, createCreditOrNull(originalAmount = Double.NaN, amountPaid = 0.0))
        assertEquals(null, CreditLedgerUtils.createCredit(
            id = "c-invalid-date",
            customerName = "Test",
            customerPhone = "",
            originalAmount = 100.0,
            amountPaid = 0.0,
            productsSummary = "Item",
            dueDate = "not a date"
        ))
    }

    @Test
    fun createCreditRoundsAmountsToCentsBeforeCalculatingBalance() {
        val credit = createCredit(originalAmount = 100.105, amountPaid = 20.104)

        assertEquals(100.11, credit.totalOutstanding, 0.0)
        assertEquals(20.10, credit.amountPaid, 0.0)
        assertEquals(80.01, credit.balance, 0.0)
    }

    @Test
    fun calculateSaleTotalUsesProductPricesAndQuantities() {
        val firstProduct = Product("p1", "Rice", "Food", "RICE", buyingPrice = 80.0, sellingPrice = 110.50, stockQuantity = 10)
        val secondProduct = Product("p2", "Soap", "Household", "SOAP", buyingPrice = 20.0, sellingPrice = 29.99, stockQuantity = 10)

        val total = CreditLedgerUtils.calculateSaleTotal(
            listOf(CartItem(firstProduct, 2), CartItem(secondProduct, 3))
        )

        assertEquals(310.97, total!!, 0.0)
        assertEquals(null, CreditLedgerUtils.calculateSaleTotal(listOf(CartItem(firstProduct, 0))))
    }

    @Test
    fun applyRepaymentTracksBalanceAndHistory() {
        val credit = CustomerCredit(
            id = "c-1",
            customerName = "Samuel Johnson",
            customerPhone = "+254700000001",
            totalOutstanding = 600.0,
            productsSummary = "Rice and Cooking Oil",
            amountPaid = 100.0,
            balance = 500.0,
            dueDate = "Dec 30, 2026",
            status = "Pending",
            createdAt = System.currentTimeMillis()
        )

        val updated = CreditLedgerUtils.applyRepayment(credit, 200.0, "M-Pesa")

        assertEquals(300.0, updated.balance, 0.0)
        assertEquals(300.0, updated.amountPaid, 0.0)
        assertEquals("M-Pesa", updated.paymentHistory.last().method)
        assertEquals(200.0, updated.paymentHistory.last().amount, 0.0)
    }

    @Test
    fun applyFinalRepaymentSettlesCreditAtZeroBalance() {
        val credit = createCredit(originalAmount = 500.0, amountPaid = 125.0)

        val settled = CreditLedgerUtils.applyRepayment(credit, 375.0, "Cash")

        assertEquals(500.0, settled.totalOutstanding, 0.0)
        assertEquals(500.0, settled.amountPaid, 0.0)
        assertEquals(0.0, settled.balance, 0.0)
        assertEquals("Settled", settled.status)
        assertEquals(2, settled.paymentHistory.size)
    }

    @Test
    fun applyRepaymentRejectsOverpayment() {
        val credit = CustomerCredit(
            id = "c-2",
            customerName = "Samuel Johnson",
            customerPhone = "+254700000002",
            totalOutstanding = 600.0,
            productsSummary = "Rice",
            amountPaid = 0.0,
            balance = 600.0,
            dueDate = "Dec 31, 2026",
            status = "Pending",
            createdAt = System.currentTimeMillis()
        )

        val thrown = try {
            CreditLedgerUtils.applyRepayment(credit, 700.0, "Cash")
            null
        } catch (e: IllegalArgumentException) {
            e
        }

        assertTrue(thrown != null)
    }

    @Test
    fun filterActiveCreditsMatchesSearchAndIgnoresSettledRecords() {
        val credits = listOf(
            CustomerCredit("c-1", "Samuel Johnson", "+254700000001", 600.0, "Rice", 100.0, 500.0, "Dec 30, 2026", "Pending", createdAt = System.currentTimeMillis()),
            CustomerCredit("c-2", "Mary Wanjiku", "+254700000002", 300.0, "Flour", 300.0, 0.0, "Dec 31, 2026", "Settled", createdAt = System.currentTimeMillis()),
            CustomerCredit("c-3", "Samuel Johnson", "+254700000003", 200.0, "Cooking Oil", 0.0, 200.0, "Jan 05, 2027", "Pending", createdAt = System.currentTimeMillis())
        )

        val results = CreditLedgerUtils.filterActiveCredits(credits, "Samuel")
        assertEquals(2, results.size)
        assertTrue(results.all { it.balance > 0.0 })
    }

    @Test
    fun filterExpensesRespectsCurrentWeekAndMonth() {
        val today = LocalDate.now()
        val currentMonth = Expense("e1", "Rent", "Utility", 1000.0, today.format(DateTimeFormatter.ofPattern("MMM d, yyyy")), "rent")
        val pastMonth = Expense("e2", "Repairs", "Maintenance", 250.0, today.minusMonths(2).format(DateTimeFormatter.ofPattern("MMM d, yyyy")), "repairs")

        val monthResult = CreditLedgerUtils.filterExpenses(listOf(currentMonth, pastMonth), "This Month")
        val allResult = CreditLedgerUtils.filterExpenses(listOf(currentMonth, pastMonth), "All")

        assertEquals(1, monthResult.size)
        assertEquals(2, allResult.size)
    }

    @Test
    fun buildDemoExpensesIncludesCurrentAndPreviousMonthSamples() {
        val expenses = CreditLedgerUtils.buildDemoExpenses()
        val today = LocalDate.now()

        val currentMonth = expenses.count { expense ->
            val parsed = LocalDate.parse(expense.dateFormatted, DateTimeFormatter.ofPattern("MMM d, yyyy"))
            parsed.year == today.year && parsed.month == today.month
        }
        val previousMonth = expenses.count { expense ->
            val parsed = LocalDate.parse(expense.dateFormatted, DateTimeFormatter.ofPattern("MMM d, yyyy"))
            parsed.year == today.minusMonths(1).year && parsed.month == today.minusMonths(1).month
        }

        assertTrue(expenses.size >= 10)
        assertTrue(currentMonth >= 5)
        assertTrue(previousMonth >= 5)
        assertTrue(CreditLedgerUtils.filterExpenses(expenses, "This Week").isNotEmpty())
    }

    @Test
    fun applyRepaymentRetainsReferenceCodeAndNotes() {
        val credit = CustomerCredit(
            id = "c-3",
            customerName = "Miriam Akinyi",
            customerPhone = "+254700000003",
            totalOutstanding = 400.0,
            productsSummary = "Soap",
            amountPaid = 100.0,
            balance = 300.0,
            dueDate = "Jan 05, 2027",
            status = "Pending",
            createdAt = System.currentTimeMillis()
        )

        val updated = CreditLedgerUtils.applyRepayment(credit, 200.0, "M-Pesa", "MPESA-7788", "Customer sent payment")

        assertEquals("MPESA-7788", updated.paymentHistory.last().referenceCode)
        assertEquals("Customer sent payment", updated.paymentHistory.last().notes)
        assertEquals(200.0, updated.paymentHistory.last().amount, 0.0)
    }

    @Test
    fun applyRepaymentRejectsDuplicateApplication() {
        val credit = createCredit(originalAmount = 500.0, amountPaid = 0.0)
        val updated = CreditLedgerUtils.applyRepayment(credit, 100.0, "Cash", "REF-1")

        val thrown = try {
            CreditLedgerUtils.applyRepayment(updated, 100.0, "Cash", "REF-1")
            null
        } catch (e: IllegalArgumentException) {
            e
        }

        assertTrue(thrown != null)
        assertEquals(1, updated.paymentHistory.size)
        assertEquals(400.0, updated.balance, 0.0)
    }

    @Test
    fun mergeRemoteCreditsPreservesLocalPaymentHistoryWhenBackendOmitsIt() {
        val local = createCredit(originalAmount = 300.0, amountPaid = 100.0)
        val remote = local.copy(
            amountPaid = 100.0,
            balance = 200.0,
            paymentHistory = emptyList(),
            createdAt = 99L,
            settledAt = null
        )

        val merged = CreditLedgerUtils.mergeRemoteCredits(listOf(remote), listOf(local)).single()

        assertEquals(1, merged.paymentHistory.size)
        assertEquals(100.0, merged.paymentHistory.single().amount, 0.0)
        assertEquals(local.createdAt, merged.createdAt)
        assertEquals(local.settledAt, merged.settledAt)
    }

    @Test
    fun mergeRemoteCreditsKeepsLocalRecordsWhenRemoteListIsEmpty() {
        val local = createCredit(originalAmount = 300.0, amountPaid = 100.0)

        val merged = CreditLedgerUtils.mergeRemoteCredits(emptyList(), listOf(local))

        assertEquals(listOf(local), merged)
        assertEquals(1, merged.single().paymentHistory.size)
    }

    @Test
    fun normalizePhoneNumberValidatesKenyanAndInternationalFormats() {
        // Kenyan 07 and 01 formats
        assertEquals("+254712345678", CreditLedgerUtils.normalizePhoneNumber("0712345678"))
        assertEquals("+254112345678", CreditLedgerUtils.normalizePhoneNumber("0112345678"))
        assertEquals("+254712345678", CreditLedgerUtils.normalizePhoneNumber("712345678"))
        assertEquals("+254112345678", CreditLedgerUtils.normalizePhoneNumber("112345678"))

        // Formats with +254 and spaces / dashes
        assertEquals("+254712345678", CreditLedgerUtils.normalizePhoneNumber("+254 712 345 678"))
        assertEquals("+254712345678", CreditLedgerUtils.normalizePhoneNumber("254-712-345-678"))
        assertEquals("+254712345678", CreditLedgerUtils.normalizePhoneNumber("+2540712345678"))
        assertEquals("+254712345678", CreditLedgerUtils.normalizePhoneNumber("(0712) 345 678"))

        // International numbers
        assertEquals("+15551234567", CreditLedgerUtils.normalizePhoneNumber("+1 555 123 4567"))
        assertEquals("+447911123456", CreditLedgerUtils.normalizePhoneNumber("+44 7911 123456"))
        assertEquals("+256701234567", CreditLedgerUtils.normalizePhoneNumber("+256 701 234567"))
        assertEquals("+15551234567", CreditLedgerUtils.normalizePhoneNumber("001 555 123 4567"))

        // Invalid numbers
        assertEquals(null, CreditLedgerUtils.normalizePhoneNumber(""))
        assertEquals(null, CreditLedgerUtils.normalizePhoneNumber("   "))
        assertEquals(null, CreditLedgerUtils.normalizePhoneNumber("12345"))
        assertEquals(null, CreditLedgerUtils.normalizePhoneNumber("abcdef"))
        assertEquals(null, CreditLedgerUtils.normalizePhoneNumber("+254 123"))
    }

    @Test
    fun createCreditEnforcesMandatoryNameAndPhone() {
        // Rejects blank name
        assertEquals(null, CreditLedgerUtils.createCredit(
            id = "c-1",
            customerName = "   ",
            customerPhone = "0712345678",
            originalAmount = 100.0,
            amountPaid = 0.0,
            productsSummary = "Item",
            dueDate = "Oct 29, 2026"
        ))

        // Rejects invalid phone
        assertEquals(null, CreditLedgerUtils.createCredit(
            id = "c-2",
            customerName = "Alice",
            customerPhone = "invalid",
            originalAmount = 100.0,
            amountPaid = 0.0,
            productsSummary = "Item",
            dueDate = "Oct 29, 2026"
        ))

        // Accepts valid name and normalizes phone
        val credit = CreditLedgerUtils.createCredit(
            id = "c-3",
            customerName = "  Alice Wambui  ",
            customerPhone = "0712 345 678",
            originalAmount = 500.0,
            amountPaid = 100.0,
            productsSummary = "Flour",
            dueDate = "Oct 29, 2026"
        )
        assertTrue(credit != null)
        assertEquals("Alice Wambui", credit!!.customerName)
        assertEquals("+254712345678", credit.customerPhone)
    }

    @Test
    fun buildReminderMessageIncludesCustomerBalanceDueDateAndShopName() {
        val message = CreditLedgerUtils.buildReminderMessage(
            customerName = "John Doe",
            balance = 1250.0,
            dueDate = "Oct 30, 2026",
            shopName = "Faith's Store"
        )

        assertTrue(message.contains("John Doe"))
        assertTrue(message.contains("Faith's Store"))
        assertTrue(message.contains("1,250.00"))
        assertTrue(message.contains("Oct 30, 2026"))
    }

    @Test
    fun getDetailedPaymentHistoryDerivesLegacySnapshotWhenHistoryEmpty() {
        val legacyCredit = CustomerCredit(
            id = "c-legacy",
            customerName = "Old Customer",
            customerPhone = "+254712345678",
            totalOutstanding = 1000.0,
            productsSummary = "Sugar",
            amountPaid = 400.0,
            balance = 600.0,
            dueDate = "Oct 20, 2026",
            status = "Pending",
            paymentHistory = emptyList(),
            createdAt = 1000L
        )

        val history = CreditLedgerUtils.getDetailedPaymentHistory(legacyCredit)
        assertEquals(1, history.size)
        val snapshot = history.first()
        assertEquals(400.0, snapshot.amount, 0.0)
        assertEquals(600.0, snapshot.balanceAfterPayment, 0.0)
        assertTrue(snapshot.isInitialPayment)
        assertTrue(snapshot.isLegacySnapshot)
    }

    @Test
    fun getDetailedPaymentHistoryReturnsAllRepaymentsInReverseChronologicalOrder() {
        val credit = createCredit(originalAmount = 1000.0, amountPaid = 200.0)
        val withFirstRepayment = CreditLedgerUtils.applyRepayment(credit, 300.0, "M-Pesa", "MP-1", "First repayment")
        val withSecondRepayment = CreditLedgerUtils.applyRepayment(withFirstRepayment, 500.0, "Cash", "", "Final payment")

        val history = CreditLedgerUtils.getDetailedPaymentHistory(withSecondRepayment)
        assertEquals(3, history.size)
        // Most recent first:
        assertEquals(500.0, history[0].amount, 0.0)
        assertEquals(0.0, history[0].balanceAfterPayment, 0.0)
        assertEquals("Final payment", history[0].notes)

        assertEquals(300.0, history[1].amount, 0.0)
        assertEquals(500.0, history[1].balanceAfterPayment, 0.0)
        assertEquals("MP-1", history[1].referenceCode)

        assertEquals(200.0, history[2].amount, 0.0)
        assertEquals(800.0, history[2].balanceAfterPayment, 0.0)
        assertTrue(history[2].isInitialPayment)
    }

    @Test
    fun activeAndSettledCreditHistoryAccess() {
        val activeCredit = createCredit(originalAmount = 1000.0, amountPaid = 300.0)
        val activeHistory = CreditLedgerUtils.getDetailedPaymentHistory(activeCredit)
        assertEquals(1, activeHistory.size)
        assertEquals(300.0, activeHistory.first().amount, 0.0)
        assertEquals(700.0, activeCredit.balance, 0.0)
        assertEquals("Pending", CreditLedgerUtils.resolveCreditStatus(activeCredit))

        val settledCredit = CreditLedgerUtils.applyRepayment(activeCredit, 700.0, "Cash")
        val settledHistory = CreditLedgerUtils.getDetailedPaymentHistory(settledCredit)
        assertEquals(2, settledHistory.size)
        assertEquals(0.0, settledCredit.balance, 0.0)
        assertEquals("Settled", CreditLedgerUtils.resolveCreditStatus(settledCredit))
    }

    @Test
    fun initialDepositsAndSubsequentPaymentsWithoutDoubleCounting() {
        // Sale of 1000 with initial deposit of 250
        val credit = createCredit(originalAmount = 1000.0, amountPaid = 250.0)
        assertEquals(250.0, CreditLedgerUtils.getInitialDeposit(credit), 0.0)
        assertEquals(0.0, CreditLedgerUtils.getSubsequentRepayments(credit), 0.0)
        assertEquals(250.0, credit.amountPaid, 0.0)
        assertEquals(750.0, credit.balance, 0.0)

        // First repayment of 350
        val afterFirstRepayment = CreditLedgerUtils.applyRepayment(credit, 350.0, "M-Pesa")
        assertEquals(250.0, CreditLedgerUtils.getInitialDeposit(afterFirstRepayment), 0.0)
        assertEquals(350.0, CreditLedgerUtils.getSubsequentRepayments(afterFirstRepayment), 0.0)
        assertEquals(600.0, afterFirstRepayment.amountPaid, 0.0)
        assertEquals(400.0, afterFirstRepayment.balance, 0.0)

        // Final repayment of 400
        val fullySettled = CreditLedgerUtils.applyRepayment(afterFirstRepayment, 400.0, "Cash")
        assertEquals(250.0, CreditLedgerUtils.getInitialDeposit(fullySettled), 0.0)
        assertEquals(750.0, CreditLedgerUtils.getSubsequentRepayments(fullySettled), 0.0)
        assertEquals(1000.0, fullySettled.amountPaid, 0.0)
        assertEquals(0.0, fullySettled.balance, 0.0)
        assertEquals("Settled", fullySettled.status)
    }

    @Test
    fun creditWithZeroInitialDepositTracksSubsequentPaymentsAccurately() {
        val credit = createCredit(originalAmount = 800.0, amountPaid = 0.0)
        assertEquals(0.0, CreditLedgerUtils.getInitialDeposit(credit), 0.0)
        assertEquals(0.0, CreditLedgerUtils.getSubsequentRepayments(credit), 0.0)
        assertEquals(800.0, credit.balance, 0.0)

        val updated = CreditLedgerUtils.applyRepayment(credit, 300.0, "Cash")
        assertEquals(0.0, CreditLedgerUtils.getInitialDeposit(updated), 0.0)
        assertEquals(300.0, CreditLedgerUtils.getSubsequentRepayments(updated), 0.0)
        assertEquals(300.0, updated.amountPaid, 0.0)
        assertEquals(500.0, updated.balance, 0.0)
    }

    @Test
    fun accurateTimestampsForNewCreditAndRepaymentRecords() {
        val now = 1776333600000L // deterministic timestamp (e.g. 2026-04-16)
        val credit = CreditLedgerUtils.createCredit(
            id = "c-time-test",
            customerName = "Jane Doe",
            customerPhone = "+254712345678",
            originalAmount = 500.0,
            amountPaid = 100.0,
            productsSummary = "Product",
            dueDate = "Oct 29, 2026",
            paymentMethod = "Cash",
            createdAt = now
        )
        requireNotNull(credit)
        assertEquals(now, credit.createdAt)
        val formattedCreation = CreditLedgerUtils.formatCreationTimestamp(credit.createdAt)
        assertTrue(formattedCreation.contains("2026"))

        val repaymentTime = now + 3600000L
        val updated = CreditLedgerUtils.applyRepayment(credit, 200.0, "M-Pesa", timestamp = repaymentTime)
        assertEquals(repaymentTime, updated.paymentHistory.last().timestamp)
        val formattedRepayment = CreditLedgerUtils.formatPaymentTimestamp(updated.paymentHistory.last().timestamp)
        assertTrue(formattedRepayment.contains("2026"))
    }

    @Test
    fun legacyRecordsWithUnavailableTimestampsLabelHonesty() {
        val legacy = CustomerCredit(
            id = "legacy-1",
            customerName = "Old Customer",
            customerPhone = "+254700000001",
            totalOutstanding = 400.0,
            productsSummary = "Old item",
            amountPaid = 100.0,
            balance = 300.0,
            dueDate = "Oct 29, 2026",
            status = "Pending",
            paymentHistory = emptyList(),
            createdAt = 0L
        )

        assertEquals("Date/time not recorded (legacy record)", CreditLedgerUtils.formatCreationTimestamp(legacy.createdAt))
        assertEquals("Date/time not recorded", CreditLedgerUtils.formatPaymentTimestamp(0L))

        val history = CreditLedgerUtils.getDetailedPaymentHistory(legacy)
        assertEquals(1, history.size)
        val snapshot = history.first()
        assertEquals("Unrecorded (legacy record)", snapshot.method)
        assertEquals("Date/time not recorded", snapshot.formattedDate)
        assertTrue(snapshot.isLegacySnapshot)
    }

    @Test
    fun reminderAvailabilityBeforeAndAfterDueDate() {
        val activeNotDue = CustomerCredit(
            id = "c-1", customerName = "A", customerPhone = "+254700000001",
            totalOutstanding = 500.0, productsSummary = "item", amountPaid = 0.0,
            balance = 500.0, dueDate = "Oct 29, 2026", status = "Pending"
        )
        val activeOverdue = activeNotDue.copy(dueDate = "Oct 10, 2026", status = "Overdue")
        val settled = activeNotDue.copy(balance = 0.0, amountPaid = 500.0, status = "Settled")

        assertTrue(CreditLedgerUtils.canSendReminder(activeNotDue))
        assertTrue(CreditLedgerUtils.canSendReminder(activeOverdue))
        org.junit.Assert.assertFalse(CreditLedgerUtils.canSendReminder(settled))
    }

    @Test
    fun reminderTextContainsCorrectBalanceAndDueDateBeforeAndAfterDueDate() {
        val today = LocalDate.of(2026, 10, 16)

        // Before due date
        val beforeMsg = CreditLedgerUtils.buildReminderMessage(
            customerName = "Peter Kamau",
            balance = 1450.50,
            dueDate = "Oct 29, 2026",
            shopName = "Quick Mart",
            currentDate = today
        )
        assertTrue(beforeMsg.contains("Peter Kamau"))
        assertTrue(beforeMsg.contains("Quick Mart"))
        assertTrue(beforeMsg.contains("1,450.50"))
        assertTrue(beforeMsg.contains("due on Oct 29, 2026"))

        // After due date (overdue)
        val afterMsg = CreditLedgerUtils.buildReminderMessage(
            customerName = "Peter Kamau",
            balance = 1450.50,
            dueDate = "Oct 10, 2026",
            shopName = "Quick Mart",
            currentDate = today
        )
        assertTrue(afterMsg.contains("Peter Kamau"))
        assertTrue(afterMsg.contains("1,450.50"))
        assertTrue(afterMsg.contains("which was due on Oct 10, 2026"))
    }

    @Test
    fun statusResolutionRulesCoverSettledOverdueAndPending() {
        val today = LocalDate.of(2026, 10, 16)

        val settledByBalance = CustomerCredit(
            id = "c-1", customerName = "A", customerPhone = "+254700000001",
            totalOutstanding = 500.0, productsSummary = "item", amountPaid = 500.0, balance = 0.0,
            dueDate = "Oct 29, 2026", status = "Pending"
        )
        assertEquals("Settled", CreditLedgerUtils.resolveCreditStatus(settledByBalance, today))

        val settledByStatus = settledByBalance.copy(balance = 10.0, status = "Settled")
        assertEquals("Settled", CreditLedgerUtils.resolveCreditStatus(settledByStatus, today))

        val overdueCredit = CustomerCredit(
            id = "c-2", customerName = "B", customerPhone = "+254700000002",
            totalOutstanding = 500.0, productsSummary = "item", amountPaid = 100.0, balance = 400.0,
            dueDate = "Oct 10, 2026", status = "Pending"
        )
        assertEquals("Overdue", CreditLedgerUtils.resolveCreditStatus(overdueCredit, today))

        val pendingCredit = CustomerCredit(
            id = "c-3", customerName = "C", customerPhone = "+254700000003",
            totalOutstanding = 500.0, productsSummary = "item", amountPaid = 100.0, balance = 400.0,
            dueDate = "Oct 29, 2026", status = "Pending"
        )
        assertEquals("Pending", CreditLedgerUtils.resolveCreditStatus(pendingCredit, today))
    }

    private fun createCredit(originalAmount: Double, amountPaid: Double, method: String = "Cash"): CustomerCredit {
        return requireNotNull(
            createCreditOrNull(originalAmount, amountPaid, method)
        )
    }

    private fun createCreditOrNull(
        originalAmount: Double,
        amountPaid: Double,
        method: String = "Cash"
    ): CustomerCredit? = CreditLedgerUtils.createCredit(
        id = "c-test",
        customerName = "Test Customer",
        customerPhone = "+254700000000",
        originalAmount = originalAmount,
        amountPaid = amountPaid,
        productsSummary = "Test product x 1",
        dueDate = "Oct 29, 2026",
        paymentMethod = method,
        createdAt = 1L
    )
}
