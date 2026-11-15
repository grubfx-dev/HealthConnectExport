package dev.grubfx.healthconnect

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.changes.DeletionChange
import androidx.health.connect.client.changes.UpsertionChange
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.records.Record
import kotlinx.coroutines.flow.first

/**
 * The token of the changes of Health Connect.
 *
 * The token is kept in the data store of the app, so it survives a restart
 * of the app and the changes are not read twice.
 */
val CHANGES_TOKEN = stringPreferencesKey("changes_token")

/**
 * The records that changed since the last call.
 *
 * A change belongs to the day of its record, not to the day the export ran
 * on. A record of yesterday that is added today belongs to yesterday, and
 * the totals of yesterday have to be read again.
 *
 * @param byDay the records that were added or changed, by the day of the
 *   record.
 * @param deletedIds the ids of the records that were deleted.
 * @param nextToken the token that marks the point in time up to which the
 *   changes were read.
 */
data class ChangedRecords(
    val byDay: Map<String, List<Record>>,
    val deletedIds: Set<String>,
    val nextToken: String,
) {
    /**
     * The days that carry a change.
     */
    val days: Set<String> get() = byDay.keys
}

/**
 * Reads the changes of Health Connect.
 *
 * Health Connect counts every call of its API against a quota. The limit is
 * a fixed rate per period and a fixed number per day, and the limit in the
 * background is stricter than the one in the foreground. An export that
 * reads every type of every day spends one call for the aggregates and one
 * call per type for the raw records, so eleven calls for a single day.
 *
 * The API offers a second way to read the data: the changes. A token
 * reserves a place in the history of Health Connect, and the changes after
 * that token are the records that were added, changed or deleted since the
 * last call. One call therefore replaces the calls that read every type.
 *
 * The token is only valid for thirty days. When it expires, Health Connect
 * says so with `changesTokenExpired`, and the export reads the days again
 * that it has not read yet. The days that the server already holds are
 * marked in the queue, so they are not read a second time.
 *
 * The token is stored by the caller and not by this class, because the
 * token may only be stored when the export worked. A token that is stored
 * too early would drop the days that the export did not manage to send.
 */
class ChangesReader(private val context: Context) {
    private val healthConnect = HealthConnectClient.getOrCreate(context)

    /**
     * The token that marks the point in time up to which the data was
     * exported, or null when there is none yet.
     */
    suspend fun storedToken(): String? =
        context.dataStore.data.first()[CHANGES_TOKEN]?.takeIf { it.isNotBlank() }

    /**
     * Reads the changes since the given token.
     *
     * The loop follows the token until Health Connect says that there is
     * nothing more. The next token is only returned when every page was
     * read, so a failure in the middle does not lose the changes that were
     * not read yet.
     *
     * The records of the app itself are skipped, because the app does not
     * write into Health Connect and the records of the app would be read
     * again and again.
     *
     * The result is null when the token expired. The caller has to read the
     * days the old way then and needs a new token.
     */
    suspend fun readChanges(token: String): ChangedRecords? {
        var currentToken: String = token

        val byDay = mutableMapOf<String, MutableList<Record>>()
        val deletedIds = mutableSetOf<String>()

        while (true) {
            val response = healthConnect.getChanges(currentToken)

            if (response.changesTokenExpired) {
                Log.w(TAG, "The token expired, the days have to be read again")
                // The token is dropped, so the next run starts with a fresh
                // one and reads the days that are missing.
                context.dataStore.edit { it.remove(CHANGES_TOKEN) }
                return null
            }

            for (change in response.changes) {
                when (change) {
                    is UpsertionChange -> {
                        val record = change.record
                        if (record.metadata.dataOrigin.packageName == context.packageName) {
                            // The app does not write into Health Connect, so
                            // its own records are of no interest here.
                            continue
                        }
                        byDay.getOrPut(recordDay(record)) { mutableListOf() }.add(record)
                    }

                    is DeletionChange -> {
                        // A deletion only carries the id of the record, not
                        // its type, so the day of the record is unknown. The
                        // server keeps the value of the record, because the
                        // day it belongs to is not known here.
                        deletedIds.add(change.recordId)
                    }
                }
            }

            currentToken = response.nextChangesToken

            if (!response.hasMore) {
                break
            }
        }

        Log.d(
            TAG,
            "The changes touch ${byDay.size} day(s) and ${deletedIds.size} deletion(s)",
        )

        return ChangedRecords(byDay.mapValues { it.value.toList() }, deletedIds, currentToken)
    }

    /**
     * Reserves a place in the history of Health Connect.
     *
     * The token watches every type the app reads, not only the types that
     * are read as raw records. A type that is not watched would not show
     * up in the changes, and a day in which only that type changed would
     * be missed: the totals of that day would stay as they are.
     *
     * The token is reserved before the export and stored after it. A token
     * that is reserved before the export marks a point in time that the
     * export covers, because the export reads everything up to the moment
     * it runs. A token that was reserved after the export would leave the
     * records that were written between the export and the token behind.
     *
     * The token is not stored here. The caller stores it when the export
     * worked, because a stored token says that everything up to that point
     * is at the server.
     */
    suspend fun reserveToken(): String {
        val token = healthConnect.getChangesToken(
            ChangesTokenRequest(recordTypes = readRecordClasses.toSet())
        )
        Log.d(TAG, "A new token was reserved")
        return token
    }

    /**
     * Stores the token, because the export worked.
     */
    suspend fun storeToken(token: String) {
        context.dataStore.edit { it[CHANGES_TOKEN] = token }
    }

    companion object {
        private const val TAG = "ChangesReader"
    }
}
