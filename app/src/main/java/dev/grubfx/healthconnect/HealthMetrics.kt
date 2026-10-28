// This file is generated. Do not edit it.
// Run `./gradlew generateMetrics` to regenerate it from the Health Connect SDK.

package dev.grubfx.healthconnect

import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalBodyTemperatureRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.BodyWaterMassRecord
import androidx.health.connect.client.records.BoneMassRecord
import androidx.health.connect.client.records.CervicalMucusRecord
import androidx.health.connect.client.records.CyclingPedalingCadenceRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.IntermenstrualBleedingRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.MenstruationFlowRecord
import androidx.health.connect.client.records.MenstruationPeriodRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.OvulationTestRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.PowerRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SexualActivityRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.WheelchairPushesRecord
import androidx.health.connect.client.records.Record
import kotlin.reflect.KClass
import androidx.health.connect.client.units.BloodGlucose
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Power
import androidx.health.connect.client.units.Pressure
import androidx.health.connect.client.units.Temperature
import androidx.health.connect.client.units.Velocity
import androidx.health.connect.client.units.Volume
import androidx.health.connect.client.units.Percentage
import java.time.Duration

/**
 * The aggregate metrics of the record types that support them.
 *
 * Every metric can be requested in a single aggregate call. The Health Connect API
 * has a daily limit on the number of calls, and the limit in the background is
 * stricter than the one in the foreground, so one call for all metrics is the only
 * way to stay inside the quota.
 */
