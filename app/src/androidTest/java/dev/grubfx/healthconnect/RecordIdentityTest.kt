package dev.grubfx.healthconnect

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.util.Log
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.reflect.KClass
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Checks that every record that is read carries an id.
 *
 * The exporter uses the id of a record as its identity, because the time of
 * a record is not unique: a type can hold several records with the same
 * time. The id is the one that Health Connect assigned to the record when it
 * was written.
 *
 * The test reads the records of the last years and checks that none of them
 * has an empty id. An empty id is the sentinel of a record that was built by
 * hand and was never written, so a record that comes from the platform always
 * has one.
 */
@RunWith(AndroidJUnit4::class)
class RecordIdentityTest {
    private val client: HealthConnectClient by lazy {
        HealthConnectClient.getOrCreate(
            ApplicationProvider.getApplicationContext(),
        )
    }

    @Test
    fun everyReadRecordCarriesAnId() = runBlocking {
        val granted = client.permissionController.getGrantedPermissions()
        val readPermission = HealthPermission.getReadPermission(BloodGlucoseRecord::class)
        if (!granted.contains(readPermission)) {
            // The test runner installs the app again before every run, and
            // that takes the permissions with it. A run without the
            // permission proves nothing, so the test ends here instead of
            // failing. To run it with the permission, grant it after the
            // installation:
            //   adb shell pm grant <package> \
            //     android.permission.health.READ_BLOOD_GLUCOSE
            Log.w(
                "RecordIdentityTest",
                "The read permission is not granted, so there is nothing to check",
            )
            return@runBlocking
        }

        // A wide range, because the amount of records differs per device.
        val from = Instant.now().minus(365, ChronoUnit.DAYS)
        val until = Instant.now().plus(1, ChronoUnit.DAYS)

        val records = listOf<Pair<KClass<out Record>, String>>(
            BloodGlucoseRecord::class to "blood_glucose",
            OxygenSaturationRecord::class to "oxygen_saturation",
        ).flatMap { (type, name) ->
            val read = client.readRecords(
                ReadRecordsRequest(
                    recordType = type,
                    timeRangeFilter = TimeRangeFilter.between(from, until),
                ),
            )
            Log.d("RecordIdentityTest", "Read ${read.records.size} record(s) of $name")
            read.records
        }

        Log.d(
            "RecordIdentityTest",
            "Ids of the records: ${records.map { it.metadata.id }.take(10)}",
        )

        records.forEach { record ->
            assertTrue(
                "A record that was read has to carry an id, but it is empty",
                record.metadata.id.isNotEmpty(),
            )
        }
    }

    @Test
    fun theIdOfAHandBuiltRecordIsEmpty() {
        // A record that was never written has no id. The sentinel of the
        // platform is an empty string.
        val record = Metadata.manualEntry()
        assertTrue(
            "The sentinel of the platform has to be an empty string",
            record.id.isEmpty(),
        )
    }
}
