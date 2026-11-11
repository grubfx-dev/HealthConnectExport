package dev.grubfx.healthconnect.queue

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The rules of the queue, tested without a database.
 */
class QueuePolicyTest {
    @Test
    fun `a day the server does not have yet is read`() {
        val days = listOf("2026-11-01", "2026-11-02", "2026-11-03")

        val result = QueuePolicy.daysToExport(days, emptySet(), emptySet(), "2026-11-03")

        assertEquals(days, result)
    }

    @Test
    fun `a day that is waiting for the server is not read again`() {
        val days = listOf("2026-11-01", "2026-11-02", "2026-11-03")
        val pending = setOf("2026-11-02")

        val result = QueuePolicy.daysToExport(days, pending, emptySet(), "2026-11-03")

        assertEquals(listOf("2026-11-01", "2026-11-03"), result)
    }

    @Test
    fun `a day the server holds is not read again`() {
        val days = listOf("2026-11-01", "2026-11-02")
        val delivered = setOf("2026-11-01")

        val result = QueuePolicy.daysToExport(days, emptySet(), delivered, "2026-11-02")

        assertEquals(listOf("2026-11-02"), result)
    }

    @Test
    fun `the current day is read again when the server holds it`() {
        val days = listOf("2026-11-01", "2026-11-02")
        val delivered = setOf("2026-11-01", "2026-11-02")

        // The totals of the current day are a running total that grows
        // during the day, so the newest values are the better ones.
        val result = QueuePolicy.daysToExport(days, emptySet(), delivered, "2026-11-02")

        assertEquals(listOf("2026-11-02"), result)
    }

    @Test
    fun `the current day is not read again while it waits for the server`() {
        val days = listOf("2026-11-02")
        val pending = setOf("2026-11-02")

        // The flush sends the queued values first. The day is read in the
        // run after that, when the totals have grown.
        val result = QueuePolicy.daysToExport(days, pending, emptySet(), "2026-11-02")

        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun `a day that is older than the history is never read again`() {
        val days = listOf("2026-09-01", "2026-11-02")
        val delivered = setOf("2026-09-01")

        val result = QueuePolicy.daysToExport(days, emptySet(), delivered, "2026-11-02")

        assertEquals(listOf("2026-11-02"), result)
    }

    @Test
    fun `the queue is never trimmed on its own`() {
        val days = (1..5000).map { "day-$it" }

        // The queue holds every day. There is no limit that drops a day,
        // because a queued day is the only copy of that day.
        assertEquals(emptyList<String>(), QueuePolicy.daysToDrop(days, Int.MAX_VALUE))
    }

    @Test
    fun `a marker of a day before the export is not needed anymore`() {
        val days = listOf("2026-01-01", "2026-11-02")

        val released = QueuePolicy.daysToRelease(days, "2026-11-01")

        assertEquals(listOf("2026-01-01"), released)
    }

    @Test
    fun `a marker of a day inside the export stays`() {
        val days = listOf("2026-11-01", "2026-11-02")

        val released = QueuePolicy.daysToRelease(days, "2026-11-01")

        assertEquals(emptyList<String>(), released)
    }
}
