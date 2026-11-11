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
     * The queued payloads, the oldest day first.
     */
    @Query("SELECT * FROM queued_payloads ORDER BY day ASC")
    suspend fun all(): List<QueuedPayload>

    /**
     * Removes a payload that the server accepted.
     */
    @Query("DELETE FROM queued_payloads WHERE day = :day")
    suspend fun delete(day: String)

    /**
     * The number of queued payloads.
     */
    @Query("SELECT COUNT(*) FROM queued_payloads")
    suspend fun count(): Int

    /**
     * The days that are queued, the oldest day first.
     */
    @Query("SELECT day FROM queued_payloads ORDER BY day ASC")
    suspend fun days(): List<String>

    /**
     * The oldest queued payloads, the ones that are dropped when the queue
     * is full.
     */
    @Query("SELECT * FROM queued_payloads ORDER BY day ASC LIMIT :limit")
    suspend fun oldest(limit: Int): List<QueuedPayload>
}
