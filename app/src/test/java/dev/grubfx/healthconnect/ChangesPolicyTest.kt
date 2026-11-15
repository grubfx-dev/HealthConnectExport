package dev.grubfx.healthconnect

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The rules of the export with the changes, tested without a device.
 */
class ChangesPolicyTest {
    @Test
    fun `a day the server does not have yet is read`() {
        val days = listOf("2026-11-01", "2026-11-02", "2026-11-03")

        val result = ChangesPolicy.daysToExport(
            days = days,
            changedDays = emptySet(),
            pendingDays = emptySet(),
            deliveredDays = emptySet(),
            currentDay = "2026-11-03",
            previousDay = "2026-11-02",
        )

        assertEquals(days, result)
    }

    @Test
    fun `a day the server holds and that changed is read again`() {
        val days = listOf("2026-11-01", "2026-11-02")
        val delivered = setOf("2026-11-01")

        val result = ChangesPolicy.daysToExport(
            days = days,
            changedDays = setOf("2026-11-01"),
            pendingDays = emptySet(),
            deliveredDays = delivered,
            currentDay = "2026-11-02",
            previousDay = "2026-11-01",
        )

        assertEquals(listOf("2026-11-01", "2026-11-02"), result)
    }

    @Test
    fun `a day the server holds and that did not change is not read again`() {
        val days = listOf("2026-11-01", "2026-11-03")
        val delivered = setOf("2026-11-01")

        val result = ChangesPolicy.daysToExport(
            days = days,
            changedDays = emptySet(),
            pendingDays = emptySet(),
            deliveredDays = delivered,
            currentDay = "2026-11-03",
            previousDay = "2026-11-02",
        )

        // The current day and the day before it are always read, because
        // their totals grow during the day. The day of the first of
        // November is neither, so it is not read again.
        assertEquals(listOf("2026-11-03"), result)
    }

    @Test
    fun `the day before the current day is always read`() {
        val days = listOf("2026-11-01", "2026-11-02")
        val delivered = setOf("2026-11-01")

        val result = ChangesPolicy.daysToExport(
            days = days,
            changedDays = emptySet(),
            pendingDays = emptySet(),
            deliveredDays = delivered,
            currentDay = "2026-11-03",
            previousDay = "2026-11-02",
        )

        // The totals of the day before the current day are only complete at
        // the end of that day, so the last minutes of it are missing
        // without this.
        assertEquals(listOf("2026-11-02"), result)
    }

    @Test
    fun `a day that is waiting for the server is not read again`() {
        val days = listOf("2026-11-01", "2026-11-02")
        val pending = setOf("2026-11-01")

        val result = ChangesPolicy.daysToExport(
            days = days,
            changedDays = setOf("2026-11-01"),
            pendingDays = pending,
            deliveredDays = emptySet(),
            currentDay = "2026-11-03",
            previousDay = "2026-11-02",
        )

        // The queue holds the values of that day. They are written when the
        // queue is flushed, so the day is not read again.
        assertEquals(listOf("2026-11-02"), result)
    }

    @Test
    fun `the records of a day the server holds come from the changes`() {
        val source = ChangesPolicy.rawSource(
            day = "2026-11-01",
            changedDays = setOf("2026-11-01"),
            deliveredDays = setOf("2026-11-01"),
        )

        assertEquals(ChangesPolicy.RawSource.CHANGES, source)
    }

    @Test
    fun `the records of a day the server does not hold are read`() {
        val source = ChangesPolicy.rawSource(
            day = "2026-11-01",
            changedDays = setOf("2026-11-01"),
            deliveredDays = emptySet(),
        )

        // The changes only carry the records that were written after the
        // token. The records of this day that were written before the
        // token would be lost.
        assertEquals(ChangesPolicy.RawSource.READ, source)
    }

    @Test
    fun `the records of a day that did not change are not read at all`() {
        val source = ChangesPolicy.rawSource(
            day = "2026-11-01",
            changedDays = emptySet(),
            deliveredDays = setOf("2026-11-01"),
        )

        // The server holds the records of that day already, and nothing
        // changed since it was sent.
        assertEquals(ChangesPolicy.RawSource.NONE, source)
    }
}
