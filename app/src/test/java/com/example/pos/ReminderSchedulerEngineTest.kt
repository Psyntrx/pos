package com.example.pos

import com.example.pos.data.CreditLedgerUtils
import com.example.pos.data.CustomerCredit
import com.example.pos.data.OverdueRepeatInterval
import com.example.pos.data.OverdueStopCondition
import com.example.pos.data.ReminderConfig
import com.example.pos.data.ReminderDeliveryChannel
import com.example.pos.data.ReminderDeliveryTimeMode
import com.example.pos.data.ReminderEventType
import com.example.pos.data.ReminderSchedulerEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ReminderSchedulerEngineTest {

    @Test
    fun defaultScheduleIncludesWeekAndThreeDayAdvanceNotices() {
        val dueDate = LocalDate.of(2026, 11, 10)
        val config = ReminderConfig(
            advanceDaysBeforeDue = 3,
            firstOverdueDaysAfterDue = 3,
            repeatInterval = OverdueRepeatInterval.NONE
        )

        val schedule = ReminderSchedulerEngine.generateSchedule(dueDate, config)

        assertEquals(4, schedule.size)
        assertEquals(ReminderEventType.ADVANCE_WEEK, schedule[0].type)
        assertEquals(LocalDate.of(2026, 11, 3), schedule[0].scheduledDateTime.toLocalDate())
        assertEquals(ReminderEventType.ADVANCE, schedule[1].type)
        assertEquals(LocalDate.of(2026, 11, 7), schedule[1].scheduledDateTime.toLocalDate())

        // Due date reminder: Mandatory on Nov 10
        assertEquals(ReminderEventType.DUE_DATE, schedule[2].type)
        assertEquals(LocalDate.of(2026, 11, 10), schedule[2].scheduledDateTime.toLocalDate())

        // First overdue reminder: 3 days after -> Nov 13
        assertEquals(ReminderEventType.FIRST_OVERDUE, schedule[3].type)
        assertEquals(LocalDate.of(2026, 11, 13), schedule[3].scheduledDateTime.toLocalDate())
    }

    @Test
    fun oneWeekAlternativeSchedulesCorrectlyAcrossMonthBoundaries() {
        // Due Date: Nov 3 -> Advance 7 days before is Oct 27
        val dueDate = LocalDate.of(2026, 11, 3)
        val config = ReminderConfig(
            advanceDaysBeforeDue = 7,
            firstOverdueDaysAfterDue = 7,
            repeatInterval = OverdueRepeatInterval.NONE
        )

        val schedule = ReminderSchedulerEngine.generateSchedule(dueDate, config)

        assertEquals(3, schedule.size)
        assertEquals(LocalDate.of(2026, 10, 27), schedule[0].scheduledDateTime.toLocalDate())
        assertEquals(LocalDate.of(2026, 11, 3), schedule[1].scheduledDateTime.toLocalDate())
        assertEquals(LocalDate.of(2026, 11, 10), schedule[2].scheduledDateTime.toLocalDate())
    }

    @Test
    fun leapYearCalculationPreservesCorrectDueAndAdvanceDates() {
        // Feb 29 on leap year 2028
        val dueDate = LocalDate.of(2028, 2, 29)
        val config = ReminderConfig(
            advanceDaysBeforeDue = 3,
            firstOverdueDaysAfterDue = 3,
            repeatInterval = OverdueRepeatInterval.NONE
        )

        val schedule = ReminderSchedulerEngine.generateSchedule(dueDate, config)

        assertEquals(4, schedule.size)
        assertEquals(ReminderEventType.ADVANCE_WEEK, schedule[0].type)
        assertEquals(LocalDate.of(2028, 2, 22), schedule[0].scheduledDateTime.toLocalDate())
        assertEquals(LocalDate.of(2028, 2, 26), schedule[1].scheduledDateTime.toLocalDate())
        assertEquals(LocalDate.of(2028, 2, 29), schedule[2].scheduledDateTime.toLocalDate())
        assertEquals(LocalDate.of(2028, 3, 3), schedule[3].scheduledDateTime.toLocalDate())
    }

    @Test
    fun workingHoursWindowEnforcesMorningStartAndDefersOffHours() {
        val config = ReminderConfig(
            workingHoursStart = 8,
            workingHoursEnd = 17,
            allowOffHoursSending = false
        )

        // Target early morning 05:00 -> deferred to 08:00 same day
        val earlyMorning = LocalDateTime.of(2026, 10, 20, 5, 0)
        val (earlyAdjusted, earlyDeferred) = ReminderSchedulerEngine.applyWorkingHoursPolicy(earlyMorning, config)
        assertTrue(earlyDeferred)
        assertEquals(LocalTime.of(8, 0), earlyAdjusted.toLocalTime())
        assertEquals(LocalDate.of(2026, 10, 20), earlyAdjusted.toLocalDate())

        // Target night 20:00 -> deferred to next day at 08:00
        val lateNight = LocalDateTime.of(2026, 10, 20, 20, 0)
        val (nightAdjusted, nightDeferred) = ReminderSchedulerEngine.applyWorkingHoursPolicy(lateNight, config)
        assertTrue(nightDeferred)
        assertEquals(LocalTime.of(8, 0), nightAdjusted.toLocalTime())
        assertEquals(LocalDate.of(2026, 10, 21), nightAdjusted.toLocalDate())

        // Target inside business hours 11:30 -> preserved without deferral
        val businessHours = LocalDateTime.of(2026, 10, 20, 11, 30)
        val (preserved, notDeferred) = ReminderSchedulerEngine.applyWorkingHoursPolicy(businessHours, config)
        assertFalse(notDeferred)
        assertEquals(businessHours, preserved)
    }

    @Test
    fun recurringOverdueFollowsIntervalsAndHonorsStopConditions() {
        val dueDate = LocalDate.of(2026, 10, 1)
        val config = ReminderConfig(
            advanceDaysBeforeDue = 3,
            firstOverdueDaysAfterDue = 3, // Oct 4
            repeatInterval = OverdueRepeatInterval.WEEKLY, // Every 7 days
            stopCondition = OverdueStopCondition.ONE_MONTH // 30 days max from dueDate
        )

        val schedule = ReminderSchedulerEngine.generateSchedule(dueDate, config)

        // Advance: Sep 28
        // Due Date: Oct 1
        // First Overdue: Oct 4 (+3d)
        // Repeat 1: Oct 11 (+10d from due)
        // Repeat 2: Oct 18 (+17d from due)
        // Repeat 3: Oct 25 (+24d from due)
        // Next would be Nov 1 (+31d from due, exceeding 30-day stop condition!)
        val repeatEvents = schedule.filter { it.type == ReminderEventType.REPEATED_OVERDUE }
        assertEquals(3, repeatEvents.size)
        assertEquals(LocalDate.of(2026, 10, 11), repeatEvents[0].scheduledDateTime.toLocalDate())
        assertEquals(LocalDate.of(2026, 10, 18), repeatEvents[1].scheduledDateTime.toLocalDate())
        assertEquals(LocalDate.of(2026, 10, 25), repeatEvents[2].scheduledDateTime.toLocalDate())
    }

    @Test
    fun customRepeatIntervalAndRepeatCountAreApplied() {
        val schedule = ReminderSchedulerEngine.generateSchedule(
            dueDate = LocalDate.of(2026, 10, 1),
            config = ReminderConfig(
                firstOverdueDaysAfterDue = 3,
                repeatInterval = OverdueRepeatInterval.CUSTOM,
                repeatIntervalDays = 14,
                repeatCount = 2,
                stopCondition = OverdueStopCondition.PAID_ONLY
            )
        )
        val repeats = schedule.filter { it.type == ReminderEventType.REPEATED_OVERDUE }
        assertEquals(2, repeats.size)
        assertEquals(LocalDate.of(2026, 10, 18), repeats[0].scheduledDateTime.toLocalDate())
        assertEquals(LocalDate.of(2026, 11, 1), repeats[1].scheduledDateTime.toLocalDate())
    }

    @Test
    fun shouldDeliverReminderRejectsFullyPaidOrSettledCredits() {
        val credit = CustomerCredit(
            id = "c1",
            customerName = "Wanjiku",
            customerPhone = "+254712345678",
            totalOutstanding = 2000.0,
            productsSummary = "Maize flour",
            amountPaid = 2000.0,
            balance = 0.0,
            dueDate = "Oct 25, 2026",
            status = "Settled"
        )

        assertFalse(
            ReminderSchedulerEngine.shouldDeliverReminder(
                credit = credit,
                event = ReminderEventType.DUE_DATE,
                currentBalance = 0.0,
                currentDate = LocalDate.of(2026, 10, 25),
                hasAlreadySentEvent = false
            )
        )
    }

    @Test
    fun overdueSeverityColorPaletteResolvesAccurately() {
        val today = LocalDate.of(2026, 10, 20)

        // Future due date
        assertEquals(
            ReminderSchedulerEngine.OverdueSeverity.UPCOMING,
            ReminderSchedulerEngine.resolveOverdueSeverity("Oct 25, 2026", 500.0, today)
        )

        // Due today
        assertEquals(
            ReminderSchedulerEngine.OverdueSeverity.DUE_TODAY,
            ReminderSchedulerEngine.resolveOverdueSeverity("Oct 20, 2026", 500.0, today)
        )

        // 3 days overdue -> newly overdue (1..7 days)
        assertEquals(
            ReminderSchedulerEngine.OverdueSeverity.NEWLY_OVERDUE,
            ReminderSchedulerEngine.resolveOverdueSeverity("Oct 17, 2026", 500.0, today)
        )

        // 14 days overdue -> seriously overdue (>7 days)
        assertEquals(
            ReminderSchedulerEngine.OverdueSeverity.SERIOUSLY_OVERDUE,
            ReminderSchedulerEngine.resolveOverdueSeverity("Oct 6, 2026", 500.0, today)
        )
    }
}