val aggregateRecordSpecs: List<RecordSpec> = listOf(
    RecordSpec(
        type = "active_calories_burned",
        metrics = listOf(
            MetricSpec("energy_total", ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL) { (it as? Energy)?.inKilocalories ?: 0.0 },
        ),
    ),
    RecordSpec(
        type = "basal_metabolic_rate",
        metrics = listOf(
            MetricSpec("energy_total", BasalMetabolicRateRecord.BASAL_CALORIES_TOTAL) { (it as? Energy)?.inKilocalories ?: 0.0 },
        ),
    ),
    RecordSpec(
        type = "blood_pressure",
        metrics = listOf(
            MetricSpec("diastolic_avg", BloodPressureRecord.DIASTOLIC_AVG) { (it as? Pressure)?.inMillimetersOfMercury ?: 0.0 },
            MetricSpec("diastolic_max", BloodPressureRecord.DIASTOLIC_MAX) { (it as? Pressure)?.inMillimetersOfMercury ?: 0.0 },
            MetricSpec("diastolic_min", BloodPressureRecord.DIASTOLIC_MIN) { (it as? Pressure)?.inMillimetersOfMercury ?: 0.0 },
            MetricSpec("systolic_avg", BloodPressureRecord.SYSTOLIC_AVG) { (it as? Pressure)?.inMillimetersOfMercury ?: 0.0 },
            MetricSpec("systolic_max", BloodPressureRecord.SYSTOLIC_MAX) { (it as? Pressure)?.inMillimetersOfMercury ?: 0.0 },
            MetricSpec("systolic_min", BloodPressureRecord.SYSTOLIC_MIN) { (it as? Pressure)?.inMillimetersOfMercury ?: 0.0 },
        ),
    ),
    RecordSpec(
        type = "cycling_pedaling_cadence",
        metrics = listOf(
            MetricSpec("rpm_avg", CyclingPedalingCadenceRecord.RPM_AVG) { (it as? Number)?.toDouble() ?: 0.0 },
            MetricSpec("rpm_max", CyclingPedalingCadenceRecord.RPM_MAX) { (it as? Number)?.toDouble() ?: 0.0 },
            MetricSpec("rpm_min", CyclingPedalingCadenceRecord.RPM_MIN) { (it as? Number)?.toDouble() ?: 0.0 },
        ),
    ),
    RecordSpec(
        type = "distance",
        metrics = listOf(
            MetricSpec("distance_total", DistanceRecord.DISTANCE_TOTAL) { (it as? Length)?.inMeters ?: 0.0 },
        ),
    ),
    RecordSpec(
        type = "elevation_gained",
        metrics = listOf(
            MetricSpec("elevation_total", ElevationGainedRecord.ELEVATION_GAINED_TOTAL) { (it as? Length)?.inMeters ?: 0.0 },
        ),
    ),
    RecordSpec(
        type = "exercise_session",
        metrics = listOf(
            MetricSpec("time_total", ExerciseSessionRecord.EXERCISE_DURATION_TOTAL) { (it as? Duration)?.toMillis()?.toDouble() ?: 0.0 },
        ),
    ),
    RecordSpec(
        type = "floors_climbed",
        metrics = listOf(
            MetricSpec("floors_total", FloorsClimbedRecord.FLOORS_CLIMBED_TOTAL) { (it as? Number)?.toDouble() ?: 0.0 },
        ),
    ),
    RecordSpec(
        type = "heart_rate",
        metrics = listOf(
            MetricSpec("bpm_avg", HeartRateRecord.BPM_AVG) { (it as? Number)?.toDouble() ?: 0.0 },
            MetricSpec("bpm_max", HeartRateRecord.BPM_MAX) { (it as? Number)?.toDouble() ?: 0.0 },
            MetricSpec("bpm_min", HeartRateRecord.BPM_MIN) { (it as? Number)?.toDouble() ?: 0.0 },
            MetricSpec("count", HeartRateRecord.MEASUREMENTS_COUNT) { (it as? Number)?.toDouble() ?: 0.0 },
        ),
    ),
    RecordSpec(
        type = "height",
        metrics = listOf(
            MetricSpec("height_avg", HeightRecord.HEIGHT_AVG) { (it as? Length)?.inMeters ?: 0.0 },
            MetricSpec("height_max", HeightRecord.HEIGHT_MAX) { (it as? Length)?.inMeters ?: 0.0 },
            MetricSpec("height_min", HeightRecord.HEIGHT_MIN) { (it as? Length)?.inMeters ?: 0.0 },
        ),
    ),
    RecordSpec(
        type = "hydration",
        metrics = listOf(
            MetricSpec("volume_total", HydrationRecord.VOLUME_TOTAL) { (it as? Volume)?.inLiters ?: 0.0 },
        ),
    ),
    RecordSpec(
        type = "nutrition",
        metrics = listOf(
            MetricSpec("biotin_total", NutritionRecord.BIOTIN_TOTAL) { (it as? Mass)?.inGrams ?: 0.0 },
            MetricSpec("caffeine_total", NutritionRecord.CAFFEINE_TOTAL) { (it as? Mass)?.inGrams ?: 0.0 },
            MetricSpec("calcium_total", NutritionRecord.CALCIUM_TOTAL) { (it as? Mass)?.inGrams ?: 0.0 },
            MetricSpec("chloride_total", NutritionRecord.CHLORIDE_TOTAL) { (it as? Mass)?.inGrams ?: 0.0 },
            MetricSpec("cholesterol_total", NutritionRecord.CHOLESTEROL_TOTAL) { (it as? Mass)?.inGrams ?: 0.0 },
            MetricSpec("chromium_total", NutritionRecord.CHROMIUM_TOTAL) { (it as? Mass)?.inGrams ?: 0.0 },
            MetricSpec("copper_total", NutritionRecord.COPPER_TOTAL) { (it as? Mass)?.inGrams ?: 0.0 },
            MetricSpec("dietary_fiber_total", NutritionRecord.DIETARY_FIBER_TOTAL) { (it as? Mass)?.inGrams ?: 0.0 },
            MetricSpec("calories_from_fat_total", NutritionRecord.ENERGY_FROM_FAT_TOTAL) { (it as? Energy)?.inKilocalories ?: 0.0 },
            MetricSpec("calories_total", NutritionRecord.ENERGY_TOTAL) { (it as? Energy)?.inKilocalories ?: 0.0 },
            MetricSpec("folate_total", NutritionRecord.FOLATE_TOTAL) { (it as? Mass)?.inGrams ?: 0.0 },
            MetricSpec("folic_acid_total", NutritionRecord.FOLIC_ACID_TOTAL) { (it as? Mass)?.inGrams ?: 0.0 },
            MetricSpec("iodine_total", NutritionRecord.IODINE_TOTAL) { (it as? Mass)?.inGrams ?: 0.0 },
            MetricSpec("iron_total", NutritionRecord.IRON_TOTAL) { (it as? Mass)?.inGrams ?: 0.0 },
            MetricSpec("magnesium_total", NutritionRecord.MAGNESIUM_TOTAL) { (it as? Mass)?.inGrams ?: 0.0 },
            MetricSpec("manganese_total", NutritionRecord.MANGANESE_TOTAL) { (it as? Mass)?.inGrams ?: 0.0 },
            MetricSpec("molybdenum_total", NutritionRecord.MOLYBDENUM_TOTAL) { (it as? Mass)?.inGrams ?: 0.0 },
            MetricSpec("monounsaturated_fat_total", NutritionRecord.MONOUNSATURATED_FAT_TOTAL) { (it as? Mass)?.inGrams ?: 0.0 },
            MetricSpec("niacin_total", NutritionRecord.NIACIN_TOTAL) { (it as? Mass)?.inGrams ?: 0.0 },
            MetricSpec("pantothenic_acid_total", NutritionRecord.PANTOTHENIC_ACID_TOTAL) { (it as? Mass)?.inGrams ?: 0.0 },
            MetricSpec("phosphorus_total", NutritionRecord.PHOSPHORUS_TOTAL) { (it as? Mass)?.inGrams ?: 0.0 },
            MetricSpec("polyunsaturated_fat_total", NutritionRecord.POLYUNSATURATED_FAT_TOTAL) { (it as? Mass)?.inGrams ?: 0.0 },
            MetricSpec("potassium_total", NutritionRecord.POTASSIUM_TOTAL) { (it as? Mass)?.inGrams ?: 0.0 },
            MetricSpec("protein_total", NutritionRecord.PROTEIN_TOTAL) { (it as? Mass)?.inGrams ?: 0.0 },
            MetricSpec("riboflavin_total", NutritionRecord.RIBOFLAVIN_TOTAL) { (it as? Mass)?.inGrams ?: 0.0 },
            MetricSpec("saturated_fat_total", NutritionRecord.SATURATED_FAT_TOTAL) { (it as? Mass)?.inGrams ?: 0.0 },
            MetricSpec("selenium_total", NutritionRecord.SELENIUM_TOTAL) { (it as? Mass)?.inGrams ?: 0.0 },
            MetricSpec("sodium_total", NutritionRecord.SODIUM_TOTAL) { (it as? Mass)?.inGrams ?: 0.0 },
            MetricSpec("sugar_total", NutritionRecord.SUGAR_TOTAL) { (it as? Mass)?.inGrams ?: 0.0 },
            MetricSpec("thiamin_total", NutritionRecord.THIAMIN_TOTAL) { (it as? Mass)?.inGrams ?: 0.0 },
            MetricSpec("total_carbohydrate_total", NutritionRecord.TOTAL_CARBOHYDRATE_TOTAL) { (it as? Mass)?.inGrams ?: 0.0 },
            MetricSpec("total_fat_total", NutritionRecord.TOTAL_FAT_TOTAL) { (it as? Mass)?.inGrams ?: 0.0 },
            MetricSpec("trans_fat_total", NutritionRecord.TRANS_FAT_TOTAL) { (it as? Mass)?.inGrams ?: 0.0 },
            MetricSpec("unsaturated_fat_total", NutritionRecord.UNSATURATED_FAT_TOTAL) { (it as? Mass)?.inGrams ?: 0.0 },
            MetricSpec("vitamin_a_total", NutritionRecord.VITAMIN_A_TOTAL) { (it as? Mass)?.inGrams ?: 0.0 },
            MetricSpec("vitamin_b12_total", NutritionRecord.VITAMIN_B12_TOTAL) { (it as? Mass)?.inGrams ?: 0.0 },
            MetricSpec("vitamin_b6_total", NutritionRecord.VITAMIN_B6_TOTAL) { (it as? Mass)?.inGrams ?: 0.0 },
            MetricSpec("vitamin_c_total", NutritionRecord.VITAMIN_C_TOTAL) { (it as? Mass)?.inGrams ?: 0.0 },
            MetricSpec("vitamin_d_total", NutritionRecord.VITAMIN_D_TOTAL) { (it as? Mass)?.inGrams ?: 0.0 },
            MetricSpec("vitamin_e_total", NutritionRecord.VITAMIN_E_TOTAL) { (it as? Mass)?.inGrams ?: 0.0 },
            MetricSpec("vitamin_k_total", NutritionRecord.VITAMIN_K_TOTAL) { (it as? Mass)?.inGrams ?: 0.0 },
            MetricSpec("zinc_total", NutritionRecord.ZINC_TOTAL) { (it as? Mass)?.inGrams ?: 0.0 },
        ),
    ),
    RecordSpec(
        type = "power",
        metrics = listOf(
            MetricSpec("power_avg", PowerRecord.POWER_AVG) { (it as? Power)?.inWatts ?: 0.0 },
            MetricSpec("power_max", PowerRecord.POWER_MAX) { (it as? Power)?.inWatts ?: 0.0 },
            MetricSpec("power_min", PowerRecord.POWER_MIN) { (it as? Power)?.inWatts ?: 0.0 },
        ),
    ),
    RecordSpec(
        type = "resting_heart_rate",
        metrics = listOf(
            MetricSpec("bpm_avg", RestingHeartRateRecord.BPM_AVG) { (it as? Number)?.toDouble() ?: 0.0 },
            MetricSpec("bpm_max", RestingHeartRateRecord.BPM_MAX) { (it as? Number)?.toDouble() ?: 0.0 },
            MetricSpec("bpm_min", RestingHeartRateRecord.BPM_MIN) { (it as? Number)?.toDouble() ?: 0.0 },
        ),
    ),
    RecordSpec(
        type = "sleep_session",
        metrics = listOf(
            MetricSpec("duration", SleepSessionRecord.SLEEP_DURATION_TOTAL) { (it as? Duration)?.toMillis()?.toDouble() ?: 0.0 },
        ),
    ),
    RecordSpec(
        type = "speed",
        metrics = listOf(
            MetricSpec("speed_avg", SpeedRecord.SPEED_AVG) { (it as? Velocity)?.inMetersPerSecond ?: 0.0 },
            MetricSpec("speed_max", SpeedRecord.SPEED_MAX) { (it as? Velocity)?.inMetersPerSecond ?: 0.0 },
            MetricSpec("speed_min", SpeedRecord.SPEED_MIN) { (it as? Velocity)?.inMetersPerSecond ?: 0.0 },
        ),
    ),
    RecordSpec(
        type = "steps_cadence",
        metrics = listOf(
            MetricSpec("rate_avg", StepsCadenceRecord.RATE_AVG) { (it as? Number)?.toDouble() ?: 0.0 },
            MetricSpec("rate_max", StepsCadenceRecord.RATE_MAX) { (it as? Number)?.toDouble() ?: 0.0 },
            MetricSpec("rate_min", StepsCadenceRecord.RATE_MIN) { (it as? Number)?.toDouble() ?: 0.0 },
        ),
    ),
    RecordSpec(
        type = "steps",
        metrics = listOf(
            MetricSpec("count_total", StepsRecord.COUNT_TOTAL) { (it as? Number)?.toDouble() ?: 0.0 },
        ),
    ),
    RecordSpec(
        type = "total_calories_burned",
        metrics = listOf(
            MetricSpec("energy_total", TotalCaloriesBurnedRecord.ENERGY_TOTAL) { (it as? Energy)?.inKilocalories ?: 0.0 },
        ),
    ),
    RecordSpec(
        type = "weight",
        metrics = listOf(
            MetricSpec("weight_avg", WeightRecord.WEIGHT_AVG) { (it as? Mass)?.inKilograms ?: 0.0 },
            MetricSpec("weight_max", WeightRecord.WEIGHT_MAX) { (it as? Mass)?.inKilograms ?: 0.0 },
            MetricSpec("weight_min", WeightRecord.WEIGHT_MIN) { (it as? Mass)?.inKilograms ?: 0.0 },
        ),
    ),
    RecordSpec(
        type = "wheelchair_pushes",
        metrics = listOf(
            MetricSpec("count_total", WheelchairPushesRecord.COUNT_TOTAL) { (it as? Number)?.toDouble() ?: 0.0 },
        ),
    ),
)

