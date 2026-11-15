package dev.grubfx.healthconnect

import androidx.health.connect.client.records.Record
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The time of a record in milliseconds since the epoch.
 *
 * The base type of the records has no time, the property is declared by the
 * concrete type. A record that holds an interval has a start and an end,
 * and it is counted for the day it starts in, so its start is the time of
 * the record. A record with neither falls back to zero, so the entry still
 * has a position in the series.
 *
 * The function is used by the export and by the reader of the changes, and
 * both have to agree about the time of a record, because the time is half
 * of the identity of a point in the database.
 */
fun recordTime(record: Record): Long {
    val time = record.javaClass.methods
        .firstOrNull { it.name == "getTime" }
        ?.invoke(record) as? Instant
    if (time != null) {
        return time.toEpochMilli()
    }

    val start = record.javaClass.methods
        .firstOrNull { it.name == "getStartTime" }
        ?.invoke(record) as? Instant
    return start?.toEpochMilli() ?: 0L
}

/**
 * The local day of a record, as the server knows the days.
 *
 * The day is the day of the start of the record, because a record that
 * spans midnight belongs to the day it starts in. The day is the local day
 * and not the day in UTC, because the export covers the local days and the
 * server writes them under the same name.
 *
 * The reader of the changes and the export have to agree about the day of a
 * record, because the changes are sorted by day and the export asks for
 * the records of a day by its name.
 */
fun recordDay(record: Record): String =
    Instant.ofEpochMilli(recordTime(record))
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .toString()

/**
 * Builds the identity of a record for the server.
 *
 * Health Connect assigns a unique id to every record when it is written.
 * That id survives a reinstall of the writing app and is stable for the
 * lifetime of the record, so it is used as the identity. A record without
 * an id falls back to the time of the record, which is unique as long as
 * the type holds no two records with the same time.
 */
fun recordId(type: String, record: Record, time: Long): String {
    val id = record.metadata.id
    return if (id.isNotEmpty()) "$type-$id" else "$type-$time"
}
