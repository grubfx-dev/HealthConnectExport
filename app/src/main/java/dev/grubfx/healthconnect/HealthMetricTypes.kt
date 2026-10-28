package dev.grubfx.healthconnect

import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.records.Record
import kotlin.reflect.KClass

/**
 * One aggregate metric of a record type.
 *
 * @param name the name the metric is written to.
 * @param metric the metric of the Health Connect SDK.
 * @param read reads the value of the metric as a number, in the unit the SDK
 *   uses for the metric.
 */
data class MetricSpec(
    val name: String,
    val metric: AggregateMetric<*>,
    val read: (Any?) -> Double,
)

/**
 * A record type with the aggregate metrics it supports.
 *
 * @param type the name of the measurement the record is written to.
 * @param metrics the aggregate metrics of the record.
 */
data class RecordSpec(
    val type: String,
    val metrics: List<MetricSpec>,
)

/**
 * One numeric field of a record that has no aggregate metrics.
 *
 * @param name the name the field is written to.
 * @param property the name of the property in the record, used to read the
 *   value out of the record.
 * @param read reads the value of the field as a number, in the unit the SDK
 *   uses for the type.
 */
data class RawFieldSpec(
    val name: String,
    val property: String,
    val read: (Any?) -> Double,
)

/**
 * A record type that has no aggregate metrics.
 *
 * Such a record is read as raw records, because the Health Connect API offers
 * no aggregate for it.
 *
 * @param type the name of the measurement the record is written to.
 * @param recordClass the class of the record.
 * @param fields the numeric fields of the record.
 */
data class RawRecordSpec(
    val type: String,
    val recordClass: KClass<out Record>,
    val fields: List<RawFieldSpec>,
)