/**
 * The record classes the registry reads.
 *
 * Every one of them needs its own read permission, and the permission is
 * requested with the class, so the list can never name a permission that
 * belongs to a type the app does not read.
 */
val readRecordClasses: List<KClass<out Record>> = listOf(
    ActiveCaloriesBurnedRecord::class,
    BasalBodyTemperatureRecord::class,
    BasalMetabolicRateRecord::class,
    BloodGlucoseRecord::class,
    BloodPressureRecord::class,
    BodyFatRecord::class,
    BodyTemperatureRecord::class,
    BodyWaterMassRecord::class,
    BoneMassRecord::class,
    CervicalMucusRecord::class,
    CyclingPedalingCadenceRecord::class,
    DistanceRecord::class,
    ElevationGainedRecord::class,
    ExerciseSessionRecord::class,
    FloorsClimbedRecord::class,
    HeartRateRecord::class,
    HeartRateVariabilityRmssdRecord::class,
    HeightRecord::class,
    HydrationRecord::class,
    IntermenstrualBleedingRecord::class,
    LeanBodyMassRecord::class,
    MenstruationFlowRecord::class,
    MenstruationPeriodRecord::class,
    NutritionRecord::class,
    OvulationTestRecord::class,
    OxygenSaturationRecord::class,
    PowerRecord::class,
    RespiratoryRateRecord::class,
    RestingHeartRateRecord::class,
    SexualActivityRecord::class,
    SleepSessionRecord::class,
    SpeedRecord::class,
    StepsCadenceRecord::class,
    StepsRecord::class,
    TotalCaloriesBurnedRecord::class,
    Vo2MaxRecord::class,
    WeightRecord::class,
    WheelchairPushesRecord::class,
)
/**
 * The record types that have no aggregate metrics.
 *
 * They are read as raw records, because the Health Connect API offers no
 * aggregate for them. A record type that holds no numeric value has no field
 * to read and is left out of the export.
 */
