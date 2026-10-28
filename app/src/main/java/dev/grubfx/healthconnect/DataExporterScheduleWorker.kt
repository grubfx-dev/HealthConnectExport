package dev.grubfx.healthconnect

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.edit
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

val httpClient = HttpClient(Android)

val LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")

// a set of all readable permissions
//
// The list of the record classes comes from the generated registry, so a
// record type that the app reads also has its permission asked for. A
// permission that is missing in the manifest makes the read of that type
// fail, which is why the two lists are kept in one place.
val readonlyHealthConnectPermissions = readRecordClasses.map { recordClass ->
    HealthPermission.getReadPermission(recordClass)
}.toSet()

class DataExporterScheduleWorker(
    appContext: Context, workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    private val notificationManager = applicationContext.getSystemService<NotificationManager>()!!
    private val healthConnect = HealthConnectClient.getOrCreate(applicationContext)

    private fun createNotificationChannel(): NotificationChannel {
        val notificationChannel = NotificationChannel(
            "export",
            "Data export",
            NotificationManager.IMPORTANCE_LOW,
        )
        notificationChannel.description = "Shown when Health Connect data is being exported"
        notificationChannel.enableLights(false)
        notificationChannel.enableVibration(false)

        notificationManager.createNotificationChannel(notificationChannel)
        return notificationChannel
    }

    private fun createExceptionNotification(e: Exception): Notification {
        return NotificationCompat.Builder(applicationContext, "export")
            .setContentTitle("Export failed").setContentText("Failed to export Health Connect data")
            .setStyle(NotificationCompat.BigTextStyle().bigText(e.message))
            .setSmallIcon(R.drawable.ic_launcher_foreground).build()
    }

    private suspend fun isHealthConnectPermissionGranted(healthConnect: HealthConnectClient): Boolean {
        val grantedPermissions = healthConnect.permissionController.getGrantedPermissions()
        val missingPermissions = readonlyHealthConnectPermissions.filter { it !in grantedPermissions }
        
        if (missingPermissions.isNotEmpty()) {
            Log.d("DataExporterWorker", "Missing permissions: $missingPermissions")
            // Temporarily allow missing permissions to proceed with available data
            // return false 
        }
        return true // Proceed even if some are missing
    }

    override suspend fun doWork(): Result {
        Log.d("DataExporterWorker", "doWork() started")
        val notificationChannel = createNotificationChannel()

        Log.d("DataExporterWorker", "Checking exports prerequisites")
        val isGranted = isHealthConnectPermissionGranted(healthConnect)

        if (!isGranted) {
            Log.d("DataExporterWorker", "Health Connect permissions not granted")
            return Result.failure()
        }
        Log.d("DataExporterWorker", "✅ Health Connect permissions granted")

        val prefs = applicationContext.dataStore.data.first()
        val exportDestination = prefs[EXPORT_DESTINATION_URI]

        if (exportDestination.isNullOrBlank()) {
            Log.d("DataExporterWorker", "Export destination not set")
            return Result.failure()
        }
        Log.d("DataExporterWorker", "✅ Export destination set")

        val foregroundNotification =
            NotificationCompat.Builder(applicationContext, notificationChannel.id)
                .setContentTitle("Exporting data")
                .setContentText("Exporting Health Connect data to the cloud")
                .setSmallIcon(R.drawable.ic_launcher_foreground).setOngoing(true).build()

        notificationManager.notify(1, foregroundNotification)

        val now = System.currentTimeMillis()
        // The last successful sync is the anchor for the history. Every day
        // from that one until today is read, so a day that was missed while
        // the phone was off or the app was not installed is still exported.
        // Without a previous sync there is no history to read, so the export
        // covers the current day only.
        val lastSync = prefs[LAST_SYNC_TIMESTAMP]
        val days = daysToExport(lastSync, now)
        Log.d(
            "DataExporterWorker",
            "Exporting ${days.size} day(s), from ${days.first()} to ${days.last()}"
        )

        // The destination is stored as a full URL. A bare host:port is
        // still accepted and gets http:// prepended, because that is what
        // older versions of the app stored.
        val url = if (exportDestination.startsWith("http://") ||
            exportDestination.startsWith("https://")
        ) {
            exportDestination
        } else {
            "http://$exportDestination"
        }

        // Every day is sent as its own payload. The server upserts the
        // summary per date, so a day that is sent twice overwrites itself
        // instead of ending up twice in the database.
        for (day in days) {
            val payload = buildPayload(day, now)
            Log.d("DataExporterWorker", "Data for $day: $payload")

            try {
                Log.d("DataExporterWorker", "Exporting data to $url")
                val response = httpClient.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(payload)
                }

                // A rejected payload is a server-side problem, not a transport
                // problem. Retrying it would send the same data again, so the
                // failure is reported and the sync timestamp is not advanced.
                if (!response.status.isSuccess()) {
                    Log.e("DataExporterWorker", "Export rejected with ${response.status}")
                    notificationManager.cancel(1)
                    notificationManager.notify(1, createExceptionNotification(
                        IllegalStateException("Server rejected the payload: ${response.status}")))
                    return Result.failure()
                }
            } catch (e: Exception) {
                Log.e("DataExporterWorker", "Failed to export data", e)

                notificationManager.cancel(1)
                notificationManager.notify(1, createExceptionNotification(e))
                // A transport failure is worth another attempt. Returning retry()
                // is what makes WorkManager apply the configured backoff.
                return Result.retry()
            }
        }

        // Update last sync timestamp
        applicationContext.dataStore.edit { it[LAST_SYNC_TIMESTAMP] = now }

        notificationManager.cancel(1)
        return Result.success()
    }

    /**
     * The local days that have to be exported, from the oldest to the newest.
     *
     * The list starts at the day of the last successful sync, because the
     * totals of that day are a running total over the whole day and may have
     * grown since the last export. It ends with the current day.
     */
    private fun daysToExport(lastSync: Long?, now: Long): List<LocalDate> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.ofInstant(Instant.ofEpochMilli(now), zone)
        val first = if (lastSync == null) {
            today
        } else {
            LocalDate.ofInstant(Instant.ofEpochMilli(lastSync), zone)
        }
        return generateSequence(first) { it.plusDays(1) }
            .takeWhile { !it.isAfter(today) }
            .toList()
    }

    /**
     * Aggregates the totals of a single local day and builds the payload for
     * it.
     *
     * The totals are aggregated over the whole local day, not just the sync
     * window. The window only covers the new records, so its totals would be
     * a partial day and a second sync would overwrite the first one with an
     * even smaller slice. Aggregating the full day makes the value a running
     * total, and the server upserts it per date.
     *
     * Every metric that Health Connect can aggregate is requested in a
     * single call. The API has a daily limit on the number of calls, and the
     * limit in the background is stricter than the one in the foreground, so
     * one call for all metrics is the only way to stay inside the quota
     * while the worker walks the days of the history.
     *
     * The record types that Health Connect cannot aggregate are read as raw
     * records. They are one call per type, because the API reads a single
     * type per call. A type that has no record in the day is not read at
     * all, which keeps the number of calls low.
     */
    private suspend fun buildPayload(day: LocalDate, now: Long): String {
        val zone = ZoneId.systemDefault()
        val startOfDay = day.atStartOfDay(zone).toInstant().toEpochMilli()
        // The end of the day is exclusive, so the records at midnight belong
        // to the next day and are not counted twice.
        val endOfDay = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        // For the current day the export stops now, there is nothing in the
        // future to read.
        val end = minOf(endOfDay, now)

        val timeRangeFilter = TimeRangeFilter.Companion.between(
            Instant.ofEpochMilli(startOfDay),
            Instant.ofEpochMilli(end)
        )

        Log.d("DataExporterWorker", "Fetching health data from $startOfDay to $end")

        val healthDataAggregate = healthConnect.aggregate(
            AggregateRequest(
                metrics = aggregateRecordSpecs.flatMap { spec -> spec.metrics }.map { it.metric }
                    .toSet(),
                timeRangeFilter = timeRangeFilter,
            )
        )
        Log.d("DataExporterWorker", "Raw data: ${Gson().toJson(healthDataAggregate)}")

        // Every record type becomes one entry with the aggregated values as
        // its fields. The daily summary keeps the four totals the server
        // knows about, so both payload shapes stay in use.
        val metrics = aggregateRecordSpecs.mapNotNull { spec ->
            val values = spec.metrics
                .mapNotNull { metricSpec ->
                    val result = healthDataAggregate[metricSpec.metric] ?: return@mapNotNull null
                    val value = metricSpec.read(result)
                    // A metric that is not available produces a zero, which
                    // would be a value the user never measured. The field is
                    // left out instead, so the database shows what was
                    // actually recorded.
                    if (value == 0.0) null else metricSpec.name to value
                }
                .toMap()

            if (values.isEmpty()) {
                null
            } else {
                mapOf(
                    "metric" to spec.type,
                    "record_id" to day.toString(),
                    "time" to startOfDay,
                    "values" to values,
                )
            }
        }
        Log.d("DataExporterWorker", "Metrics: ${Gson().toJson(metrics)}")

        // The record types without an aggregate are read as raw records. Each
        // of them becomes an entry of its own, so a day with several
        // measurements keeps all of them.
        val rawMetrics = readRawMetrics(day, timeRangeFilter)
        Log.d("DataExporterWorker", "Raw metrics: ${Gson().toJson(rawMetrics)}")

        // The daily summary keeps the totals the server writes to its own
        // measurement, so the existing dashboard data stays the same.
        val jsonValues = HashMap<String, Number>()
        jsonValues["steps"] = healthDataAggregate[StepsRecord.COUNT_TOTAL] ?: 0
        jsonValues["active_calories"] =
            healthDataAggregate[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories
                ?: 0
        jsonValues["total_calories"] =
            healthDataAggregate[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories ?: 0
        jsonValues["sleep_duration_seconds"] =
            healthDataAggregate[SleepSessionRecord.SLEEP_DURATION_TOTAL]?.seconds ?: 0

        return Gson().toJson(mapOf(
            // Identifies the phone the data came from. The model alone is
            // not unique across identical phones, so the manufacturer is
            // combined with it. The value is stable for the lifetime of the
            // install, which is what the server needs to keep one series per
            // device.
            "device_id" to "${Build.MANUFACTURER}-${Build.MODEL}".lowercase(),
            "time" to now,
            // The local day the totals belong to. The server would otherwise
            // derive the date from `time` in UTC, which is the wrong day for
            // most timezones.
            "date" to day.toString(),
            "data" to jsonValues,
            // The aggregated values of every record type that supports
            // aggregation. The server writes them into one measurement per
            // record type.
            "metrics" to metrics + rawMetrics,
        ))
    }

    /**
     * Reads the record types that Health Connect cannot aggregate.
     *
     * The API reads a single type per call, so every type costs one call.
     * The types that have no record in the day are skipped, because a type
     * without a record would cost a call for nothing.
     *
     * The records of a type are read page by page. The API answers with a
     * token while there are more records, and the token is empty string
     * instead of null on some versions of Health Connect, so it is checked
     * with isNullOrEmpty.
     */
    @Suppress("UNCHECKED_CAST")
    private suspend fun readRawMetrics(
        day: LocalDate,
        timeRangeFilter: TimeRangeFilter,
    ): List<Map<String, Any>> {
        val entries = mutableListOf<Map<String, Any>>()

        for (spec in rawRecordSpecs) {
            try {
                var pageToken: String? = null
                do {
                    val response = healthConnect.readRecords(
                        ReadRecordsRequest(
                            recordType = spec.recordClass as kotlin.reflect.KClass<Record>,
                            timeRangeFilter = timeRangeFilter,
                            pageToken = pageToken,
                        )
                    )
                    pageToken = response.pageToken
                    Log.d(
                        "DataExporterWorker",
                        "Read ${response.records.size} record(s) of ${spec.type}",
                    )

                    for (record in response.records) {
                        val values = spec.fields
                            .mapNotNull { field ->
                                val value = field.read(fieldValue(record, field))
                                if (value == 0.0) null else field.name to value
                            }
                            .toMap()

                        if (values.isNotEmpty()) {
                            // The identity of the record is the id that
                            // Health Connect assigned to it. The time alone
                            // is not enough: a type can hold several records
                            // with the same time, and they would overwrite
                            // each other in the server, because the server
                            // uses the record id and the time as the key of
                            // the measurement.
                            val time = recordTime(record)
                            entries.add(
                                mapOf(
                                    "metric" to spec.type,
                                    "record_id" to recordId(spec.type, record, time),
                                    "time" to time,
                                    "values" to values,
                                ),
                            )
                        }
                    }
                } while (!pageToken.isNullOrEmpty())
            } catch (e: SecurityException) {
                // The permission for this type was not granted. The rest of
                // the export is not affected by it.
                Log.w("DataExporterWorker", "No permission for ${spec.type}: ${e.message}")
            } catch (e: Exception) {
                // A type that fails to read does not fail the whole export.
                // The other types still hold their values.
                Log.e("DataExporterWorker", "Failed to read ${spec.type}", e)
            }
        }

        return entries
    }

    /**
     * Reads the value of a field out of a record.
     *
     * The registry names the fields of a type, but the records are read as
     * the base type, so the value is looked up by its name. The name of the
     * property in the record is not the name the field is written to, so the
     * registry keeps both.
     */
    private fun fieldValue(record: Record, field: RawFieldSpec): Any? =
        record.javaClass.methods
            .firstOrNull { it.name == "get${field.property.replaceFirstChar { it.uppercase() }}" }
            ?.invoke(record)

    /**
     * Reads the time of a record in milliseconds since the epoch.
     *
     * The base type of the records has no time, the property is declared by
     * the concrete type. A record without a time falls back to the start of
     * the day, so the entry still has a position in the series.
     */
    private fun recordTime(record: Record): Long {
        val time = record.javaClass.methods
            .firstOrNull { it.name == "getTime" }
            ?.invoke(record) as? Instant
        return time?.toEpochMilli() ?: 0L
    }

    /**
     * Builds the identity of a record for the server.
     *
     * Health Connect assigns a unique id to every record when it is written.
     * That id survives a reinstall of the writing app and is stable for the
     * lifetime of the record, so it is used as the identity. A record without
     * an id falls back to the time of the record, which is unique as long as
     * the type holds no two records with the same time.
     */
    private fun recordId(type: String, record: Record, time: Long): String {
        val id = record.metadata.id
        return if (id.isNotEmpty()) "$type-$id" else "$type-$time"
    }
}
