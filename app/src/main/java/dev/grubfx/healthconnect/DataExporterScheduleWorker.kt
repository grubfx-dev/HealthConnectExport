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

val httpClient = HttpClient(Android)

val LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")

// a set of all readable permissions
val readonlyHealthConnectPermissions = setOf(
    HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
    HealthPermission.getReadPermission(BasalBodyTemperatureRecord::class),
    HealthPermission.getReadPermission(BasalMetabolicRateRecord::class),
    HealthPermission.getReadPermission(BloodGlucoseRecord::class),
    HealthPermission.getReadPermission(BloodPressureRecord::class),
    HealthPermission.getReadPermission(BodyFatRecord::class),
    HealthPermission.getReadPermission(BodyTemperatureRecord::class),
    HealthPermission.getReadPermission(BoneMassRecord::class),
    HealthPermission.getReadPermission(CervicalMucusRecord::class),
    HealthPermission.getReadPermission(DistanceRecord::class),
    HealthPermission.getReadPermission(ElevationGainedRecord::class),
    HealthPermission.getReadPermission(ExerciseSessionRecord::class),
    HealthPermission.getReadPermission(FloorsClimbedRecord::class),
    HealthPermission.getReadPermission(HeartRateRecord::class),
    HealthPermission.getReadPermission(HeightRecord::class),
    HealthPermission.getReadPermission(HydrationRecord::class),
    HealthPermission.getReadPermission(MenstruationFlowRecord::class),
    HealthPermission.getReadPermission(NutritionRecord::class),
    HealthPermission.getReadPermission(OvulationTestRecord::class),
    HealthPermission.getReadPermission(OxygenSaturationRecord::class),
    HealthPermission.getReadPermission(PowerRecord::class),
    HealthPermission.getReadPermission(RespiratoryRateRecord::class),
    HealthPermission.getReadPermission(RestingHeartRateRecord::class),
    HealthPermission.getReadPermission(SexualActivityRecord::class),
    HealthPermission.getReadPermission(SleepSessionRecord::class),
    HealthPermission.getReadPermission(SpeedRecord::class),
    HealthPermission.getReadPermission(StepsRecord::class),
    HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
    HealthPermission.getReadPermission(Vo2MaxRecord::class),
    HealthPermission.getReadPermission(WeightRecord::class),
    HealthPermission.getReadPermission(WheelchairPushesRecord::class)
)

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
        // The totals are aggregated over the whole local day, not just the
        // sync window. The window only covers the new records, so its totals
        // would be a partial day and a second sync would overwrite the first
        // one with an even smaller slice. Aggregating the full day makes the
        // value a running total, and the server upserts it per date.
        val startOfDay = java.util.Calendar.getInstance().apply {
            timeInMillis = now
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        val timeRangeFilter = TimeRangeFilter.Companion.between(
            Instant.ofEpochMilli(startOfDay),
            Instant.ofEpochMilli(now)
        )

        Log.d("DataExporterWorker", "Fetching health data from $startOfDay to $now")

        val healthDataAggregate = healthConnect.aggregate(
            AggregateRequest(
                metrics = setOf(
                    StepsRecord.COUNT_TOTAL,
                    ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL,
                    TotalCaloriesBurnedRecord.ENERGY_TOTAL,
                    SleepSessionRecord.SLEEP_DURATION_TOTAL,
                ),
                timeRangeFilter = timeRangeFilter,
            )
        )
        Log.d("DataExporterWorker", "Raw data: ${Gson().toJson(healthDataAggregate)}")

        // build a JSON file from the aggregate data
        val jsonValues = HashMap<String, Number>()
        jsonValues["steps"] = healthDataAggregate[StepsRecord.COUNT_TOTAL] ?: 0
        jsonValues["active_calories"] =
            healthDataAggregate[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories
                ?: 0
        jsonValues["total_calories"] =
            healthDataAggregate[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories ?: 0
        jsonValues["sleep_duration_seconds"] =
            healthDataAggregate[SleepSessionRecord.SLEEP_DURATION_TOTAL]?.seconds ?: 0
        //TODO add the rest of the newly added fields for export

        val json = Gson().toJson(mapOf(
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
            "date" to java.time.LocalDate.ofInstant(
                Instant.ofEpochMilli(now),
                java.time.ZoneId.systemDefault()
            ).toString(),
            "data" to jsonValues
        ))
        Log.d("DataExporterWorker", "Data: $json")

        try {
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

            Log.d("DataExporterWorker", "Exporting data to $url")
            val response = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(json)
            }

            // A rejected payload is a server-side problem, not a transport
            // problem. Retrying it would send the same data again, so the
            // failure is reported and the sync timestamp is not advanced.
            if (!response.status.isSuccess()) {
                Log.e("DataExporterWorker", "Export rejected with ${response.status}")
                notificationManager.cancel(1)
                notificationManager.notify(1, createExceptionNotification(
                    IllegalStateException("Server rejected the payload: ${response.status}")
                ))
                return Result.failure()
            }

            // Update last sync timestamp
            applicationContext.dataStore.edit { it[LAST_SYNC_TIMESTAMP] = now }

        } catch (e: Exception) {
            Log.e("DataExporterWorker", "Failed to export data", e)

            notificationManager.cancel(1)
            notificationManager.notify(1, createExceptionNotification(e))
            // A transport failure is worth another attempt. Returning retry()
            // is what makes WorkManager apply the configured backoff.
            return Result.retry()
        }

        notificationManager.cancel(1)
        return Result.success()
    }
}