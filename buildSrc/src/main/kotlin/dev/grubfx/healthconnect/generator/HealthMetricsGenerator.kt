package dev.grubfx.healthconnect.generator

import java.io.File
import java.util.zip.ZipFile

/**
 * The type of the value an aggregate metric holds.
 *
 * The Health Connect SDK types the aggregate metrics, so the generator reads
 * the unit straight from the declaration instead of guessing it.
 */
sealed interface MetricValueType {
    /** A unit that the SDK models as its own class, such as `Mass` or `Pressure`. */
    data class Unit(val unitClass: String, val unit: String) : MetricValueType

    /** A plain number. */
    data object Plain : MetricValueType

    /** A duration, which the SDK models as `java.time.Duration`. */
    data object Duration : MetricValueType

    /** A percentage, which the SDK models as `Percentage`. */
    data object Percentage : MetricValueType
}

/** One aggregate metric of a record type. */
data class AggregateSpec(
    /** The name of the constant in the companion object of the record. */
    val constant: String,
    /** The name the metric is written to, taken from the declaration of the SDK. */
    val fieldName: String,
    /** The kind of the aggregation, such as `avg` or `total`. */
    val aggregation: String,
    /** The type of the value the aggregate holds. */
    val valueType: MetricValueType,
)

/**
 * One numeric field of a record that has no aggregate metrics.
 *
 * The record types without an aggregate hold their values in plain fields,
 * which have to be read one record at a time.
 */
data class RawFieldSpec(
    /** The name of the field in the record, such as `level`. */
    val fieldName: String,
    /** The type of the value the field holds. */
    val valueType: MetricValueType,
)

/** A record type of the Health Connect SDK. */
data class RecordSpec(
    /** The simple name of the record class, such as `BloodPressureRecord`. */
    val simpleName: String,
    /** The aggregate metrics the record supports. Empty for the records that have none. */
    val aggregates: List<AggregateSpec>,
    /** The numeric fields of a record that has no aggregate metrics. */
    val rawFields: List<RawFieldSpec> = emptyList(),
) {
    /** The name of the measurement the record is written to. */
    val measurementName: String
        get() = simpleName.removeSuffix("Record")
            .replace(Regex("([a-z0-9])([A-Z])"), "$1_$2")
            .lowercase()
}

/**
 * Writes the metric registry of the app from the sources of the Health
 * Connect SDK.
 *
 * The SDK does not ship a schema file, but every record class declares its
 * aggregate metrics, the name of the field they aggregate and the unit they
 * are converted to. The generator reads those declarations out of the sources
 * of the SDK, so the registry can never name a metric that the SDK does not
 * have.
 *
 * The sources are used instead of the compiled classes, because the sources
 * name the field and the unit of every metric in a way that can be read
 * directly, while the compiled classes hide them behind the generic signature
 * of the field and the key of the metric.
 *
 * The name of a metric follows the scheme of the SDK itself. The SDK builds
 * the key of a metric from the name of the data type, the name of the field
 * and the kind of the aggregation, and drops the field when the metric has
 * none. The generator uses the same scheme, so the names of the registry
 * match the keys of the SDK.
 */
object HealthMetricsGenerator {

    private const val RECORDS_PACKAGE = "androidx/health/connect/client/records"
    private const val UNITS_PACKAGE = "androidx/health/connect/client/units"

    /**
     * The records that need a feature flag and are therefore skipped.
     *
     * The feature flags of Health Connect are only available to a few apps,
     * so the records behind them are left out of the registry.
     */
    private val experimentalRecords: Set<String> = setOf(
        "ActivityIntensityRecord",
        "MindfulnessSessionRecord",
        "PlannedExerciseSessionRecord",
        "SkinTemperatureRecord",
    )

    /**
     * The base classes of the records.
     *
     * They describe the shape of a record, but they hold no data of their
     * own, so they are no data types of Health Connect.
     */
    private val baseRecords: Set<String> = setOf(
        "Record",
        "IntervalRecord",
        "InstantaneousRecord",
        "SeriesRecord",
    )

