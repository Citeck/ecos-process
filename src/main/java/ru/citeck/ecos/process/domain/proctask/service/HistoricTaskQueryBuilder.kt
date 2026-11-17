package ru.citeck.ecos.process.domain.proctask.service

import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.data.sql.records.utils.DbDateUtils
import ru.citeck.ecos.data.sql.repo.find.DbFindRes
import ru.citeck.ecos.model.lib.attributes.dto.AttributeType
import ru.citeck.ecos.process.domain.proctask.attssync.ProcTaskAttsSyncService
import ru.citeck.ecos.records2.predicate.model.*
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import ru.citeck.ecos.webapp.api.authority.EcosAuthoritiesApi
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/** Each leaf owns its EXISTS: conditions on the same variable must not be merged across AST branches. */
class HistoricTaskQueryBuilder(
    private val configuration: ProcessEngineConfigurationImpl,
    private val syncService: ProcTaskAttsSyncService,
    private val authoritiesApi: EcosAuthoritiesApi
) {
    private data class Field(
        val column: String? = null,
        val names: List<String> = emptyList(),
        val process: Boolean = false,
        val type: AttributeType = AttributeType.TEXT,
        val multiple: Boolean = false,
        val equalityOnly: Boolean = false,
        val normalization: String = ""
    )

    companion object {
        private val columns = mapOf(
            "_created" to Field("START_TIME_", type = AttributeType.DATETIME),
            "ended" to Field("END_TIME_", type = AttributeType.DATETIME),
            "dueDate" to Field("DUE_DATE_", type = AttributeType.DATETIME),
            "priority" to Field("PRIORITY_", type = AttributeType.NUMBER),
            "name" to Field("NAME_"),
            "taskKey" to Field("TASK_DEF_KEY_"),
            "deleteReason" to Field("DELETE_REASON_"),
            "assignee" to Field("ASSIGNEE_", equalityOnly = true, normalization = "user"),
            "processInstanceId" to Field("PROC_INST_ID_", equalityOnly = true, normalization = "process"),
            "processDefinitionId" to Field("PROC_DEF_ID_", equalityOnly = true)
        )
        private val ranges = setOf(ValuePredicate.Type.GT, ValuePredicate.Type.GE, ValuePredicate.Type.LT, ValuePredicate.Type.LE)
        private val textOps = setOf(ValuePredicate.Type.LIKE, ValuePredicate.Type.CONTAINS)
        private val referenceTypes = setOf(AttributeType.ASSOC, AttributeType.PERSON, AttributeType.AUTHORITY, AttributeType.AUTHORITY_GROUP)
    }

    private val params = linkedMapOf<String, Any?>()
    private fun bind(value: Any?): String = "#{p${params.size}}".also { params["p${params.size}"] = value }

    private fun field(name: String): Field = columns[name] ?: when (name) {
        "document" -> Field(names = listOf("documentRef", "mainDocumentRef"), process = true)
        "mainDocumentRef" -> Field(names = listOf(name), process = true)
        "documentType", "documentTypeRef" -> Field(
            names = listOf("documentType"),
            process = true,
            equalityOnly = true,
            normalization = "type"
        )
        "completedBy" -> Field(names = listOf(name), equalityOnly = true, normalization = "user")
        else -> {
            require(name.startsWith("_doc_")) { "Unsupported historic task field: $name" }
            val definition = requireNotNull(syncService.getTaskSyncAttribute(name)) {
                "Unknown historic task sync field: $name"
            }
            require(
                definition.type in referenceTypes ||
                    definition.type in setOf(
                        AttributeType.TEXT,
                        AttributeType.NUMBER,
                        AttributeType.DATE,
                        AttributeType.DATETIME,
                        AttributeType.BOOLEAN
                    )
            ) { "Unsupported historic task sync type: ${definition.type}" }
            Field(names = listOf(name), type = definition.type, multiple = definition.multiple)
        }
    }

    internal fun compile(predicate: Predicate): String = when (predicate) {
        is VoidPredicate -> "TRUE"
        is AndPredicate -> group(predicate.getPredicates().map { compile(it) }, "AND", "TRUE")
        is OrPredicate -> group(predicate.getPredicates().map { compile(it) }, "OR", "FALSE")
        is NotPredicate -> "NOT (${compile(predicate.getPredicate())})"
        is EmptyPredicate -> empty(field(predicate.getAttribute()))
        is ValuePredicate -> value(field(predicate.getAttribute()), predicate.getType(), predicate.getValue())
        else -> error("Unsupported historic task predicate: $predicate")
    }

    private fun group(parts: List<String>, operator: String, empty: String = "FALSE"): String = if (parts.isEmpty()) empty else parts.joinToString(" $operator ", "(", ")")

    private fun variable(field: Field, condition: () -> String): String {
        val scope = if (field.process) "v.TASK_ID_ IS NULL AND v.EXECUTION_ID_ = t.PROC_INST_ID_" else "v.TASK_ID_ = t.ID_"
        val names = field.names.joinToString(",") { bind(it) }
        val expression = condition()
        return "EXISTS (SELECT 1 FROM ${configuration.databaseTablePrefix}ACT_HI_VARINST v " +
            "WHERE (v.STATE_ IS NULL OR v.STATE_ <> 'DELETED') " +
            "AND v.PROC_INST_ID_ = t.PROC_INST_ID_ AND $scope AND v.NAME_ IN ($names) AND ($expression))"
    }

    private fun scalarStorage(field: Field): Pair<String, String> = when (field.type) {
        AttributeType.NUMBER ->
            "v.VAR_TYPE_ IN ('short','integer','long','double')" to
                "CASE WHEN v.VAR_TYPE_ = 'double' THEN v.DOUBLE_::numeric ELSE v.LONG_::numeric END"
        AttributeType.DATE, AttributeType.DATETIME -> "v.VAR_TYPE_ = 'date'" to "v.LONG_"
        AttributeType.BOOLEAN -> "v.VAR_TYPE_ = 'boolean'" to "v.LONG_"
        else -> "v.VAR_TYPE_ = 'string'" to "v.TEXT_"
    }

    private fun listExpression(): String = HistoricTaskSql.jsonListExpression(configuration.databaseTablePrefix)

    private fun empty(field: Field): String {
        if (field.column != null) {
            val col = "t.${field.column}"
            return if (field.type == AttributeType.TEXT) "($col IS NULL OR $col = '')" else "$col IS NULL"
        }
        return "NOT (${variable(field) {
            val storedScalar = "NULLIF(v.TEXT_, '') IS NOT NULL OR v.LONG_ IS NOT NULL OR v.DOUBLE_ IS NOT NULL"
            val storedBytes = if (field.multiple) {
                "v.BYTEARRAY_ID_ IS NOT NULL AND COALESCE(BTRIM(${listExpression()}), 'unsupported') <> '[]'"
            } else {
                "v.BYTEARRAY_ID_ IS NOT NULL"
            }
            "v.VAR_TYPE_ <> 'null' AND ($storedScalar OR ($storedBytes))"
        }})"
    }

    private fun value(field: Field, originalOp: ValuePredicate.Type, input: DataValue): String {
        if (input.isNull()) {
            require(originalOp == ValuePredicate.Type.EQ) { "Null is unsupported for historic task operation $originalOp" }
            return empty(field)
        }
        // The journal priority select emits text operations for numeric option values.
        val priorityValue = if (field.column == "PRIORITY_" && originalOp in textOps) {
            require(!input.isArray() && !input.isObject()) { "Historic task $originalOp requires a scalar value" }
            normalize(field, input)
        } else {
            null
        }
        val op = if ((field.normalization == "type" && originalOp in textOps) || priorityValue != null) {
            ValuePredicate.Type.EQ
        } else {
            originalOp
        }
        require(!(field.equalityOnly && op != ValuePredicate.Type.EQ && op != ValuePredicate.Type.IN)) {
            "Unsupported historic task operation $op for ${field.column ?: field.names}"
        }
        require(!(op in ranges && (field.multiple || field.type == AttributeType.BOOLEAN || field.type in referenceTypes))) {
            "Unsupported historic task range $op for ${field.names} (${field.type})"
        }
        require(!(op in textOps && !field.multiple && field.type != AttributeType.TEXT)) {
            "Unsupported historic task text operation $op for ${field.type}"
        }
        require(
            !(
                field.multiple &&
                    op in setOf(ValuePredicate.Type.EQ, ValuePredicate.Type.IN) &&
                    field.type != AttributeType.TEXT &&
                    field.type !in referenceTypes
                )
        ) {
            "Unsupported historic task exact list operation $op for ${field.type}"
        }
        if (op == ValuePredicate.Type.IN) {
            require(input.isArray()) { "Historic task IN requires an array" }
            return group(input.map { value(field, ValuePredicate.Type.EQ, it) }, "OR")
        }
        require(!input.isArray() && !input.isObject()) { "Historic task $op requires a scalar value" }
        if (!field.multiple && (field.type == AttributeType.DATE || field.type == AttributeType.DATETIME)) {
            if (input.isTextual()) {
                val normalized = DbDateUtils.normalizeDateTimePredicateValue(input.asText(), true)
                if (normalized.contains('/')) {
                    val bounds = normalized.split('/')
                    require(bounds.size == 2 && bounds.all { it.isNotBlank() }) { "Invalid historic date range: $input" }
                    return group(
                        listOf(
                            value(field, ValuePredicate.Type.GE, DataValue.create(bounds[0])),
                            value(field, ValuePredicate.Type.LT, DataValue.create(bounds[1]))
                        ),
                        "AND"
                    )
                }
            }
        }
        val values = priorityValue ?: if (field.multiple && op in textOps && field.type !in referenceTypes) listOf(input.asText()) else normalize(field, input)
        if (values.size != 1) {
            require(op == ValuePredicate.Type.EQ || op in textOps) { "Multiple placeholder values unsupported for $op" }
        }
        return group(
            values.map { normalized ->
                if (field.column != null) {
                    compare("t.${field.column}", op, normalized)
                } else {
                    variable(field) {
                        if (field.multiple) {
                            val pattern = if (op == ValuePredicate.Type.EQ) {
                                ProcTaskListAttConditions.elementLikePattern(normalized)
                            } else {
                                "%$normalized%"
                            }
                            val expression = listExpression()
                            if (field.type == AttributeType.TEXT || op in textOps) {
                                "COALESCE(LOWER($expression COLLATE \"default\") LIKE ${bind(pattern.lowercase())}, FALSE)"
                            } else {
                                "COALESCE($expression LIKE ${bind(pattern)}, FALSE)"
                            }
                        } else {
                            val (guard, col) = scalarStorage(field)
                            "$guard AND ${compare(col, op, normalized)}"
                        }
                    }
                }
            },
            "OR"
        )
    }

    private fun compare(column: String, op: ValuePredicate.Type, normalized: Any): String {
        var value = normalized
        if (value is Timestamp && value.nanos % 1000 != 0) {
            // PostgreSQL timestamps have microsecond precision. Round the boundary in the
            // direction of the operator, not by JDBC's nearest-microsecond conversion.
            if (op == ValuePredicate.Type.EQ) return "FALSE"
            val fraction = value.nanos % 1000
            val adjustment = if (op == ValuePredicate.Type.GE || op == ValuePredicate.Type.LT) 1000 - fraction else -fraction
            value = Timestamp.from(value.toInstant().plusNanos(adjustment.toLong()))
        }
        val operation = when (op) {
            ValuePredicate.Type.EQ -> "="
            ValuePredicate.Type.GT -> ">"
            ValuePredicate.Type.GE -> ">="
            ValuePredicate.Type.LT -> "<"
            ValuePredicate.Type.LE -> "<="
            ValuePredicate.Type.LIKE, ValuePredicate.Type.CONTAINS -> "LIKE"
            else -> error("Unsupported historic task operation: $op")
        }
        return if (op in textOps) {
            "COALESCE(LOWER($column) LIKE ${bind("%${value.toString().lowercase()}%")}, FALSE)"
        } else {
            "COALESCE($column $operation ${bind(value)}, FALSE)"
        }
    }

    private fun normalize(field: Field, input: DataValue): List<Any> {
        val text = input.asText()
        if (field.normalization == "user" || field.type in referenceTypes - AttributeType.ASSOC) {
            if (text == ATT_CURRENT_USER || text == ATT_CURRENT_USER_WITH_AUTH) {
                return if (field.normalization != "user" && text == ATT_CURRENT_USER_WITH_AUTH) {
                    AuthContext.getCurrentUserWithAuthorities()
                } else {
                    listOf(AuthContext.getCurrentUser())
                }
            }
            require(input.isTextual()) { "Historic authority requires a string: $input" }
            return listOf(authoritiesApi.getAuthorityName(text))
        }
        if (field.normalization == "process" || field.normalization == "type") {
            require(input.isTextual() && text.isNotBlank()) { "Invalid historic reference: $input" }
            if (!text.contains('@') && !text.contains('/')) return listOf(text)
            val ref = EntityRef.valueOf(text)
            val expected = if (field.normalization == "process") "eproc/bpmn-proc@" else "emodel/type@"
            require(ref.toString().startsWith(expected) && ref.getLocalId().isNotBlank()) { "Invalid historic reference: $input" }
            return listOf(ref.getLocalId())
        }
        return listOf(
            when (field.type) {
                AttributeType.NUMBER -> {
                    require(input.isNumber() || input.isTextual()) { "Invalid historic number: $input" }
                    requireNotNull(text.toBigDecimalOrNull()) { "Invalid historic number: $input" }
                }
                AttributeType.BOOLEAN -> {
                    require(input.isBoolean() || text == "true" || text == "false") { "Invalid historic boolean: $input" }
                    if (text == "true") 1L else 0L
                }
                AttributeType.DATE, AttributeType.DATETIME -> {
                    val instant = if (input.isNumber()) {
                        val millis = input.asText().toBigDecimal()
                        Instant.ofEpochSecond(
                            millis.divideToIntegralValue(BigDecimal(1000)).longValueExact(),
                            millis.remainder(BigDecimal(1000)).multiply(BigDecimal(1_000_000)).longValueExact()
                        )
                    } else {
                        require(input.isTextual()) { "Invalid historic date: $input" }
                        val normalized = DbDateUtils.normalizeDateTimePredicateValue(text, true)
                        val dateTime = if ('T' in normalized) normalized else "${normalized}T00:00:00Z"
                        try {
                            Instant.parse(dateTime)
                        } catch (_: DateTimeParseException) {
                            // Match current task filters' ISO support, including minute-precision offsets.
                            OffsetDateTime.parse(dateTime).toInstant()
                        }
                    }
                    if (field.column != null) {
                        Timestamp.from(instant)
                    } else {
                        BigDecimal.valueOf(instant.epochSecond).multiply(BigDecimal(1000)) + BigDecimal.valueOf(instant.nano.toLong(), 6)
                    }
                }
                else -> {
                    require(input.isTextual()) { "Historic text/reference requires a string: $input" }
                    text
                }
            }
        )
    }

    fun select(predicate: Predicate, sorting: List<SortBy>, skipCount: Int, maxItems: Int): DbFindRes<String> {
        require(skipCount >= 0) { "Historic task skipCount must be nonnegative" }
        params.clear()
        val user = compile(predicate)
        val actor = if (AuthContext.isRunAsSystem()) "TRUE" else compile(Predicates.eq("completedBy", AuthContext.getCurrentUser()))
        val where = "t.END_TIME_ IS NOT NULL AND ($actor) AND ($user)"
        val order = sorting.ifEmpty { listOf(SortBy("ended", false)) }.map {
            val column = requireNotNull(columns[it.attribute]) { "Unsupported historic task sort: ${it.attribute}" }.column
            "t.$column ${if (it.ascending) "ASC" else "DESC"} NULLS LAST"
        }.plus("t.ID_ ASC").joinToString(", ")
        if (configuration.historyLevel.id == 0) return DbFindRes.empty()
        val from = "FROM ${configuration.databaseTablePrefix}ACT_HI_TASKINST t WHERE $where"
        val count = HistoricTaskSql.query(configuration, "SELECT COUNT(*) $from", params) { it.getLong(1) }.single()
        val limit = if (maxItems < 0) 1000 else maxItems
        val ids = if (limit == 0) {
            emptyList()
        } else {
            HistoricTaskSql.query(
                configuration,
                "SELECT t.ID_ $from ORDER BY $order LIMIT ${bind(limit)} OFFSET ${bind(skipCount)}",
                params
            ) { it.getString(1) }
        }
        return DbFindRes(ids, count)
    }
}
