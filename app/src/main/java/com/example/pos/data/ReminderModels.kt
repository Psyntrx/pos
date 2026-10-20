package com.example.pos.data

import kotlinx.serialization.Serializable

@Serializable
enum class ReminderDeliveryChannel(val storageValue: String) {
    WHATSAPP("whatsapp"),
    SMS("sms"),
    BOTH("both");

    companion object {
        fun fromStorage(value: String?): ReminderDeliveryChannel =
            entries.firstOrNull { it.storageValue.equals(value, ignoreCase = true) } ?: BOTH
    }
}

@Serializable
enum class ReminderDeliveryTimeMode(val storageValue: String) {
    DEFAULT("default"),
    SPECIFIC("specific"),
    RANDOMIZED("randomized");

    companion object {
        fun fromStorage(value: String?): ReminderDeliveryTimeMode =
            entries.firstOrNull { it.storageValue.equals(value, ignoreCase = true) } ?: DEFAULT
    }
}

@Serializable
enum class OverdueRepeatInterval(val storageValue: String, val days: Int) {
    NONE("none", 0),
    DAILY("daily", 1),
    EVERY_3_DAYS("every_3_days", 3),
    WEEKLY("weekly", 7),
    CUSTOM("custom", -1);

    companion object {
        fun fromStorage(value: String?): OverdueRepeatInterval =
            entries.firstOrNull { it.storageValue.equals(value, ignoreCase = true) } ?: NONE
    }
}

@Serializable
enum class OverdueStopCondition(val storageValue: String, val maxDays: Int?) {
    PAID_ONLY("paid_only", null),
    ONE_MONTH("one_month", 30),
    TWO_MONTHS("two_months", 60),
    THREE_MONTHS("three_months", 90),
    NEVER("never", null);

    companion object {
        fun fromStorage(value: String?): OverdueStopCondition =
            entries.firstOrNull { it.storageValue.equals(value, ignoreCase = true) } ?: PAID_ONLY
    }
}

@Serializable
enum class ReminderEventType(val storageValue: String, val label: String) {
    ADVANCE("advance", "Advance Payment Reminder"),
    ADVANCE_WEEK("advance_week", "One Week Before Due"),
    DUE_DATE("due_date", "Payment Due Today"),
    FIRST_OVERDUE("first_overdue", "First Overdue Reminder"),
    REPEATED_OVERDUE("repeated_overdue", "Repeated Overdue Alert");

    companion object {
        fun fromStorage(value: String?): ReminderEventType =
            entries.firstOrNull { it.storageValue.equals(value, ignoreCase = true) } ?: DUE_DATE
    }
}

@Serializable
enum class DeliveryStatus(val storageValue: String) {
    QUEUED("queued"),
    ATTEMPTED("attempted"),
    ACCEPTED("accepted"),
    DELIVERED("delivered"),
    FAILED("failed"),
    SKIPPED("skipped"),
    CANCELLED("cancelled");

    companion object {
        fun fromStorage(value: String?): DeliveryStatus =
            entries.firstOrNull { it.storageValue.equals(value, ignoreCase = true) } ?: QUEUED
    }
}

@Serializable
data class ReminderTemplates(
    val advanceTemplate: String = "Hello {customer_name}, friendly reminder from {shop_name} that your balance of KSh {balance} is due in {due_date}. Thank you!",
    val dueDateTemplate: String = "Hello {customer_name}, your credit balance of KSh {balance} from {shop_name} is due today ({due_date}). Kindly arrange payment at your earliest convenience.",
    val firstOverdueTemplate: String = "Hello {customer_name}, your payment of KSh {balance} to {shop_name} was due on {due_date} and is now {overdue_days} days overdue. Please clear the outstanding balance.",
    val repeatedOverdueTemplate: String = "Urgent reminder: Hello {customer_name}, your balance of KSh {balance} to {shop_name} remains overdue by {overdue_days} days (due {due_date}). Please settle immediately."
) {
    fun getTemplateFor(type: ReminderEventType): String = when (type) {
        ReminderEventType.ADVANCE,
        ReminderEventType.ADVANCE_WEEK -> advanceTemplate
        ReminderEventType.DUE_DATE -> dueDateTemplate
        ReminderEventType.FIRST_OVERDUE -> firstOverdueTemplate
        ReminderEventType.REPEATED_OVERDUE -> repeatedOverdueTemplate
    }

    fun render(
        type: ReminderEventType,
        customerName: String,
        balance: Double,
        dueDate: String,
        overdueDays: Long,
        shopName: String
    ): String {
        val template = getTemplateFor(type)
        val formattedBalance = String.format(java.util.Locale.US, "%,.2f", balance.coerceAtLeast(0.0))
        return template
            .replace("{customer_name}", customerName.trim())
            .replace("{balance}", formattedBalance)
            .replace("{due_date}", dueDate.trim())
            .replace("{overdue_days}", overdueDays.coerceAtLeast(0L).toString())
            .replace("{shop_name}", shopName.trim().ifBlank { "RetailSync POS" })
    }
}

@Serializable
data class ReminderConfig(
    val advanceDaysBeforeDue: Int = 3, // 3 (default) or 7
    val additionalAdvanceDaysBeforeDue: Int = 7,
    val firstOverdueDaysAfterDue: Int = 3, // 3 (default) or 7
    val repeatInterval: OverdueRepeatInterval = OverdueRepeatInterval.NONE, // none (default), daily, every_3_days, weekly
    val repeatIntervalDays: Int = 7,
    val repeatCount: Int = 3,
    val stopCondition: OverdueStopCondition = OverdueStopCondition.PAID_ONLY, // paid_only (default), 1, 2, 3 months, never
    val defaultChannel: ReminderDeliveryChannel = ReminderDeliveryChannel.BOTH,
    val workingHoursStart: Int = 8, // 8:00 AM default
    val workingHoursEnd: Int = 17, // 5:00 PM default
    val timeZoneId: String = "Africa/Nairobi",
    val deliveryTimeMode: ReminderDeliveryTimeMode = ReminderDeliveryTimeMode.DEFAULT,
    val specificDeliveryHour: Int = 10,
    val specificDeliveryMinute: Int = 0,
    val allowOffHoursSending: Boolean = false,
    val autoSendEnabled: Boolean = true,
    val templates: ReminderTemplates = ReminderTemplates()
)

@Serializable
data class ScheduledReminder(
    val id: String,
    val creditId: String,
    val customerName: String,
    val customerPhone: String,
    val reminderType: ReminderEventType,
    val scheduledTimeMillis: Long,
    val remainingBalanceAtScheduling: Double,
    val shopId: String? = null,
    val status: DeliveryStatus = DeliveryStatus.QUEUED,
    val failureReason: String? = null
)

@Serializable
data class ReminderExecutionRecord(
    val id: String,
    val creditId: String,
    val customerName: String,
    val customerPhone: String,
    val reminderType: ReminderEventType,
    val scheduledTimeMillis: Long,
    val executionTimeMillis: Long,
    val balanceAtExecution: Double,
    val renderedMessage: String,
    val whatsAppStatus: DeliveryStatus,
    val whatsAppError: String? = null,
    val smsStatus: DeliveryStatus,
    val smsError: String? = null,
    val shopId: String? = null
)

@Serializable
data class SyncOperation(
    val id: String,
    val entityType: String, // "sale", "credit", "repayment", "product", "expense"
    val entityId: String,
    val action: String, // "UPSERT", "DELETE"
    val payloadJson: String,
    val timestamp: Long = System.currentTimeMillis(),
    val attempts: Int = 0,
    val lastError: String? = null,
    val shopId: String? = null
)