    /**
     * Builds the registry from the sources of the SDK.
     *
     * @param sourcesJar the sources jar of the Health Connect SDK.
     */
    fun generate(sourcesJar: File): String {
        val records = readRecords(sourcesJar)
        check(records.isNotEmpty()) {
            "The Health Connect SDK was not found in ${sourcesJar.path}."
        }

        val unitAccessors = readUnitAccessors(sourcesJar)
        val withAggregates = records.filter { it.aggregates.isNotEmpty() }
        val withoutAggregates = records.filter { it.aggregates.isEmpty() }

        return buildString {
            appendLine("// This file is generated. Do not edit it.")
            appendLine("// Run `./gradlew generateMetrics` to regenerate it from the Health Connect SDK.")
            appendLine()
            appendLine("package dev.grubfx.healthconnect")
            appendLine()
            appendLine("import androidx.health.connect.client.aggregate.AggregateMetric")
            records.forEach { record ->
                appendLine("import androidx.health.connect.client.records.${record.simpleName}")
            }
            // The list of the record classes holds the base type, and the
            // classes are held as Kotlin classes.
            appendLine("import androidx.health.connect.client.records.Record")
            appendLine("import kotlin.reflect.KClass")

            // The units of the metrics are read in the generated code, so the
            // classes that hold them have to be imported.
            val units = records
                .flatMap { record ->
                    record.aggregates.map { it.valueType } + record.rawFields.map { it.valueType }
                }
                .mapNotNull { type ->
                    (type as? MetricValueType.Unit)?.unitClass
                }
                .distinct()
                .sorted()
            units.forEach { unit ->
                appendLine("import androidx.health.connect.client.units.$unit")
            }
            // The percentage is a unit of its own in the SDK, so it is
            // imported like every other unit.
            if (records.any { record ->
                    record.aggregates.any { it.valueType is MetricValueType.Percentage } ||
                        record.rawFields.any { it.valueType is MetricValueType.Percentage }
                }
            ) {
                appendLine("import androidx.health.connect.client.units.Percentage")
            }
            if (records.any { record ->
                    record.aggregates.any { it.valueType is MetricValueType.Duration }
                }
            ) {
                appendLine("import java.time.Duration")
            }
            appendLine()
            appendLine("/**")
            appendLine(" * The aggregate metrics of the record types that support them.")
            appendLine(" *")
            appendLine(" * Every metric can be requested in a single aggregate call. The Health Connect API")
            appendLine(" * has a daily limit on the number of calls, and the limit in the background is")
            appendLine(" * stricter than the one in the foreground, so one call for all metrics is the only")
            appendLine(" * way to stay inside the quota.")
            appendLine(" */")
            appendLine("val aggregateRecordSpecs: List<RecordSpec> = listOf(")
            withAggregates.forEach { record ->
                appendLine("    RecordSpec(")
                appendLine("        type = \"${record.measurementName}\",")
                appendLine("        metrics = listOf(")
                record.aggregates.forEach { aggregate ->
                    val metric = "${record.simpleName}.${aggregate.constant}"
                    appendLine(
                        "            MetricSpec(\"${aggregate.metricName()}\", $metric) { ${
                            aggregate.readExpression(unitAccessors)
                        } },",
                    )
                }
                appendLine("        ),")
                appendLine("    ),")
            }
            appendLine(")")
            appendLine()
            appendLine("/**")
            appendLine(" * The record classes the registry reads.")
            appendLine(" *")
            appendLine(" * Every one of them needs its own read permission, and the permission is")
            appendLine(" * requested with the class, so the list can never name a permission that")
            appendLine(" * belongs to a type the app does not read.")
            appendLine(" */")
            appendLine("val readRecordClasses: List<KClass<out Record>> = listOf(")
            records.forEach { record ->
                appendLine("    ${record.simpleName}::class,")
            }
            appendLine(")")
            appendLine("/**")
            appendLine(" * The record types that have no aggregate metrics.")
            appendLine(" *")
            appendLine(" * They are read as raw records, because the Health Connect API offers no")
            appendLine(" * aggregate for them. A record type that holds no numeric value has no field")
            appendLine(" * to read and is left out of the export.")
            appendLine(" */")
            appendLine("val rawRecordSpecs: List<RawRecordSpec> = listOf(")
            withoutAggregates.filter { it.rawFields.isNotEmpty() }.forEach { record ->
                appendLine("    RawRecordSpec(")
                appendLine("        type = \"${record.measurementName}\",")
                appendLine("        recordClass = ${record.simpleName}::class,")
                appendLine("        fields = listOf(")
                record.rawFields.forEach { field ->
                    appendLine(
                        "            RawFieldSpec(\"${field.fieldName.toSnakeCase()}\", \"${field.fieldName}\") { ${
                            field.readExpression(unitAccessors)
                        } },",
                    )
                }
                appendLine("        ),")
                appendLine("    ),")
            }
            appendLine(")")
        }
    }

    /** Reads the record classes and their aggregate metrics out of the sources. */
    private fun readRecords(sourcesJar: File): List<RecordSpec> =
        ZipFile(sourcesJar).use { zip ->
            zip.entries().asSequence()
                .filter { it.name.startsWith("$RECORDS_PACKAGE/") && it.name.endsWith("Record.kt") }
                .mapNotNull { entry ->
                    val simpleName = entry.name.removePrefix("$RECORDS_PACKAGE/")
                        .removeSuffix(".kt")
                    if (simpleName in experimentalRecords || simpleName in baseRecords) {
                        null
                    } else {
                        val source = zip.getInputStream(entry).bufferedReader().readText()
                        val aggregates = readAggregates(source)
                        RecordSpec(
                            simpleName = simpleName,
                            aggregates = aggregates,
                            rawFields = if (aggregates.isEmpty()) readRawFields(source) else emptyList(),
                        )
                    }
                }
                .sortedBy { it.simpleName }
                .toList()
        }

    /**
     * Reads the numeric fields of a record that has no aggregate metrics.
     *
     * A record holds its values in plain fields. Only the fields with a
     * number or with a unit are read, the fields that hold an enumeration
     * carry no measurement.
     */
    private fun readRawFields(source: String): List<RawFieldSpec> =
        rawField.findAll(source)
            .mapNotNull { match ->
                val valueType = readFieldValueType(match.groupValues[2]) ?: return@mapNotNull null
                RawFieldSpec(
                    fieldName = match.groupValues[1],
                    valueType = valueType,
                )
            }
            .distinctBy { it.fieldName }
            .toList()

    /**
     * Reads the type of the value a field holds.
     *
     * A field of a unit is read in the unit the SDK uses for the metric of
     * that type. A field of a plain number is read as it is. A field that
     * holds an enumeration carries no measurement and is left out.
     */
    private fun readFieldValueType(type: String): MetricValueType? =
        when (type) {
            "Double", "Float" -> MetricValueType.Plain
            "Percentage" -> MetricValueType.Percentage
            // The units of the SDK. Every other type is an enumeration or a
            // class that holds no measurement.
            "BloodGlucose", "Energy", "Length", "Mass", "Percentage", "Power",
            "Pressure", "Temperature", "TemperatureDelta", "Velocity", "Volume",
            -> MetricValueType.Unit(unitClass = type, unit = "")
            else -> null
        }

    /**
     * Reads the accessor of every unit class.
     *
     * A unit holds its value in several units, such as grams and kilograms.
     * The accessor is the property that reads the value in the unit the SDK
     * itself uses for the metric.
     */
    private fun readUnitAccessors(sourcesJar: File): Map<String, Set<String>> =
        ZipFile(sourcesJar).use { zip ->
            zip.entries().asSequence()
                .filter { it.name.startsWith("$UNITS_PACKAGE/") && it.name.endsWith(".kt") }
                .mapNotNull { entry ->
                    val unitClass = entry.name.removePrefix("$UNITS_PACKAGE/")
                        .removeSuffix(".kt")
                    val source = zip.getInputStream(entry).bufferedReader().readText()

                    // The accessors of the unit are the properties that start
                    // with "in", such as inGrams. A unit without such a
                    // property holds its value directly.
                    val accessors = unitAccessor.findAll(source)
                        .map { it.groupValues[1] }
                        .toSet()
                    unitClass to accessors
                }
                .toMap()
        }

    /** Reads the aggregate metrics a record declares in its companion object. */
    private fun readAggregates(source: String): List<AggregateSpec> {
        // The constants of the record hold the name of the field that is
        // aggregated, so they are read before the declarations are parsed.
        val fieldNames = fieldNameConstants(source)

        val specs = mutableListOf<AggregateSpec>()
        val lines = source.lines()
        var index = 0

        while (index < lines.size) {
            val match = declaration.find(lines[index])
            if (match == null) {
                index++
                continue
            }

            // The call that creates the metric runs over several lines, so it
            // is read until the parenthesis is closed.
            val body = StringBuilder()
            var depth = 0
            while (index < lines.size) {
                val line = lines[index]
                body.appendLine(line)
                depth += line.count { it == '(' } - line.count { it == ')' }
                index++
                if (depth <= 0 && body.contains("(")) break
            }

            val text = body.toString()
            specs.add(
                AggregateSpec(
                    constant = match.groupValues[1],
                    fieldName = readFieldName(text, fieldNames),
                    aggregation = readAggregation(text),
                    valueType = readValueType(match.groupValues[2], text),
                ),
            )
        }

        return specs.sortedBy { it.constant }
    }

    /** Reads the constants of the record that hold the name of a field. */
    private fun fieldNameConstants(source: String): Map<String, String> =
        constant.findAll(source)
            .map { it.groupValues[1] to it.groupValues[2] }
            .toMap()

    /**
     * Reads the name of the field the metric aggregates.
     *
     * The name is either a string literal, a constant that is passed by name
     * or the last argument of the call. The constants of the record are looked
     * up in the map that was read before.
     *
     * A metric without a field, such as the count of the samples of a type,
     * has an empty name. The SDK drops the field from the key of such a
     * metric.
     */
    private fun readFieldName(body: String, fieldNames: Map<String, String>): String {
        // A metric that counts the records of a type has no field at all.
        if (countMetric.find(body) != null) return ""

        // A metric of a duration has no field when it measures the whole type.
        if (durationMetric.find(body) != null && arguments(body).size == 1) return ""

        // The field is passed by name. The name of the data type is passed by
        // name as well, so the argument of the field has to be picked out
        // instead of the first string of the call.
        val namedLiteral = namedFieldName.find(body)?.groupValues?.get(1)
        if (namedLiteral != null) return namedLiteral

        val named = fieldNameArgument.find(body)?.groupValues?.get(1)
        if (named != null) return resolveFieldName(named, fieldNames)

        // The field is passed by position. The name of the type comes first,
        // then the kind of the aggregation, then the name of the field.
        val positional = arguments(body).getOrNull(2)
        if (positional != null) {
            if (positional.startsWith("\"") && positional.endsWith("\"")) {
                return positional.trim('"')
            }
            if (constantName.matches(positional)) {
                return resolveFieldName(positional, fieldNames)
            }
        }

        // A field that is passed by position as the only name in the call is
        // the name of the field itself.
        val literal = stringLiteral.find(body)?.groupValues?.get(1)
        if (literal != null) return literal

        error("The field name of the metric could not be read from: $body")
    }

    /** Resolves the name of a field from the constant that holds it. */
    private fun resolveFieldName(constant: String, fieldNames: Map<String, String>): String =
        fieldNames[constant]
            ?: constant.removeSuffix("_FIELD_NAME").removeSuffix("_FIELD").lowercase()

    /**
     * Reads the kind of the aggregation.
     *
     * The names are the ones the SDK itself uses for the key of a metric,
     * such as `avg` for an average and `total` for a total.
     */
    private fun readAggregation(body: String): String {
        // A metric that counts the records of a type always counts.
        if (countMetric.find(body) != null) return "count"

        val named = aggregationArgument.find(body)?.groupValues?.get(1)
        if (named != null) return aggregationNames.getValue(named.lowercase())

        // The kind of the aggregation is the second argument of the call,
        // when it is passed by position.
        val positional = arguments(body).getOrNull(1)
        if (positional != null) {
            val name = positional.substringAfterLast(".")
            if (constantName.matches(name)) {
                return aggregationNames.getValue(name.lowercase())
            }
        }

        // A metric of a duration without a field measures the whole duration
        // of the type.
        if (durationMetric.find(body) != null) return "duration"

        error("The kind of the aggregation could not be read from: $body")
    }

    /** Splits the arguments of the call that creates the metric. */
    private fun arguments(body: String): List<String> {
        val call = callBody.find(body)?.groupValues?.get(1) ?: return emptyList()
        return call.split(",").map { it.trim().substringAfter("=").trim() }
    }

    /** Reads the type of the value the metric holds. */
    private fun readValueType(type: String, body: String): MetricValueType =
        when (type) {
            "Long", "Double" -> MetricValueType.Plain
            "Duration" -> MetricValueType.Duration
            else -> {
                // The unit is converted by a mapper of the SDK. The mapper
                // names the unit the result holds, so the value is read with
                // the same unit the SDK uses.
                val mapper = unitMapper.find(body)
                    ?: error("The unit of the metric could not be read from: $body")
                MetricValueType.Unit(
                    unitClass = mapper.groupValues[1],
                    unit = mapper.groupValues[2],
                )
            }
        }

    /**
     * Matches the declaration of an aggregate metric.
     */
    private val declaration = Regex(
        """val\s+([A-Z][A-Z0-9_]+)\s*:\s*AggregateMetric<([A-Za-z.]+)>\s*=""",
    )

    /** Matches a constant that holds a string. */
    private val constant = Regex("""const\s+val\s+([A-Z][A-Z0-9_]*)\s*=\s*["']([a-zA-Z]+)["']""")

    /** Matches a string literal. */
    private val stringLiteral = Regex("""["']([a-zA-Z][a-zA-Z0-9]*)["']""")

    /** Matches the constant that holds the name of a field. */
    /** Matches the field name that is passed as a string. */
    private val namedFieldName = Regex("""fieldName\s*=\s*["']([a-zA-Z][a-zA-Z0-9]*)["']""")

    /** Matches the constant that holds the name of a field. */
    private val fieldNameArgument = Regex("""fieldName\s*=\s*([A-Z][A-Z0-9_]*)""")

    /** Matches the kind of the aggregation that is passed by name. */
    private val aggregationArgument = Regex("""aggregationType\s*=\s*[A-Za-z.]*\.([A-Z]+)""")

    /** Matches the call that counts the records of a type. */
    private val countMetric = Regex("""countMetric\s*\(""")

    /** Matches the call that measures the duration of a whole type. */
    private val durationMetric = Regex("""durationMetric\s*\(""")

    /** Matches the mapper that converts the value of a metric into its unit. */
    private val unitMapper = Regex("""(?:mapper\s*=\s*|,\s*)([A-Z][A-Za-z]*)::([a-zA-Z]+)""")

    /** Matches the accessor of a unit class. */
    private val unitAccessor = Regex("""\bval\s+(in[A-Za-z]+):\s*Double""")

    /**
     * Matches a numeric field of a record.
     *
     * The fields of a record are declared as public properties, so the type
     * of the field is read from the declaration.
     */
    private val rawField = Regex("""public\s+val\s+([a-z][A-Za-z0-9]*)\s*:\s*([A-Z][A-Za-z.]*)""")

    /** Matches the arguments of the call that creates the metric. */
    private val callBody = Regex(
        """(?:Metric\.)?\w+\(\s*([^()]*?)\s*\)""",
        RegexOption.DOT_MATCHES_ALL,
    )

    /** Matches the name of a constant. */
    private val constantName = Regex("""[A-Z][A-Z0-9_]*""")

    /**
     * The names the SDK uses for the kinds of aggregation.
     *
     * The SDK builds the key of a metric from these names, so the registry
     * uses them as well.
     */
    private val aggregationNames: Map<String, String> = mapOf(
        "average" to "avg",
        "avg" to "avg",
        "minimum" to "min",
        "min" to "min",
        "maximum" to "max",
        "max" to "max",
        "total" to "total",
        "count" to "count",
        "duration" to "duration",
    )

    /**
     * The name of a metric.
     *
     * The name follows the scheme of the SDK: the name of the field and the
     * kind of the aggregation. A metric without a field drops it from the
     * name.
     */
    private fun AggregateSpec.metricName(): String {
        val parts = listOfNotNull(
            fieldName.takeIf { it.isNotEmpty() },
            aggregation,
        )
        return parts.joinToString("_").toSnakeCase()
    }

    /** Turns a camel case name into the snake case the server expects. */
    private fun String.toSnakeCase(): String =
        replace(Regex("([a-z0-9])([A-Z])"), "$1_$2").lowercase()

    /** The expression that reads the value of the metric as a number. */
    private fun AggregateSpec.readExpression(unitAccessors: Map<String, Set<String>>): String =
        when (val type = valueType) {
            is MetricValueType.Plain -> "(it as? Number)?.toDouble() ?: 0.0"
            is MetricValueType.Duration -> "(it as? Duration)?.toMillis()?.toDouble() ?: 0.0"
            is MetricValueType.Unit -> {
                val accessors = unitAccessors[type.unitClass]
                    ?: error("The unit ${type.unitClass} is unknown.")
                // The mapper of the SDK names the unit the result holds, such
                // as grams. The property that reads the value in that unit is
                // the one that starts with "in", such as inGrams.
                val accessor = accessors.firstOrNull { it.equals("in${type.unit.capitalize()}", ignoreCase = true) }
                    ?: accessors.firstOrNull { it.equals("in${type.unit.capitalize()}s", ignoreCase = true) }
                    ?: error("The unit ${type.unitClass} has no accessor for ${type.unit}.")
                "(it as? ${type.unitClass})?.$accessor ?: 0.0"
            }
            is MetricValueType.Percentage -> "(it as? Percentage)?.value ?: 0.0"
        }

    /**
     * The expression that reads the value of a raw field as a number.
     *
     * A field of a unit is read in the unit the SDK uses for the metric of
     * that type. The unit is picked as the one the SDK converts the metric
     * to, so the value is the same the aggregate of the type would hold.
     */
    private fun RawFieldSpec.readExpression(unitAccessors: Map<String, Set<String>>): String =
        when (val type = valueType) {
            is MetricValueType.Plain -> "(it as? Number)?.toDouble() ?: 0.0"
            is MetricValueType.Percentage -> "(it as? Percentage)?.value ?: 0.0"
            is MetricValueType.Unit -> {
                val accessors = unitAccessors[type.unitClass]
                    ?: error("The unit ${type.unitClass} is unknown.")
                // A unit holds its value in several units. The first accessor
                // is the one the SDK converts its metrics to, so the raw value
                // is read in the same unit the aggregate of the type holds.
                val accessor = accessors.firstOrNull()
                    ?: error("The unit ${type.unitClass} has no accessor.")
                "(it as? ${type.unitClass})?.$accessor ?: 0.0"
            }
            is MetricValueType.Duration -> "(it as? Duration)?.toMillis()?.toDouble() ?: 0.0"
        }
}
