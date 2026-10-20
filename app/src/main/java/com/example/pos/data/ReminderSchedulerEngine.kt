package com.example.pos.data

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.random.Random

data class CalculatedReminderEvent(
    val type: ReminderEventType,
    val scheduledDateTime: LocalDateTime,
    val isDeferred: Boolean = false
)

object ReminderSchedulerEngine {

    fun parseZoneId(zoneString: String): ZoneId = try {
        ZoneId.of(zoneString.trim())
    } catch (_: Exception) {
        ZoneId.of("Africa/Nairobi")
    }

    /**
     * Given an intended date and reminder configuration, calculate the exact execution LocalDateTime
     * respecting the configured working hours window [workingHoursStart..workingHoursEnd] and delivery time mode.
     */
    fun resolveExecutionDateTime(
        targetDate: LocalDate,
        config: ReminderConfig,
        randomSeed: Long = targetDate.toEpochDay()
    ): LocalDateTime {
        val startHour = config.workingHoursStart.coerceIn(0, 23)
        val endHour = config.workingHoursEnd.coerceIn(startHour + 1, 24)

        val targetTime = when (config.deliveryTimeMode) {
            ReminderDeliveryTimeMode.DEFAULT -> LocalTime.of(startHour.coerceAtLeast(8), 30)
            ReminderDeliveryTimeMode.SPECIFIC -> {
                val hour = config.specificDeliveryHour.coerceIn(startHour, (endHour - 1).coerceAtLeast(startHour))
                val min = config.specificDeliveryMinute.coerceIn(0, 59)
                LocalTime.of(hour, min)
            }
            ReminderDeliveryTimeMode.RANDOMIZED -> {
                val rng = Random(randomSeed)
                val totalMinutes = ((endHour - startHour) * 60).coerceAtLeast(30)
                val randomOffset = rng.nextInt(totalMinutes)
                LocalTime.of(startHour, 0).plusMinutes(randomOffset.toLong())
            }
        }
        return LocalDateTime.of(targetDate, targetTime)
    }

    /**
     * Defer a scheduled LocalDateTime to the next permitted working window if off-hours sending is disabled.
     */
    fun applyWorkingHoursPolicy(
        dateTime: LocalDateTime,
        config: ReminderConfig
    ): Pair<LocalDateTime, Boolean> {
        if (config.allowOffHoursSending) {
            return Pair(dateTime, false)
        }

        val startHour = config.workingHoursStart.coerceIn(0, 23)
        val endHour = config.workingHoursEnd.coerceIn(startHour + 1, 24)
        val hour = dateTime.hour

        return when {
            hour < startHour -> {
                // Too early in morning: adjust to opening hour of same day
                Pair(dateTime.withHour(startHour).withMinute(0).withSecond(0), true)
            }
            hour >= endHour -> {
                // Past evening closing: defer to next morning at opening hour
                val nextDay = dateTime.toLocalDate().plusDays(1)
                Pair(LocalDateTime.of(nextDay, LocalTime.of(startHour, 0)), true)
            }
            else -> Pair(dateTime, false)
        }
    }

