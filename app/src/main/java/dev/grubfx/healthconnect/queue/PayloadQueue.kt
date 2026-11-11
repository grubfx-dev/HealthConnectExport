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
 * The queue is not bounded. A queued day is the only copy of that day:
 * Health Connect keeps thirty days of history, so a day that is older
 * than that exists nowhere else. Dropping it would lose the data of that
 * day, and a few kilobytes of storage are a smaller problem than the loss.
 */
class PayloadQueue(context: Context) {
    private val dao = PayloadQueueDatabase.get(context).payloadQueueDao()

    /**
     * Puts a payload into the queue.
     *
     * The day is the key of the entry, so a day that is queued twice
     * replaces its earlier entry. The later values are the ones that were
     * read last, and they are the running total of the day.
     */
    suspend fun enqueue(day: String, payload: String, queuedAt: Long) {
        dao.upsert(QueuedPayload(day = day, payload = payload, queuedAt = queuedAt))
    }

    /**
     * The payloads that the server did not accept yet, the oldest day
     * first.
     */
    suspend fun pending(): List<QueuedPayload> = dao.pending()

    /**
     * Every entry of the queue, the oldest day first.
     */
    suspend fun all(): List<QueuedPayload> = dao.all()

    /**
     * Marks a day as accepted by the server.
     *
     * The entry stays as a marker, because it says that the day must not
     * be read again. Only the payload is dropped, because the server holds
     * it now.
     */
    suspend fun markDelivered(day: String) {
        dao.markDelivered(day)
    }

    /**
     * Removes an entry.
     */
    suspend fun remove(day: String) {
        dao.delete(day)
    }

    /**
     * The number of entries.
     */
    suspend fun size(): Int = dao.count()

    /**
     * The days of the queue, the oldest day first.
     */
    suspend fun days(): List<String> = dao.days()

    /**
     * The days that are still waiting for the server.
     */
    suspend fun pendingDays(): List<String> = dao.pendingDays()

    /**
     * The days the server already holds.
     */
    suspend fun deliveredDays(): List<String> = dao.deliveredDays()
}