val rawRecordSpecs: List<RawRecordSpec> = listOf(
    RawRecordSpec(
        type = "basal_body_temperature",
        recordClass = BasalBodyTemperatureRecord::class,
        fields = listOf(
            RawFieldSpec("temperature", "temperature") { (it as? Temperature)?.inCelsius ?: 0.0 },
        ),
    ),
    RawRecordSpec(
        type = "blood_glucose",
        recordClass = BloodGlucoseRecord::class,
        fields = listOf(
            RawFieldSpec("level", "level") { (it as? BloodGlucose)?.inMillimolesPerLiter ?: 0.0 },
        ),
    ),
    RawRecordSpec(
        type = "body_fat",
        recordClass = BodyFatRecord::class,
        fields = listOf(
            RawFieldSpec("percentage", "percentage") { (it as? Percentage)?.value ?: 0.0 },
        ),
    ),
    RawRecordSpec(
        type = "body_temperature",
        recordClass = BodyTemperatureRecord::class,
        fields = listOf(
            RawFieldSpec("temperature", "temperature") { (it as? Temperature)?.inCelsius ?: 0.0 },
        ),
    ),
    RawRecordSpec(
        type = "body_water_mass",
        recordClass = BodyWaterMassRecord::class,
        fields = listOf(
            RawFieldSpec("mass", "mass") { (it as? Mass)?.inGrams ?: 0.0 },
        ),
    ),
    RawRecordSpec(
        type = "bone_mass",
        recordClass = BoneMassRecord::class,
        fields = listOf(
            RawFieldSpec("mass", "mass") { (it as? Mass)?.inGrams ?: 0.0 },
        ),
    ),
    RawRecordSpec(
        type = "heart_rate_variability_rmssd",
        recordClass = HeartRateVariabilityRmssdRecord::class,
        fields = listOf(
            RawFieldSpec("heart_rate_variability_millis", "heartRateVariabilityMillis") { (it as? Number)?.toDouble() ?: 0.0 },
        ),
    ),
    RawRecordSpec(
        type = "lean_body_mass",
        recordClass = LeanBodyMassRecord::class,
        fields = listOf(
            RawFieldSpec("mass", "mass") { (it as? Mass)?.inGrams ?: 0.0 },
        ),
    ),
    RawRecordSpec(
        type = "oxygen_saturation",
        recordClass = OxygenSaturationRecord::class,
        fields = listOf(
            RawFieldSpec("percentage", "percentage") { (it as? Percentage)?.value ?: 0.0 },
        ),
    ),
    RawRecordSpec(
        type = "respiratory_rate",
        recordClass = RespiratoryRateRecord::class,
        fields = listOf(
            RawFieldSpec("rate", "rate") { (it as? Number)?.toDouble() ?: 0.0 },
        ),
    ),
    RawRecordSpec(
        type = "vo2_max",
        recordClass = Vo2MaxRecord::class,
        fields = listOf(
            RawFieldSpec("vo2_milliliters_per_minute_kilogram", "vo2MillilitersPerMinuteKilogram") { (it as? Number)?.toDouble() ?: 0.0 },
        ),
    ),
)
