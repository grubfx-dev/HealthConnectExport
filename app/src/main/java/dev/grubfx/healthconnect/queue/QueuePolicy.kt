package dev.grubfx.healthconnect.queue

/**
 * The rules of the queue, without a database.
 *
 * The rules are kept apart from Room, so they can be tested without an
 * Android runtime. The database only stores the entries, the rules decide
 * which entries stay.
 */
object QueuePolicy {
    /**
     * The days that have to be read from Health Connect.
     *
     * A day that the queue holds is not read again. The queue holds the
     * values of that day as they were read, and the server writes them
     * when the queue is flushed. Reading the day again would read it from
     * Health Connect, which returns nothing for a day that is older than
     * the history of the platform, and the server would write the empty
     * values over the good ones.
     *
     * The current day is the one exception, and only when the server
     * already holds it. Its totals are a running total that grows during
     * the day, so the newest values are the better ones, and the server
     * writes them over the older ones.
     *
     * When the queue still holds the current day, the day is not read
     * again. The flush sends the queued values first, and the day is read
     * in the run after that, when the totals have grown.
     *
     * @param days every day the export covers, oldest first.
     * @param pendingDays the days that the server did not accept yet.
     * @param deliveredDays the days the server already holds.
     * @param currentDay the day that is running.
     */
    fun daysToExport(
        days: List<String>,
        pendingDays: Set<String>,
        deliveredDays: Set<String>,
        currentDay: String,
    ): List<String> = days.filter { day ->
        when {
            day in pendingDays -> false
            day in deliveredDays -> day == currentDay
            else -> true
        }
    }

    /**
     * The days that the queue may release.
     *
     * A day that the server accepted and that lies before the first day of
     * the export can never be read again, because the export starts at the
     * day of the last successful sync. The marker of such a day is not
     * needed anymore, and the queue would grow without a limit otherwise.
     *
     * A day that the server did not accept yet is never released. It is the
     * only copy of that day: Health Connect keeps thirty days of history,
     * so a day that is older than that exists nowhere else.
     */
    fun daysToRelease(days: List<String>, firstDay: String): List<String> =
        days.filter { it < firstDay }

    /**
     * The days that would have to be dropped when the queue holds more
     * entries than the storage allows.
     *
     * The queue is never trimmed on its own. A day that the server did not
     * accept is the only copy of that day, so dropping it would lose the
     * data of that day silently. The space it takes is a smaller problem
     * than the loss.
     *
     * The method exists so that the caller can see how full the queue is.
     * It never drops anything by itself.
     */
    fun daysToDrop(days: List<String>, maxEntries: Int): List<String> {
        if (days.size <= maxEntries) {
            return emptyList()
        }
        return days.take(days.size - maxEntries)
    }
}
