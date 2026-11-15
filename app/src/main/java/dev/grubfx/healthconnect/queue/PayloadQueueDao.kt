package dev.grubfx.healthconnect.queue

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * The queue of the payloads that the server did not accept.
 *
 * The order of the queue is the order of the days, because the server
 * upserts the summary per date and a day that is sent twice overwrites the
 * first one. A later day therefore has to arrive after the earlier one.
 */
@Dao
interface PayloadQueueDao {
    /**
     * Puts a payload into the queue.
     *
     * The day is the key of the entry, so a day that is queued twice
     * replaces its earlier entry instead of producing a second one. The
     * queue never holds two payloads of the same day.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(payload: QueuedPayload)

    /**
     * The payloads that the server did not accept yet, the oldest day
     * first.
     */
    @Query("SELECT * FROM queued_payloads WHERE delivered = 0 ORDER BY day ASC")
    suspend fun pending(): List<QueuedPayload>

    /**
     * Every entry of the queue, the oldest day first. The delivered ones
     * are the markers that say the server holds the day.
     */
    @Query("SELECT * FROM queued_payloads ORDER BY day ASC")
    suspend fun all(): List<QueuedPayload>

    /**
     * Turns an entry into a marker of a day the server accepted.
     *
     * The payload is dropped, because the server holds it now. The entry
     * stays, because it says that the day must not be read again.
     */
    @Query("UPDATE queued_payloads SET delivered = 1, payload = '' WHERE day = :day")
    suspend fun markDelivered(day: String)

    /**
     * Marks a day the server accepted, even if there is no entry for it.
     *
     * A day that the worker sends right away never sat in the queue, so
     * there is no entry that the update could change. The marker is the
     * entry, and the entry is the marker, so it is written here.
     *
     * The entry carries no payload, because the payload is in the server.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMarker(marker: QueuedPayload)

    /**
     * Removes an entry.
     */
    @Query("DELETE FROM queued_payloads WHERE day = :day")
    suspend fun delete(day: String)

    /**
     * The number of entries.
     */
    @Query("SELECT COUNT(*) FROM queued_payloads")
    suspend fun count(): Int

    /**
     * The days of the queue, the oldest day first.
     */
    @Query("SELECT day FROM queued_payloads ORDER BY day ASC")
    suspend fun days(): List<String>

    /**
     * The days that are still waiting for the server.
     */
    @Query("SELECT day FROM queued_payloads WHERE delivered = 0 ORDER BY day ASC")
    suspend fun pendingDays(): List<String>

    /**
     * The days the server already holds.
     */
    @Query("SELECT day FROM queued_payloads WHERE delivered = 1 ORDER BY day ASC")
    suspend fun deliveredDays(): List<String>
}
