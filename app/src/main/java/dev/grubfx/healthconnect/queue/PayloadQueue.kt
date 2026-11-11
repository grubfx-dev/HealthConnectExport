package dev.grubfx.healthconnect.queue

import android.content.Context
import android.util.Log

/**
 * The queue of the payloads that the server did not accept.
 *
 * The queue is the durable half of the export. The retry of the worker
 * covers a transport failure while the app runs, but a phone that is out of
 * reach for days would lose the days it could not send. The queue keeps
 * them until the server accepts them.
 *
 * The queue is bounded. A server that is gone for a year would fill the
 * storage of the phone otherwise. The oldest entries are dropped, because
 * a day that is too old has lost most of its value anyway, and the newest
 * ones describe the present state of the health data.
 */
class PayloadQueue(context: Context) {
    private val dao = PayloadQueueDatabase.get(context).payloadQueueDao()

    /**
     * Puts a payload into the queue and drops the oldest entries when the
     * queue is full.
     */
    suspend fun enqueue(day: String, payload: String, queuedAt: Long) {
        dao.upsert(QueuedPayload(day = day, payload = payload, queuedAt = queuedAt))
        trim()
    }

    /**
     * The queued payloads, the oldest day first.
     */
    suspend fun all(): List<QueuedPayload> = dao.all()

    /**
     * Removes a payload that the server accepted.
     */
    suspend fun remove(day: String) {
        dao.delete(day)
    }

    /**
     * The number of queued payloads.
     */
    suspend fun size(): Int = dao.count()

    /**
     * The days that are queued, the oldest day first.
     */
    suspend fun days(): List<String> = dao.days()

    /**
     * Drops the oldest entries until the queue holds at most
     * `MAX_ENTRIES`.
     *
     * The oldest entry is dropped first, because the newest entries
     * describe the present state of the health data and the oldest ones
     * have lost the most of their value.
     */
    private suspend fun trim() {
        val days = dao.days()
        val dropped = QueuePolicy.daysToDrop(days)
        dropped.forEach { day ->
            dao.delete(day)
        }
        if (dropped.isNotEmpty()) {
            Log.w(
                "PayloadQueue",
                "The queue was full, dropped ${dropped.size} of the oldest entries: " +
                    dropped.joinToString(", "),
            )
        }
    }

    companion object {
        /** The number of payloads the queue holds. */
        const val MAX_ENTRIES = QueuePolicy.MAX_ENTRIES
    }
}
