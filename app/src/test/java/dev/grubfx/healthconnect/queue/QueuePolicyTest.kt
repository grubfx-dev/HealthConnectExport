package dev.grubfx.healthconnect.queue

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The rules of the queue, tested without a database.
 */
class QueuePolicyTest {
    @Test
    fun `a queue that is not full keeps every day`() {
        val days = (1..10).map { "2026-11-%02d".format(it) }

        assertEquals(emptyList<String>(), QueuePolicy.daysToDrop(days))
    }

    @Test
    fun `a queue that is exactly full keeps every day`() {
        val days = (1..QueuePolicy.MAX_ENTRIES).map { "day-$it" }

        assertEquals(emptyList<String>(), QueuePolicy.daysToDrop(days))
    }

    @Test
    fun `a queue that is over full drops the oldest days`() {
        val days = (1..QueuePolicy.MAX_ENTRIES + 3).map { "day-$it" }

        val dropped = QueuePolicy.daysToDrop(days)

        assertEquals(listOf("day-1", "day-2", "day-3"), dropped)
    }

    @Test
    fun `an empty queue drops nothing`() {
        assertEquals(emptyList<String>(), QueuePolicy.daysToDrop(emptyList()))
    }
}