    /**
     * Generates all scheduled reminder events for a given credit sale based on its due date and config.
     * Guaranteed:
     * 1. Due date reminder is ALWAYS scheduled (mandatory default).
     * 2. Advance reminder is scheduled (3 or 7 days prior).
     * 3. First overdue reminder is scheduled (3 or 7 days post-due).
     * 4. Recurring overdue reminders scheduled according to repetition interval and stop conditions.
     */
    fun generateSchedule(
        dueDate: LocalDate,
        config: ReminderConfig,
        saleDate: LocalDate = dueDate.minusDays(14)
    ): List<CalculatedReminderEvent> {
        val events = mutableListOf<CalculatedReminderEvent>()

        // Defaults produce one-week and three-day advance notices.
        listOf(
            ReminderEventType.ADVANCE_WEEK to config.additionalAdvanceDaysBeforeDue,
            ReminderEventType.ADVANCE to config.advanceDaysBeforeDue
        ).filter { it.second > 0 }
            .distinctBy { it.second }
            .forEach { (type, days) ->
                val advanceDate = dueDate.minusDays(days.toLong())
                if (!advanceDate.isBefore(saleDate)) {
                    val rawAdvanceTime = resolveExecutionDateTime(advanceDate, config, advanceDate.toEpochDay())
                    val (finalTime, deferred) = applyWorkingHoursPolicy(rawAdvanceTime, config)
                    events.add(CalculatedReminderEvent(type, finalTime, deferred))
                }
        }

        // 2. Due Date reminder (MANDATORY EVENT)
        val rawDueDateTime = resolveExecutionDateTime(dueDate, config, dueDate.toEpochDay())
        val (finalDueTime, dueDeferred) = applyWorkingHoursPolicy(rawDueDateTime, config)
        events.add(CalculatedReminderEvent(ReminderEventType.DUE_DATE, finalDueTime, dueDeferred))

        // 3. First Overdue reminder (3 or 7 days after due date)
        val firstOverdueDays = config.firstOverdueDaysAfterDue.coerceAtLeast(1)
        val firstOverdueDate = dueDate.plusDays(firstOverdueDays.toLong())
        val rawFirstOverdueTime = resolveExecutionDateTime(firstOverdueDate, config, firstOverdueDate.toEpochDay())
        val (finalFirstOverdueTime, firstDeferred) = applyWorkingHoursPolicy(rawFirstOverdueTime, config)
        events.add(CalculatedReminderEvent(ReminderEventType.FIRST_OVERDUE, finalFirstOverdueTime, firstDeferred))

        // 4. Repeated Overdue reminders (if enabled)
        if (config.repeatInterval != OverdueRepeatInterval.NONE) {
            val stepDays = (if (config.repeatInterval == OverdueRepeatInterval.CUSTOM) {
                config.repeatIntervalDays.coerceIn(1, 365)
            } else config.repeatInterval.days).toLong()
            val maxDaysAllowed = config.stopCondition.maxDays ?: Int.MAX_VALUE
            val maxRepeats = config.repeatCount.coerceIn(0, 365)
            var currentOverdueDate = firstOverdueDate.plusDays(stepDays)
            var repeatIndex = 0

            while (repeatIndex < maxRepeats && ChronoUnit.DAYS.between(dueDate, currentOverdueDate) <= maxDaysAllowed) {
                val rawRepeatTime = resolveExecutionDateTime(currentOverdueDate, config, currentOverdueDate.toEpochDay())
                val (finalRepeatTime, repeatDeferred) = applyWorkingHoursPolicy(rawRepeatTime, config)
                events.add(CalculatedReminderEvent(ReminderEventType.REPEATED_OVERDUE, finalRepeatTime, repeatDeferred))
                currentOverdueDate = currentOverdueDate.plusDays(stepDays)
                repeatIndex++
            }
        }

        return events.sortedBy { it.scheduledDateTime }
    }

    /**
     * Validates whether a credit sale is eligible for a scheduled reminder delivery right now.
     */
    fun shouldDeliverReminder(
        credit: CustomerCredit,
        event: ReminderEventType,
        currentBalance: Double,
        currentDate: LocalDate,
        hasAlreadySentEvent: Boolean
    ): Boolean {
        // Debt paid off or settled -> never deliver
        if (currentBalance <= 0.0 || credit.status.equals("Settled", ignoreCase = true)) {
            return false
        }
        // Duplicate delivery check -> don't send the same reminder event twice
        if (hasAlreadySentEvent) {
            return false
        }
        val due = CreditLedgerUtils.parseDueDate(credit.dueDate) ?: return false

        // Logic check per type:
        return when (event) {
            ReminderEventType.ADVANCE,
            ReminderEventType.ADVANCE_WEEK -> !currentDate.isAfter(due)
            ReminderEventType.DUE_DATE -> !currentDate.isBefore(due)
            ReminderEventType.FIRST_OVERDUE,
            ReminderEventType.REPEATED_OVERDUE -> currentDate.isAfter(due)
        }
    }

    /**
     * Calculate overdue days from due date using shop-local date policy.
     */
    fun calculateOverdueDays(dueDateStr: String, currentDate: LocalDate = LocalDate.now()): Long {
        val parsedDue = CreditLedgerUtils.parseDueDate(dueDateStr) ?: return 0L
        if (!currentDate.isAfter(parsedDue)) return 0L
        return ChronoUnit.DAYS.between(parsedDue, currentDate).coerceAtLeast(0L)
    }

    /**
     * Resolves overdue severity level and visual color representation.
     */
    enum class OverdueSeverity(val label: String, val badgeColorHex: Long) {
        UPCOMING("Upcoming", 0xFF64748B),      // Neutral / Informational
        DUE_TODAY("Due Today", 0xFFF59E0B),    // Noticeable warning (Amber/Yellow)
        NEWLY_OVERDUE("Overdue", 0xFFF97316),  // Amber / Orange (1-7 days)
        SERIOUSLY_OVERDUE("Seriously Overdue", 0xFFEF4444) // Red (>7 days)
    }

    fun resolveOverdueSeverity(dueDateStr: String, balance: Double, currentDate: LocalDate = LocalDate.now()): OverdueSeverity {
        if (balance <= 0.0) return OverdueSeverity.UPCOMING
        val parsedDue = CreditLedgerUtils.parseDueDate(dueDateStr) ?: return OverdueSeverity.UPCOMING
        val days = ChronoUnit.DAYS.between(parsedDue, currentDate)
        return when {
            days < 0 -> OverdueSeverity.UPCOMING
            days == 0L -> OverdueSeverity.DUE_TODAY
            days in 1..7 -> OverdueSeverity.NEWLY_OVERDUE
            else -> OverdueSeverity.SERIOUSLY_OVERDUE
        }
    }
}
