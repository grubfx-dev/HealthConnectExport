package dev.grubfx.healthconnect

/**
 * The rules of the export, when the changes of Health Connect are used.
 *
 * The changes say which records were added or changed since the last run.
 * They cost one call for all the types, instead of one call per type and
 * per day. The rules decide which days are read and where the records of a
 * day come from.
 *
 * The rules are kept apart from the worker, so they can be tested without
 * an Android runtime.
 */
object ChangesPolicy {

    /**
     * Where the raw records of a day come from.
     */
    enum class RawSource {
        /**
         * The records come from the changes. Only the records that were
         * added or changed are sent, because the server holds the others
         * already.
         */
        CHANGES,

        /**
         * The records are read from Health Connect, one call per type. This
         * is the way for a day the server does not hold yet: the changes
         * only carry the records that were written after the token, and the
         * records before the token would be lost.
         */
        READ,

        /**
         * The day carries no raw records. The day is at the server and
         * nothing changed since it was sent, so the records it holds are
         * the ones the server already has.
         */
        NONE,
    }

    /**
     * The days that have to be read.
     *
     * A day is read when it changed, when the server does not hold it yet,
     * when it is the current day or when it is the day before it.
     *
     * The current day is always read, because its totals are a running
     * total that grows during the day.
     *
     * The day before it is always read as well, because the totals of that
     * day are only complete at the end of the day. The export of the day
     * before it stops at the moment it runs, so the last minutes of that
     * day are missing without this.
     *
     * A day that is in the queue is not read. The queue holds the values of
     * that day as they were read, and the server writes them when the queue
     * is flushed. Reading the day again would read it from Health Connect,
     * which returns nothing for a day that is older than the history of
     * the platform, and the server would write the empty values over the
     * good ones.
     *
     * @param days every day the export covers, oldest first.
     * @param changedDays the days the changes touch.
     * @param pendingDays the days that the server did not accept yet.
     * @param deliveredDays the days the server already holds.
     * @param currentDay the day that is running.
     * @param previousDay the day before the current day.
     */
    fun daysToExport(
        days: List<String>,
        changedDays: Set<String>,
        pendingDays: Set<String>,
        deliveredDays: Set<String>,
        currentDay: String,
        previousDay: String?,
    ): List<String> = days.filter { day ->
        when {
            day in pendingDays -> false
            day in changedDays -> true
            day !in deliveredDays -> true
            day == currentDay -> true
            day == previousDay -> true
            else -> false
        }
    }

    /**
     * Where the raw records of a day come from.
     *
     * A day that the server holds and that changed comes from the changes.
     * Only the records that were added or changed are sent, because the
     * server holds the others already.
     *
     * A day that the server does not hold yet is read from Health Connect.
     * The changes only carry the records that were written after the token,
     * and the records of that day that were written before the token would
     * be lost.
     *
     * A day that the server holds and that did not change carries no raw
     * records. The records it has are the ones the server already has.
     *
     * @param day the day that is built.
     * @param changedDays the days the changes touch.
     * @param deliveredDays the days the server already holds.
     */
    fun rawSource(
        day: String,
        changedDays: Set<String>,
        deliveredDays: Set<String>,
    ): RawSource = when {
        day in changedDays && day in deliveredDays -> RawSource.CHANGES
        day in deliveredDays -> RawSource.NONE
        else -> RawSource.READ
    }
}
