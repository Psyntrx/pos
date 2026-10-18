package com.example.pos.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.math.BigDecimal
import java.math.RoundingMode

data class DisplayPaymentRecord(
    val id: String,
    val amount: Double,
    val method: String,
    val timestamp: Long,
    val formattedDate: String,
    val balanceAfterPayment: Double,
    val referenceCode: String = "",
    val notes: String = "",
    val isInitialPayment: Boolean = false,
    val isLegacySnapshot: Boolean = false
)

object CreditLedgerUtils {
    private val dueDateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)
    private val expenseDateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    private val paymentTimestampFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.US)

    fun defaultCreditDurationDays(): Int = 14

    fun calculateDueDate(days: Int = defaultCreditDurationDays(), fromDate: LocalDate = LocalDate.now()): String {
        return formatDueDate(fromDate.plusDays(days.coerceAtLeast(1).toLong()))
    }

    fun formatDueDate(date: LocalDate): String = date.format(dueDateFormatter)

    fun parseDueDate(value: String): LocalDate? = try {
        LocalDate.parse(value.trim(), dueDateFormatter)
    } catch (_: Exception) {
        null
    }

    fun normalizeMoney(amount: Double): Double? {
        if (!amount.isFinite() || amount < 0.0) return null
        return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP).toDouble()
    }

    fun calculateOutstandingBalance(originalAmount: Double, amountPaid: Double): Double? {
        val normalizedTotal = normalizeMoney(originalAmount) ?: return null
        val normalizedPaid = normalizeMoney(amountPaid) ?: return null
        if (normalizedPaid > normalizedTotal) return null
        return normalizeMoney(normalizedTotal - normalizedPaid)
    }

    fun calculateSaleTotal(items: List<CartItem>): Double? {
        if (items.isEmpty()) return null
        var total = BigDecimal.ZERO
        items.forEach { item ->
            val unitPrice = normalizeMoney(item.unitPrice) ?: return null
            if (unitPrice <= 0.0 || item.quantity <= 0) return null
            total = total.add(BigDecimal.valueOf(unitPrice).multiply(BigDecimal.valueOf(item.quantity.toLong())))
        }
        return normalizeMoney(total.toDouble())
    }

    fun normalizePhoneNumber(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return null

        // Strip spaces, dashes, parentheses, dots
        val cleaned = trimmed.replace(Regex("[\\s\\-\\(\\)\\.]"), "")
        if (cleaned.isBlank()) return null

        val hasPlus = cleaned.startsWith("+")
        val digitsOnly = if (hasPlus) cleaned.substring(1) else cleaned
        if (!digitsOnly.all { it.isDigit() }) return null

        // If prefixed with international dialing prefix "00"
        val effectiveDigits = if (!hasPlus && digitsOnly.startsWith("00")) {
            digitsOnly.substring(2)
        } else {
            digitsOnly
        }
        val isExplicitInternational = hasPlus || (!hasPlus && digitsOnly.startsWith("00"))

        // Check Kenyan formats
        if (effectiveDigits.startsWith("254")) {
            val national = effectiveDigits.substring(3).trimStart('0')
            if (national.length == 9 && (national.startsWith("7") || national.startsWith("1"))) {
                return "+254$national"
            }
        } else if (!isExplicitInternational && (effectiveDigits.startsWith("07") || effectiveDigits.startsWith("01")) && effectiveDigits.length == 10) {
            return "+254${effectiveDigits.substring(1)}"
        } else if (!isExplicitInternational && (effectiveDigits.startsWith("7") || effectiveDigits.startsWith("1")) && effectiveDigits.length == 9) {
            return "+254$effectiveDigits"
        }

        // Other international numbers (E.164: 7 to 15 digits)
        if (isExplicitInternational && effectiveDigits.length in 7..15) {
            return "+$effectiveDigits"
        }

        // Reasonable international formats without explicit + (10 to 15 digits)
        if (!isExplicitInternational && effectiveDigits.length in 10..15) {
            return "+$effectiveDigits"
        }

        return null
    }

    fun isValidPhoneNumber(input: String): Boolean = normalizePhoneNumber(input) != null

    fun buildReminderMessage(
        customerName: String,
        balance: Double,
        dueDate: String,
        shopName: String = "",
        currentDate: LocalDate = LocalDate.now()
    ): String {
        val storeContext = if (shopName.isNotBlank()) " from ${shopName.trim()}" else ""
        val due = parseDueDate(dueDate)
        val dueText = when {
            due != null && currentDate.isAfter(due) -> "which was due on $dueDate"
            dueDate.isNotBlank() -> "due on $dueDate"
            else -> "due soon"
        }
        val formattedBalance = String.format(Locale.US, "%,.2f", balance.coerceAtLeast(0.0))
        return "Hello ${customerName.trim()}, this is a friendly reminder$storeContext that you have an outstanding credit balance of KSh $formattedBalance, $dueText. Kindly arrange payment at your convenience. Thank you."
    }

    fun canSendReminder(credit: CustomerCredit): Boolean {
        return credit.balance > 0.0 && !credit.status.equals("Settled", ignoreCase = true)
    }

    fun resolveCreditStatus(credit: CustomerCredit, currentDate: LocalDate = LocalDate.now()): String {
        if (credit.balance <= 0.0 || credit.status.equals("Settled", ignoreCase = true)) {
            return "Settled"
        }
        val due = parseDueDate(credit.dueDate)
        if (due != null && currentDate.isAfter(due)) {
            return "Overdue"
        }
        if (credit.status.equals("Overdue", ignoreCase = true)) {
            return "Overdue"
        }
        return if (credit.status.isNotBlank()) credit.status else "Pending"
    }

    fun formatPaymentTimestamp(timestampMillis: Long): String {
        if (timestampMillis <= 0L) return "Date/time not recorded"
        return try {
            Instant.ofEpochMilli(timestampMillis)
                .atZone(ZoneId.systemDefault())
                .format(paymentTimestampFormatter)
        } catch (_: Exception) {
            "Date/time not recorded"
        }
    }

    fun formatCreationTimestamp(timestampMillis: Long): String {
        if (timestampMillis <= 0L) return "Date/time not recorded (legacy record)"
        return try {
            Instant.ofEpochMilli(timestampMillis)
                .atZone(ZoneId.systemDefault())
                .format(paymentTimestampFormatter)
        } catch (_: Exception) {
            "Date/time not recorded (legacy record)"
        }
    }

    fun getInitialDeposit(credit: CustomerCredit): Double {
        if (credit.paymentHistory.isNotEmpty()) {
            val sorted = credit.paymentHistory.sortedBy { it.timestamp }
            val first = sorted.firstOrNull()
            if (first != null && (first.id == "pay_${credit.id}" || first.id.startsWith("pay_c_") || (credit.createdAt > 0L && Math.abs(first.timestamp - credit.createdAt) <= 60000L))) {
                return first.amount
            }
            return 0.0
        }
        return credit.amountPaid
    }

    fun getSubsequentRepayments(credit: CustomerCredit): Double {
        val initial = getInitialDeposit(credit)
        val subsequent = credit.amountPaid - initial
        return if (subsequent > 0.0) BigDecimal.valueOf(subsequent).setScale(2, RoundingMode.HALF_UP).toDouble() else 0.0
    }

    fun getDetailedPaymentHistory(credit: CustomerCredit): List<DisplayPaymentRecord> {
        if (credit.paymentHistory.isNotEmpty()) {
            val sorted = credit.paymentHistory.sortedBy { it.timestamp }
            return sorted.mapIndexed { index, p ->
                val isFirstDeposit = index == 0 && (p.id.startsWith("pay_c_") || p.id == "pay_${credit.id}" || (credit.createdAt > 0L && Math.abs(p.timestamp - credit.createdAt) <= 60000L))
                val dateLabel = if (p.timestamp > 0L) formatPaymentTimestamp(p.timestamp) else "Date/time not recorded"
                val methodLabel = p.method.ifBlank { "Not specified" }
                DisplayPaymentRecord(
                    id = p.id,
                    amount = p.amount,
                    method = methodLabel,
                    timestamp = p.timestamp,
                    formattedDate = dateLabel,
                    balanceAfterPayment = p.balanceAfterPayment,
                    referenceCode = p.referenceCode,
                    notes = p.notes,
                    isInitialPayment = isFirstDeposit,
                    isLegacySnapshot = false
                )
            }.reversed()
        }

        // Legacy record snapshot derivation
        if (credit.amountPaid > 0.0) {
            val legacyRecord = DisplayPaymentRecord(
                id = "legacy_init_${credit.id}",
                amount = credit.amountPaid,
                method = "Unrecorded (legacy record)",
                timestamp = credit.createdAt,
                formattedDate = if (credit.createdAt > 0L) formatPaymentTimestamp(credit.createdAt) else "Date/time not recorded",
                balanceAfterPayment = credit.balance,
                referenceCode = "",
                notes = "Legacy opening payment snapshot",
                isInitialPayment = true,
                isLegacySnapshot = true
            )
            return listOf(legacyRecord)
        }

        return emptyList()
    }

    fun createCredit(
        id: String,
        customerName: String,
        customerPhone: String,
        originalAmount: Double,
        amountPaid: Double,
        productsSummary: String,
        dueDate: String,
        paymentMethod: String = "Cash",
        createdAt: Long = System.currentTimeMillis()
    ): CustomerCredit? {
        val trimmedName = customerName.trim()
        if (trimmedName.isBlank()) return null
        val normalizedPhone = normalizePhoneNumber(customerPhone) ?: return null
        val total = normalizeMoney(originalAmount)?.takeIf { it > 0.0 } ?: return null
        val paid = normalizeMoney(amountPaid) ?: return null
        val balance = calculateOutstandingBalance(total, paid) ?: return null
        val normalizedDueDate = parseDueDate(dueDate)?.let(::formatDueDate) ?: return null
        val normalizedMethod = paymentMethod.ifBlank { "Cash" }.trim()
        val initialPayments = if (paid > 0.0) {
            listOf(
                CreditPayment(
                    id = "pay_$id",
                    amount = paid,
                    method = normalizedMethod,
                    timestamp = createdAt,
                    balanceAfterPayment = balance
                )
            )
        } else {
            emptyList()
        }

        return CustomerCredit(
            id = id,
            customerName = trimmedName,
            customerPhone = normalizedPhone,
            totalOutstanding = total,
            productsSummary = productsSummary,
            amountPaid = paid,
            balance = balance,
            dueDate = normalizedDueDate,
            status = if (balance == 0.0) "Settled" else "Pending",
            paymentHistory = initialPayments,
            createdAt = createdAt,
            settledAt = if (balance == 0.0) formatDueDate(LocalDate.now()) else null
        )
    }

    fun mergeRemoteCredits(
        remoteCredits: List<CustomerCredit>,
        localCredits: List<CustomerCredit>
    ): List<CustomerCredit> {
        if (remoteCredits.isEmpty()) return localCredits
        val localById = localCredits.associateBy { it.id }
        val mergedRemote = remoteCredits.map { remoteCredit ->
            val localCredit = localById[remoteCredit.id]
            if (localCredit == null) {
                remoteCredit
            } else {
                remoteCredit.copy(
                    paymentHistory = (localCredit.paymentHistory + remoteCredit.paymentHistory)
                        .distinctBy { it.id }
                        .sortedBy { it.timestamp },
                    createdAt = if (localCredit.createdAt > 0L) localCredit.createdAt else remoteCredit.createdAt,
                    settledAt = localCredit.settledAt ?: remoteCredit.settledAt
                )
            }
        }
        val remoteIds = remoteCredits.map { it.id }.toSet()
        val localOnly = localCredits.filter { it.id !in remoteIds }
        return mergedRemote + localOnly
    }

    fun buildDemoExpenses(): List<Expense> {
        val today = LocalDate.now()
        val currentMonth = listOf(
            Expense("demo_exp_1", "Stock transport", "Transport", 2400.0, today.withDayOfMonth(3).format(expenseDateFormatter), "truck"),
            Expense("demo_exp_2", "Packaging materials", "Supplies", 1325.0, today.withDayOfMonth(6).format(expenseDateFormatter), "package"),
            Expense("demo_exp_3", "Electricity", "Utilities", 8600.0, today.withDayOfMonth(12).format(expenseDateFormatter), "flash"),
            Expense("demo_exp_4", "Shop rent", "Rent", 25000.0, today.withDayOfMonth(18).format(expenseDateFormatter), "home"),
            Expense("demo_exp_5", "Cleaning supplies", "Supplies", 980.0, today.withDayOfMonth(22).format(expenseDateFormatter), "sparkles"),
            Expense("demo_exp_6", "Internet", "Utilities", 4200.0, today.minusDays(3).format(expenseDateFormatter), "wifi"),
            Expense("demo_exp_7", "Equipment maintenance", "Maintenance", 3400.0, today.minusDays(6).format(expenseDateFormatter), "build"),
            Expense("demo_exp_8", "Delivery costs", "Transport", 1600.0, today.minusDays(2).format(expenseDateFormatter), "delivery"),
        )
        val previousMonth = listOf(
            Expense("demo_exp_9", "Airtime", "Communication", 750.0, today.minusMonths(1).withDayOfMonth(2).format(expenseDateFormatter), "phone"),
            Expense("demo_exp_10", "Packaging materials", "Supplies", 1185.0, today.minusMonths(1).withDayOfMonth(7).format(expenseDateFormatter), "package"),
            Expense("demo_exp_11", "Cleaner wages", "Payroll", 5200.0, today.minusMonths(1).withDayOfMonth(12).format(expenseDateFormatter), "people"),
            Expense("demo_exp_12", "Shop repairs", "Maintenance", 2900.0, today.minusMonths(1).withDayOfMonth(19).format(expenseDateFormatter), "build"),
            Expense("demo_exp_13", "Office supplies", "Supplies", 610.0, today.minusMonths(1).withDayOfMonth(26).format(expenseDateFormatter), "document"),
            Expense("demo_exp_14", "Water bill", "Utilities", 2050.0, today.minusMonths(1).withDayOfMonth(28).format(expenseDateFormatter), "water"),
        )
        return (currentMonth + previousMonth).sortedByDescending { parseExpenseDate(it.dateFormatted) ?: LocalDate.MIN }
    }

    fun filterActiveCredits(credits: List<CustomerCredit>, query: String = ""): List<CustomerCredit> {
        val normalizedQuery = query.trim()
        return credits.filter { it.balance > 0.0 && !it.status.equals("Settled", ignoreCase = true) }
            .filter { credit ->
                if (normalizedQuery.isBlank()) return@filter true
                val haystack = "${credit.customerName} ${credit.customerPhone} ${credit.productsSummary}".trim()
                haystack.contains(normalizedQuery, ignoreCase = true)
            }
            .sortedByDescending { it.createdAt }
    }

    fun filterPastCredits(credits: List<CustomerCredit>, query: String = ""): List<CustomerCredit> {
        val normalizedQuery = query.trim()
        return credits.filter { it.balance <= 0.0 || it.status.equals("Settled", ignoreCase = true) }
            .filter { credit ->
                if (normalizedQuery.isBlank()) return@filter true
                val haystack = "${credit.customerName} ${credit.customerPhone} ${credit.productsSummary}".trim()
                haystack.contains(normalizedQuery, ignoreCase = true)
            }
            .sortedByDescending { it.createdAt }
    }

    fun filterExpenses(expenses: List<Expense>, period: String): List<Expense> {
        val today = LocalDate.now()
        return when (period.lowercase()) {
            "this week" -> expenses.filter { expense ->
                val parsedDate = parseExpenseDate(expense.dateFormatted)
                parsedDate != null && !parsedDate.isBefore(today.minusDays(7))
            }
            "this month" -> expenses.filter { expense ->
                val parsedDate = parseExpenseDate(expense.dateFormatted)
                parsedDate != null && parsedDate.year == today.year && parsedDate.month == today.month
            }
            else -> expenses
        }
    }

    private fun parseExpenseDate(dateText: String): LocalDate? {
        val candidates = listOf(
            "MMM d, yyyy",
            "MMM d yyyy",
            "yyyy-MM-dd",
            "d MMM yyyy",
            "MMM d, yyyy h:mm a",
            "MMM d yyyy h:mm a"
        )
        return candidates.firstNotNullOfOrNull { pattern ->
            try {
                LocalDate.parse(dateText, DateTimeFormatter.ofPattern(pattern))
            } catch (_: Exception) {
                null
            }
        } ?: try {
            LocalDate.parse(dateText)
        } catch (_: Exception) {
            null
        }
    }

    fun isDuplicatePayment(credit: CustomerCredit, paymentAmount: Double, method: String, referenceCode: String = "", notes: String = ""): Boolean {
        val normalizedAmount = normalizeMoney(paymentAmount) ?: return false
        val normalizedMethod = method.ifBlank { "Cash" }
        val normalizedReference = referenceCode.trim()
        val normalizedNotes = notes.trim()
        return credit.paymentHistory.any { payment ->
            normalizeMoney(payment.amount) == normalizedAmount &&
                payment.method.equals(normalizedMethod, ignoreCase = true) &&
                (normalizedReference.isBlank() || payment.referenceCode.equals(normalizedReference, ignoreCase = true)) &&
                (normalizedNotes.isBlank() || payment.notes.equals(normalizedNotes, ignoreCase = true))
        }
    }

    fun applyRepayment(
        credit: CustomerCredit,
        paymentAmount: Double,
        method: String,
        referenceCode: String = "",
        notes: String = "",
        timestamp: Long = System.currentTimeMillis()
    ): CustomerCredit {
        val validAmount = normalizeMoney(paymentAmount)
            ?: throw IllegalArgumentException("Payment must be a valid non-negative amount.")
        require(validAmount > 0.0) { "Payment must be greater than zero." }
        val originalAmount = normalizeMoney(credit.totalOutstanding)
            ?: throw IllegalArgumentException("Credit total is invalid.")
        val paidBefore = normalizeMoney(credit.amountPaid)
            ?: throw IllegalArgumentException("Recorded payments are invalid.")
        val currentBalance = calculateOutstandingBalance(originalAmount, paidBefore)
            ?: throw IllegalArgumentException("Credit balance is invalid.")
        require(validAmount <= currentBalance) { "Payment exceeds the outstanding balance." }

        val normalizedMethod = method.ifBlank { "Cash" }
        val duplicate = isDuplicatePayment(credit, validAmount, normalizedMethod, referenceCode, notes)
        require(!duplicate) { "This payment has already been recorded." }

        val updatedPaid = normalizeMoney(paidBefore + validAmount)
            ?: throw IllegalArgumentException("Recorded payments are invalid.")
        val updatedBalance = calculateOutstandingBalance(originalAmount, updatedPaid)
            ?: throw IllegalArgumentException("Payment exceeds the outstanding balance.")
        val payment = CreditPayment(
            id = "pay_$timestamp",
            amount = validAmount,
            method = normalizedMethod,
            timestamp = timestamp,
            balanceAfterPayment = updatedBalance,
            referenceCode = referenceCode.trim(),
            notes = notes.trim()
        )

        val nextStatus = if (updatedBalance <= 0.0) {
            "Settled"
        } else {
            val due = parseDueDate(credit.dueDate)
            if (due != null && LocalDate.now().isAfter(due)) {
                "Overdue"
            } else if (credit.status.equals("Overdue", ignoreCase = true)) {
                "Overdue"
            } else {
                "Pending"
            }
        }

        val baseHistory = if (credit.paymentHistory.isEmpty() && paidBefore > 0.0) {
            listOf(
                CreditPayment(
                    id = "legacy_init_${credit.id}",
                    amount = paidBefore,
                    method = "Unrecorded (legacy record)",
                    timestamp = credit.createdAt.takeIf { it > 0L } ?: 0L,
                    balanceAfterPayment = currentBalance,
                    notes = "Legacy opening payment snapshot"
                )
            )
        } else {
            credit.paymentHistory
        }

        return credit.copy(
            amountPaid = updatedPaid,
            balance = updatedBalance,
            status = nextStatus,
            paymentHistory = baseHistory + payment,
            settledAt = if (updatedBalance <= 0.0) (credit.settledAt ?: LocalDate.now().format(dueDateFormatter)) else credit.settledAt
        )
    }
}
