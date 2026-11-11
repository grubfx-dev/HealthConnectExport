package dev.grubfx.healthconnect.queue

/**
 * The rules of the queue, without a database.
 *
 * The rules are kept apart from Room, so they can be tested without an
 * Android runtime. The database only stores the entries, the rules decide
 * which entries stay.
 */
object QueuePolicy {
    /** The number of payloads the queue holds. */
    const val MAX_ENTRIES = 1000

    /**
     * The days that are dropped when the queue holds more than
     * `MAX_ENTRIES` entries.
     *
     * The oldest entries are dropped, because the newest ones describe the
     * present state of the health data and the oldest ones have lost the
     * most of their value.
     */
    fun daysToDrop(days: List<String>): List<String> {
        if (days.size <= MAX_ENTRIES) {
            return emptyList()
        }
        return days.take(days.size - MAX_ENTRIES)
    }
}
