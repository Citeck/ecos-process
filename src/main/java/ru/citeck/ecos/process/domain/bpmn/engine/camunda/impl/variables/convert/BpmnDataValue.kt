package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.variables.convert

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.JsonNode
import com.jayway.jsonpath.JsonPath
import org.graalvm.polyglot.HostAccess.Export
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.util.function.BiConsumer

@Deprecated(
    "Class was moved to lib",
    ReplaceWith("BpmnDataValue", imports = ["ru.citeck.ecos.bpmn.commons.values.BpmnDataValue"])
)
@Suppress("DEPRECATION")
class BpmnDataValue private constructor(
    private val data: DataValue = DataValue.createObj()
) {

    companion object {

        @Export
        @JvmField
        val NULL = BpmnDataValue(DataValue.NULL)

        @Export
        @JvmField
        val TRUE = BpmnDataValue(DataValue.TRUE)

        @Export
        @JvmField
        val FALSE = BpmnDataValue(DataValue.FALSE)

        @Export
        @JvmStatic
        @JsonCreator
        fun create(content: Any?): BpmnDataValue {
            return BpmnDataValue(DataValue.create(content))
        }

        @Export
        @JvmStatic
        fun createObj(): BpmnDataValue {
            return BpmnDataValue(DataValue.createObj())
        }

        @Export
        @JvmStatic
        fun createArr(): BpmnDataValue {
            return BpmnDataValue(DataValue.createArr())
        }

        @Export
        @JvmStatic
        fun createStr(value: Any?): BpmnDataValue {
            return BpmnDataValue(DataValue.createStr(value))
        }
    }

    @Export
    fun remove(path: String): BpmnDataValue {
        data.remove(path)
        return this
    }

    @Export
    fun remove(idx: Int): BpmnDataValue {
        data.remove(idx)
        return this
    }

    @Export
    fun fieldNames(): Iterator<String> {
        return data.fieldNames()
    }

    @Export
    fun fieldNamesList(): List<String> {
        return data.fieldNamesList()
    }

    @Export
    fun has(fieldName: String): Boolean {
        return data.has(fieldName)
    }

    @Export
    fun has(index: Int): Boolean {
        return data.has(index)
    }

    @Export
    fun copy(): BpmnDataValue {
        return BpmnDataValue(data.copy())
    }

    @Export
    fun isObject() = data.isObject()

    @Export
    fun isValueNode() = data.isValueNode()

    @Export
    fun isTextual() = data.isTextual()

    @Export
    fun isBoolean() = data.isBoolean()

    @Export
    fun isNotNull() = !isNull()

    @Export
    fun isNull() = data.isNull()

    @Export
    fun isBinary() = data.isBinary()

    @Export
    fun isPojo() = data.isPojo()

    @Export
    fun isNumber() = data.isNumber()

    @Export
    fun isIntegralNumber() = data.isIntegralNumber()

    @Export
    fun isFloatingPointNumber() = data.isFloatingPointNumber()

    @Export
    fun isLong() = data.isLong()

    @Export
    fun isDouble() = data.isDouble()

    @Export
    fun isBigDecimal() = data.isBigDecimal()

    @Export
    fun isBigInteger() = data.isBigInteger()

    @Export
    fun isArray() = data.isArray()

    @Export
    fun renameKey(path: String, oldKey: String, newKey: String): BpmnDataValue {
        data.renameKey(path, oldKey, newKey)
        return this
    }

    // ====== set =======

    @Export
    fun set(path: String, key: String, value: Any?): BpmnDataValue {
        data.set(path, key, value)
        return this
    }

    @Export
    fun set(path: JsonPath, value: Any?): BpmnDataValue {
        data.set(path, value)
        return this
    }

    @Export
    operator fun set(path: String, value: Any?): BpmnDataValue {
        data.set(path, value)
        return this
    }

    @Export
    operator fun set(idx: Int, value: Any?): BpmnDataValue {
        data.set(idx, value)
        return this
    }

    // ====== /set =======
    // ===== set str =====

    @Export
    fun setStr(path: String, value: Any?): BpmnDataValue {
        data.setStr(path, value)
        return this
    }

    @Export
    fun setStr(idx: Int, value: Any?): BpmnDataValue {
        data.setStr(idx, value)
        return this
    }

    @Export
    fun setStr(path: JsonPath, value: Any?): BpmnDataValue {
        data.setStr(path, value)
        return this
    }

    @Export
    fun setStr(path: String, key: String, value: Any?): BpmnDataValue {
        data.setStr(path, key, value)
        return this
    }

    // ===== /set str =====
    // ======= get ========

    @Export
    operator fun get(idx: Int): BpmnDataValue {
        return BpmnDataValue(data.get(idx))
    }

    @Export
    fun <T : Any> get(field: String, type: Class<T>, orElse: T?): T? {
        return data.get(field, type, orElse)
    }

    @Export
    operator fun get(path: String): BpmnDataValue {
        return create(data.get(path))
    }

    @Export
    fun get(path: JsonPath): BpmnDataValue {
        return BpmnDataValue(data.get(path))
    }

    @Export
    fun takeFirst(path: String): BpmnDataValue {
        return BpmnDataValue(data.getFirst(path))
    }

    @Export
    fun takePaths(path: String): List<String> {
        return data.getPaths(path)
    }

    // ====== /get ======
    // ===== insert =====

    @Export
    fun insert(path: String, idx: Int, value: Any): BpmnDataValue {
        data.insert(path, idx, value)
        return this
    }

    @Export
    fun insert(idx: Int, value: Any?): BpmnDataValue {
        data.insert(idx, value)
        return this
    }

    @Export
    fun insertAll(path: String, idx: Int, values: Iterable<Any>): BpmnDataValue {
        data.insert(path, idx, values)
        return this
    }

    @Export
    fun insertAll(idx: Int, values: Iterable<Any>): BpmnDataValue {
        data.insert(idx, values)
        return this
    }

    // ===== /insert =====
    // ======= add =======

    @Export
    fun add(path: String, value: Any?): BpmnDataValue {
        data.add(path, value)
        return this
    }

    @Export
    fun add(value: Any?): BpmnDataValue {
        data.add(value)
        return this
    }

    @Export
    fun addAll(path: String, values: Iterable<Any>): BpmnDataValue {
        data.addAll(path, values)
        return this
    }

    @Export
    fun addAll(values: Iterable<Any>): BpmnDataValue {
        data.addAll(values)
        return this
    }

    // ======= /add =======

    @Export
    fun size() = data.size()

    @Export
    fun isEmpty() = data.isEmpty()

    @Export
    fun isNotEmpty() = data.isNotEmpty()

    @Export
    fun <T> mapKV(func: (String, DataValue) -> T): List<T> {
        return data.mapKV(func)
    }

    @Export
    fun <T> map(func: (DataValue) -> T): List<T> {
        return data.map(func)
    }

    @Export
    fun forEach(consumer: (String, DataValue) -> Unit) {
        data.forEach(consumer)
    }

    @Export
    fun forEachJ(consumer: BiConsumer<String, DataValue>) {
        data.forEach { k, v -> consumer.accept(k, v) }
    }

    @Export
    fun canConvertToInt(): Boolean {
        return data.canConvertToInt()
    }

    @Export
    fun canConvertToLong(): Boolean {
        return data.canConvertToLong()
    }

    @Export
    fun textValue(): String {
        return data.textValue()
    }

    @Export
    fun binaryValue(): ByteArray {
        return data.binaryValue()
    }

    @Export
    fun booleanValue(): Boolean {
        return data.booleanValue()
    }

    @Export
    fun numberValue(): Number {
        return data.numberValue()
    }

    @Export
    fun shortValue(): Short {
        return data.shortValue()
    }

    @Export
    fun intValue(): Int {
        return data.intValue()
    }

    @Export
    fun longValue(): Long {
        return data.longValue()
    }

    @Export
    fun floatValue(): Float {
        return data.floatValue()
    }

    @Export
    fun doubleValue(): Double {
        return data.doubleValue()
    }

    @Export
    fun decimalValue(): BigDecimal {
        return data.decimalValue()
    }

    @Export
    fun bigIntegerValue(): BigInteger {
        return data.bigIntegerValue()
    }

    @Export
    fun asText(): String {
        return data.asText()
    }

    @Export
    fun asText(defaultValue: String?): String? {
        return data.asText(defaultValue)
    }

    @Export
    fun asInt(): Int {
        return data.asInt()
    }

    @Export
    fun asInt(defaultValue: Int): Int {
        return data.asInt(defaultValue)
    }

    @Export
    fun asLong(): Long {
        return data.asLong()
    }

    @Export
    fun asLong(defaultValue: Long): Long {
        return data.asLong(defaultValue)
    }

    @Export
    fun asDouble(): Double {
        return data.asDouble()
    }

    @Export
    fun asDouble(defaultValue: Double): Double {
        return data.asDouble(defaultValue)
    }

    @Export
    fun asBoolean(): Boolean {
        return data.asBoolean()
    }

    @Export
    fun asBoolean(defaultValue: Boolean): Boolean {
        return data.asBoolean(defaultValue)
    }

    @Export
    fun takeAsInstant(): Instant? {
        return data.getAsInstant()
    }

    @Export
    fun takeAsInstantOrEpoch(): Instant {
        return data.getAsInstantOrEpoch()
    }

    @Export
    fun <T : Any> takeAs(type: Class<T>): T? {
        return data.getAs(type)
    }

    /**
     * @throws ru.citeck.ecos.commons.json.exception.JsonMapperException
     */
    @Export
    fun <T : Any> takeAsNotNull(type: Class<T>): T {
        return data.getAsNotNull(type)
    }

    @Export
    fun toStrList(): List<String> {
        return data.toStrList()
    }

    /**
     * Convert internal value and return a new mutable list.
     * If internal value is not an array-like object then list with single element will be returned.
     */
    @Export
    fun <T : Any> toList(elementType: Class<T>): MutableList<T> {
        return data.toList(elementType)
    }

    @Export
    fun asStrList(): MutableList<String> {
        return data.asStrList()
    }

    /**
     * Convert internal value and return a new mutable list.
     * If internal value is not an array-like object then empty list will be returned.
     */
    @Export
    fun <T : Any> asList(elementType: Class<T>): MutableList<T> {
        return data.asList(elementType)
    }

    @Export
    fun asDataValue(): DataValue {
        return data.copy()
    }

    /**
     * Convert internal value and return a new mutable map.
     * If internal value is not a map-like object then empty list will be returned.
     */
    @Export
    fun <K : Any, V : Any> asMap(keyType: Class<K>, valueType: Class<V>): MutableMap<K, V> {
        return data.asMap(keyType, valueType)
    }

    @Export
    fun asJavaObj(): Any? {
        return data.asJavaObj()
    }

    @Export
    fun asObjectData(): ObjectData {
        return data.asObjectData()
    }

    @Export
    @JsonValue
    fun asJson(): JsonNode {
        return data.asJson()
    }

    @Export
    fun asBinary(): ByteArray {
        return Json.mapper.toBytesNotNull(data)
    }

    @Export
    override fun toString(): String {
        return data.toString()
    }

    @Export
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (javaClass != other?.javaClass) {
            return false
        }

        other as BpmnDataValue

        return data == other.data
    }

    @Export
    override fun hashCode(): Int {
        return data.hashCode()
    }

    @Export
    fun asUnmodifiable(): BpmnDataValue {
        return BpmnDataValue(data.asUnmodifiable())
    }

    @Export
    fun isUnmodifiable(): Boolean {
        return data.isUnmodifiable()
    }
}
