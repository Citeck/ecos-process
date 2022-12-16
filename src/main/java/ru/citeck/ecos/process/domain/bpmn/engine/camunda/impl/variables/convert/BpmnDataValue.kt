package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.variables.convert

import ecos.com.fasterxml.jackson210.annotation.JsonCreator
import ecos.com.fasterxml.jackson210.annotation.JsonValue
import ecos.com.fasterxml.jackson210.databind.JsonNode
import ecos.com.jayway.jsonpath.JsonPath
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.util.function.BiConsumer

class BpmnDataValue private constructor(
    private val data: DataValue = DataValue.createObj()
) {

    companion object {

        @JvmField
        val NULL = BpmnDataValue(DataValue.NULL)

        @JvmField
        val TRUE = BpmnDataValue(DataValue.TRUE)

        @JvmField
        val FALSE = BpmnDataValue(DataValue.FALSE)

        @JvmStatic
        @JsonCreator
        @com.fasterxml.jackson.annotation.JsonCreator
        fun create(content: Any?): BpmnDataValue {
            return BpmnDataValue(DataValue.create(content))
        }

        @JvmStatic
        fun createObj(): BpmnDataValue {
            return BpmnDataValue(DataValue.createObj())
        }

        @JvmStatic
        fun createArr(): BpmnDataValue {
            return BpmnDataValue(DataValue.createArr())
        }

        @JvmStatic
        fun createStr(value: Any?): BpmnDataValue {
            return BpmnDataValue(DataValue.createStr(value))
        }
    }

    fun remove(path: String): BpmnDataValue {
        data.remove(path)
        return this
    }

    fun remove(idx: Int): BpmnDataValue {
        data.remove(idx)
        return this
    }

    fun fieldNames(): Iterator<String> {
        return data.fieldNames()
    }

    fun fieldNamesList(): List<String> {
        return data.fieldNamesList()
    }

    fun has(fieldName: String): Boolean {
        return data.has(fieldName)
    }

    fun has(index: Int): Boolean {
        return data.has(index)
    }

    fun copy(): BpmnDataValue {
        return BpmnDataValue(data.copy())
    }

    fun isObject() = data.isObject()

    fun isValueNode() = data.isValueNode()

    fun isTextual() = data.isTextual()

    fun isBoolean() = data.isBoolean()

    fun isNotNull() = !isNull()

    fun isNull() = data.isNull()

    fun isBinary() = data.isBinary()

    fun isPojo() = data.isPojo()

    fun isNumber() = data.isNumber()

    fun isIntegralNumber() = data.isIntegralNumber()

    fun isFloatingPointNumber() = data.isFloatingPointNumber()

    fun isShort() = data.isShort()

    fun isInt() = data.isInt()

    fun isLong() = data.isLong()

    fun isFloat() = data.isFloat()

    fun isDouble() = data.isDouble()

    fun isBigDecimal() = data.isBigDecimal()

    fun isBigInteger() = data.isBigInteger()

    fun isArray() = data.isArray()

    fun renameKey(path: String, oldKey: String, newKey: String): BpmnDataValue {
        data.renameKey(path, oldKey, newKey)
        return this
    }

    // ====== set =======

    fun set(path: String, key: String, value: Any?): BpmnDataValue {
        data.set(path, key, value)
        return this
    }

    fun set(path: JsonPath, value: Any?): BpmnDataValue {
        data.set(path, value)
        return this
    }

    operator fun set(path: String, value: Any?): BpmnDataValue {
        data.set(path, value)
        return this
    }

    operator fun set(idx: Int, value: Any?): BpmnDataValue {
        data.set(idx, value)
        return this
    }

    // ====== /set =======
    // ===== set str =====

    fun setStr(path: String, value: Any?): BpmnDataValue {
        data.setStr(path, value)
        return this
    }

    fun setStr(idx: Int, value: Any?): BpmnDataValue {
        data.setStr(idx, value)
        return this
    }

    fun setStr(path: JsonPath, value: Any?): BpmnDataValue {
        data.setStr(path, value)
        return this
    }

    fun setStr(path: String, key: String, value: Any?): BpmnDataValue {
        data.setStr(path, key, value)
        return this
    }

    // ===== /set str =====
    // ======= get ========

    operator fun get(idx: Int): BpmnDataValue {
        return BpmnDataValue(data.get(idx))
    }

    fun <T : Any> get(field: String, type: Class<T>, orElse: T?): T? {
        return data.get(field, type, orElse)
    }

    operator fun get(path: String): BpmnDataValue {
        return create(data.get(path))
    }

    fun get(path: JsonPath): BpmnDataValue {
        return BpmnDataValue(data.get(path))
    }

    fun takeFirst(path: String): BpmnDataValue {
        return BpmnDataValue(data.getFirst(path))
    }

    fun takePaths(path: String): List<String> {
        return data.getPaths(path)
    }

    // ====== /get ======
    // ===== insert =====

    fun insert(path: String, idx: Int, value: Any): BpmnDataValue {
        data.insert(path, idx, value)
        return this
    }

    fun insert(idx: Int, value: Any?): BpmnDataValue {
        data.insert(idx, value)
        return this
    }

    fun insertAll(path: String, idx: Int, values: Iterable<Any>): BpmnDataValue {
        data.insert(path, idx, values)
        return this
    }

    fun insertAll(idx: Int, values: Iterable<Any>): BpmnDataValue {
        data.insert(idx, values)
        return this
    }

    // ===== /insert =====
    // ======= add =======

    fun add(path: String, value: Any?): BpmnDataValue {
        data.add(path, value)
        return this
    }

    fun add(value: Any?): BpmnDataValue {
        data.add(value)
        return this
    }

    fun addAll(path: String, values: Iterable<Any>): BpmnDataValue {
        data.addAll(path, values)
        return this
    }

    fun addAll(values: Iterable<Any>): BpmnDataValue {
        data.addAll(values)
        return this
    }

    // ======= /add =======

    fun size() = data.size()

    fun isEmpty() = data.isEmpty()

    fun isNotEmpty() = data.isNotEmpty()

    fun <T> mapKV(func: (String, DataValue) -> T): List<T> {
        return data.mapKV(func)
    }

    fun <T> map(func: (DataValue) -> T): List<T> {
        return data.map(func)
    }

    fun forEach(consumer: (String, DataValue) -> Unit) {
        data.forEach(consumer)
    }

    fun forEachJ(consumer: BiConsumer<String, DataValue>) {
        data.forEach { k, v -> consumer.accept(k, v) }
    }

    fun canConvertToInt(): Boolean {
        return data.canConvertToInt()
    }

    fun canConvertToLong(): Boolean {
        return data.canConvertToLong()
    }

    fun textValue(): String {
        return data.textValue()
    }

    fun binaryValue(): ByteArray {
        return data.binaryValue()
    }

    fun booleanValue(): Boolean {
        return data.booleanValue()
    }

    fun numberValue(): Number {
        return data.numberValue()
    }

    fun shortValue(): Short {
        return data.shortValue()
    }

    fun intValue(): Int {
        return data.intValue()
    }

    fun longValue(): Long {
        return data.longValue()
    }

    fun floatValue(): Float {
        return data.floatValue()
    }

    fun doubleValue(): Double {
        return data.doubleValue()
    }

    fun decimalValue(): BigDecimal {
        return data.decimalValue()
    }

    fun bigIntegerValue(): BigInteger {
        return data.bigIntegerValue()
    }

    fun asText(): String {
        return data.asText()
    }

    fun asText(defaultValue: String?): String? {
        return data.asText(defaultValue)
    }

    fun asInt(): Int {
        return data.asInt()
    }

    fun asInt(defaultValue: Int): Int {
        return data.asInt(defaultValue)
    }

    fun asLong(): Long {
        return data.asLong()
    }

    fun asLong(defaultValue: Long): Long {
        return data.asLong(defaultValue)
    }

    fun asDouble(): Double {
        return data.asDouble()
    }

    fun asDouble(defaultValue: Double): Double {
        return data.asDouble(defaultValue)
    }

    fun asBoolean(): Boolean {
        return data.asBoolean()
    }

    fun asBoolean(defaultValue: Boolean): Boolean {
        return data.asBoolean(defaultValue)
    }

    fun takeAsInstant(): Instant? {
        return data.getAsInstant()
    }

    fun takeAsInstantOrEpoch(): Instant {
        return data.getAsInstantOrEpoch()
    }

    fun <T : Any> takeAs(type: Class<T>): T? {
        return data.getAs(type)
    }

    /**
     * @throws ru.citeck.ecos.commons.json.exception.JsonMapperException
     */
    fun <T : Any> takeAsNotNull(type: Class<T>): T {
        return data.getAsNotNull(type)
    }

    fun toStrList(): List<String> {
        return data.toStrList()
    }

    /**
     * Convert internal value and return a new mutable list.
     * If internal value is not an array-like object then list with single element will be returned.
     */
    fun <T : Any> toList(elementType: Class<T>): MutableList<T> {
        return data.toList(elementType)
    }

    fun asStrList(): MutableList<String> {
        return data.asStrList()
    }

    /**
     * Convert internal value and return a new mutable list.
     * If internal value is not an array-like object then empty list will be returned.
     */
    fun <T : Any> asList(elementType: Class<T>): MutableList<T> {
        return data.asList(elementType)
    }

    fun asDataValue(): DataValue {
        return data.copy()
    }

    /**
     * Convert internal value and return a new mutable map.
     * If internal value is not a map-like object then empty list will be returned.
     */
    fun <K : Any, V : Any> asMap(keyType: Class<K>, valueType: Class<V>): MutableMap<K, V> {
        return data.asMap(keyType, valueType)
    }

    @com.fasterxml.jackson.annotation.JsonValue
    fun asJavaObj(): Any? {
        return data.asJavaObj()
    }

    fun asObjectData(): ObjectData {
        return data.asObjectData()
    }

    @JsonValue
    fun asJson(): JsonNode {
        return data.asJson()
    }

    fun asBinary(): ByteArray {
        return Json.mapper.toBytesNotNull(data)
    }

    override fun toString(): String {
        return data.toString()
    }

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

    override fun hashCode(): Int {
        return data.hashCode()
    }

    fun asUnmodifiable(): BpmnDataValue {
        return BpmnDataValue(data.asUnmodifiable())
    }

    fun isUnmodifiable(): Boolean {
        return data.isUnmodifiable()
    }
}
