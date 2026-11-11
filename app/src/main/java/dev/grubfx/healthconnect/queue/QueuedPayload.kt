package dev.grubfx.healthconnect.queue

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One day of the export.
 *
 * The queue holds the payload as it was built, so a day that could not be
 * sent is sent later with the same values and not with values that are
 * read again at a later time. A running total that is read again would
 * carry the values of a later day, and the server would write them to the
 * day of the first attempt.
 *
 * The day is the primary key of the entry, so a day that is queued twice
 * replaces its earlier entry instead of producing a second one. The day is
 * also the order of the queue: the server upserts the summary per date, so
 * the days have to arrive in the order of their dates.
 *
 * A delivered day keeps its entry as a marker and drops its payload. The
 * entry is the marker that says the server holds this day. Without it the
 * day would be read again from Health Connect, which returns nothing for a
 * day that is older than the history of the platform, and the server would
 * write the empty values over the ones it already has.
 */
@Entity(tableName = "queued_payloads")
data class QueuedPayload(
    /** The local day the payload belongs to, as `yyyy-MM-dd`. */
    @PrimaryKey
    @ColumnInfo(name = "day")
    val day: String,

    /** The payload as it was built, as the JSON the server expects. */
    @ColumnInfo(name = "payload")
    val payload: String,

    /** When the payload was queued, in milliseconds since the epoch. */
    @ColumnInfo(name = "queued_at")
    val queuedAt: Long,

    /**
     * Whether the server accepted this day.
     *
     * A delivered day keeps its entry as a marker and drops its payload,
     * because the payload is in the server now.
     */
    @ColumnInfo(name = "delivered", defaultValue = "0")
    val delivered: Boolean = false,
)
