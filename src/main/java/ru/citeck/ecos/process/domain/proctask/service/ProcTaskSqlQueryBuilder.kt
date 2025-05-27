package ru.citeck.ecos.process.domain.proctask.service

import mu.KotlinLogging
import org.camunda.bpm.engine.TaskService
import org.camunda.bpm.engine.impl.TaskQueryProperty
import org.camunda.bpm.engine.task.NativeTaskQuery
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.context.lib.auth.AuthGroup
import ru.citeck.ecos.context.lib.auth.AuthRole
import ru.citeck.ecos.data.sql.repo.find.DbFindRes
import ru.citeck.ecos.model.lib.attributes.dto.AttributeType
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT_REF
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT_TYPE
import ru.citeck.ecos.process.domain.proctask.attssync.ProcTaskAttsSyncService
import ru.citeck.ecos.process.domain.proctask.attssync.ProcTaskAttsSynchronizer.Companion.TASK_DOCUMENT_ATT_PREFIX
import ru.citeck.ecos.process.domain.proctask.attssync.ProcTaskAttsSynchronizer.Companion.TASK_DOCUMENT_TYPE_ATT_PREFIX
import ru.citeck.ecos.process.domain.proctask.service.ProcTaskSqlQueryBuilder.Companion.ATT_ACTORS
import ru.citeck.ecos.process.domain.proctask.service.ProcTaskSqlQueryBuilder.Companion.ATT_ASSIGNEE
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.predicate.model.*
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import ru.citeck.ecos.webapp.api.authority.EcosAuthoritiesApi
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.system.measureTimeMillis

const val ATT_CURRENT_USER_WITH_AUTH = "\$CURRENT"
const val ATT_CURRENT_USER = "\$CURRENT_USER"

/**
 * Convert predicate to SQL query for Camunda tasks
 *
 * @property ATT_ACTORS means all possible candidates for the task
 * @property ATT_ASSIGNEE means exact user who is assigned to the task
 */
class ProcTaskSqlQueryBuilder(
    private val authoritiesApi: EcosAuthoritiesApi,
    private val taskService: TaskService,
    private val procTaskAttsSyncService: ProcTaskAttsSyncService
) {

    companion object {

        private const val TASK_ALIAS = "task"
        private const val CANDIDATE_ALIAS = "candidate"
        private const val VARIABLE_ALIAS_PREFIX = "var"

        const val ATT_ACTORS = "actors"
        const val ATT_ACTOR = "actor"
        const val ATT_ASSIGNEE = "assignee"
        const val ATT_NAME = "name"
        const val ATT_DUE_DATE = "dueDate"
        const val ATT_PRIORITY = "priority"
        const val ATT_DOCUMENT = "document"
        const val ATT_DOCUMENT_TYPE = "documentType"
        const val ATT_DOCUMENT_TYPE_REF = "documentTypeRef"
        const val ATT_TASK_KEY = "taskKey"

        private val TASK_ATTS_MAPPING = mapOf(
            RecordConstants.ATT_CREATED to "$TASK_ALIAS.${TaskQueryProperty.CREATE_TIME.name}",
            ATT_ASSIGNEE to "$TASK_ALIAS.${TaskQueryProperty.ASSIGNEE.name}",
            ATT_TASK_KEY to "$TASK_ALIAS.task_def_key_",
            ATT_NAME to "$TASK_ALIAS.${TaskQueryProperty.NAME.name}",
            ATT_DUE_DATE to "$TASK_ALIAS.${TaskQueryProperty.DUE_DATE.name}",
            ATT_PRIORITY to "$TASK_ALIAS.${TaskQueryProperty.PRIORITY.name}"
        )

        private val TASK_ATTS_TYPES = mapOf(
            RecordConstants.ATT_CREATED to AttributeType.DATETIME,
            ATT_NAME to AttributeType.TEXT,
            ATT_DUE_DATE to AttributeType.DATETIME,
            ATT_PRIORITY to AttributeType.NUMBER
        )

        private val PROC_VARIABLES_MAPPING = mapOf(
            ATT_DOCUMENT to BPMN_DOCUMENT_REF,
            ATT_DOCUMENT_TYPE to BPMN_DOCUMENT_TYPE,
            ATT_DOCUMENT_TYPE_REF to "documentTypeRef"
        )

        private val log = KotlinLogging.logger {}
    }

    private data class VariableCondition(
        val name: String,
        val isProcessVar: Boolean,
        val attType: AttributeType,
        val isEmptyCondition: Boolean = false,
        val eqValues: MutableList<Any?> = mutableListOf(),
        val otherConditions: MutableList<String> = mutableListOf()
    )

    private val joins = StringBuilder()
    private val condition = StringBuilder()
    private val params = LinkedHashMap<String, Any?>()
    private val variableConditions = mutableMapOf<String, VariableCondition>()

    private var skipCount = 0
    private var maxItems = -1

    private var sorting: List<SortBy> = emptyList()

    fun addConditions(predicate: Predicate): ProcTaskSqlQueryBuilder {
        addConditionsImpl(predicate)
        return this
    }

    private fun addConditionsImpl(predicate: Predicate): Boolean {
        return when (predicate) {
            is ValuePredicate -> addValueCondition(predicate)
            is EmptyPredicate -> {
                if (PROC_VARIABLES_MAPPING.containsKey(predicate.getAttribute())) {
                    addEmptyVariableCondition(PROC_VARIABLES_MAPPING[predicate.getAttribute()], true)
                } else if (predicate.getAttribute().isAttFromSync()) {
                    addEmptyVariableCondition(predicate.getAttribute(), false)
                } else if (TASK_ATTS_MAPPING.containsKey(predicate.getAttribute())) {
                    val field = TASK_ATTS_MAPPING[predicate.getAttribute()]
                    if (field.isNullOrBlank()) {
                        false
                    } else {
                        val fieldType = TASK_ATTS_TYPES[predicate.getAttribute()] ?: AttributeType.TEXT
                        if (fieldType == AttributeType.TEXT) {
                            condition.append(" (")
                                .append(field)
                                .append(" IS NULL OR ")
                                .append(field)
                                .append(" = '') ")
                        } else {
                            condition.append(" ")
                                .append(field)
                                .append(" IS NULL ")
                        }
                        true
                    }
                } else {
                    false
                }
            }

            is NotPredicate -> {
                val prevLen = condition.length
                condition.append(" NOT (")
                if (addConditionsImpl(predicate.getPredicate())) {
                    condition.append(")")
                    true
                } else {
                    condition.setLength(prevLen)
                    false
                }
            }

            is ComposedPredicate -> {

                val joinOperator: String = when (predicate) {
                    is AndPredicate -> " AND "
                    is OrPredicate -> " OR "
                    else -> error("Unknown predicate type: " + predicate.javaClass)
                }
                condition.append("(")
                var notEmpty = false
                for (innerPred in predicate.getPredicates()) {
                    if (addConditionsImpl(innerPred)) {
                        condition.append(joinOperator)
                        notEmpty = true
                    }
                }
                if (notEmpty) {
                    condition.setLength(condition.length - joinOperator.length)
                    condition.append(")")
                    true
                } else {
                    condition.setLength(condition.length - 1)
                    false
                }
            }

            else -> false
        }
    }

    fun setPage(skipCount: Int, maxItems: Int): ProcTaskSqlQueryBuilder {
        this.skipCount = skipCount
        this.maxItems = maxItems
        return this
    }

    fun setSorting(sorting: List<SortBy>): ProcTaskSqlQueryBuilder {
        this.sorting = sorting.mapNotNull { origSortBy ->
            if (TASK_ATTS_MAPPING.containsKey(origSortBy.attribute)) {
                TASK_ATTS_MAPPING[origSortBy.attribute]?.let {
                    SortBy(it, origSortBy.ascending)
                }
            } else {
                null
            }
        }
        return this
    }

    private fun addValueCondition(predicate: ValuePredicate): Boolean {

        var attribute = predicate.getAttribute()
        val type = predicate.getType()
        var value = predicate.getValue()

        if (attribute == ATT_ACTOR) {
            attribute = ATT_ACTORS
            if (value.isTextual() && value.asText() == "\$CURRENT") {
                value = DataValue.create(AuthContext.getCurrentUserWithAuthorities())
            }
        }

        if (attribute == ATT_ACTORS) {
            if (!joins.contains("JOIN act_ru_identitylink")) {
                joins.append(
                    " LEFT JOIN act_ru_identitylink $CANDIDATE_ALIAS ON " +
                        "$CANDIDATE_ALIAS.task_id_ = $TASK_ALIAS.id_ " +
                        "AND $CANDIDATE_ALIAS.type_ = 'candidate'"
                )
            }

            val actors: List<Any?> = castSqlParamValueToListOf(value, AttributeType.AUTHORITY)

            val users = actors.filter {
                it is String && !it.startsWith(AuthGroup.PREFIX) && !it.startsWith(AuthRole.PREFIX)
            }
            val groups = actors.filter {
                it is String && it.startsWith(AuthGroup.PREFIX)
            }
            if (users.isEmpty() && groups.isEmpty()) {
                condition.append(
                    "(" +
                        "$TASK_ALIAS.assignee_ IS NULL AND " +
                        "$CANDIDATE_ALIAS.user_id_ IS NULL AND " +
                        "$CANDIDATE_ALIAS.group_id_ IS NULL" +
                        ")"
                )
                return true
            }

            condition.append("(")
            if (users.isNotEmpty()) {
                condition.append("$TASK_ALIAS.assignee_ IN (")
                addSqlQueryParams(condition, users)
                condition.append(") OR ")
            }
            condition.append("$TASK_ALIAS.assignee_ IS NULL AND (")
            if (users.isNotEmpty()) {
                condition.append("$CANDIDATE_ALIAS.user_id_ IN (")
                addSqlQueryParams(condition, users)
                condition.append(")")
            }
            if (groups.isNotEmpty()) {
                if (users.isNotEmpty()) {
                    condition.append(" OR ")
                }
                condition.append("$CANDIDATE_ALIAS.group_id_ IN (")
                addSqlQueryParams(condition, groups)
                condition.append(")")
            }
            condition.append("))")
            return true
        } else if (PROC_VARIABLES_MAPPING.containsKey(attribute)) {

            return addVariableCondition(
                PROC_VARIABLES_MAPPING[attribute],
                value,
                type,
                isProcessVar = true,
                isRuVariable = true
            )
        } else if (attribute.isAttFromSync()) {

            return addVariableCondition(attribute, value, type, isProcessVar = false, isRuVariable = true)
        } else if (TASK_ATTS_MAPPING.containsKey(attribute)) {

            val field = TASK_ATTS_MAPPING[attribute]
            if (field.isNullOrBlank()) {
                return false
            }
            val operator = when (type) {
                ValuePredicate.Type.GT -> ">"
                ValuePredicate.Type.LT -> "<"
                ValuePredicate.Type.GE -> ">="
                ValuePredicate.Type.LE -> "<="
                ValuePredicate.Type.EQ -> "="
                ValuePredicate.Type.LIKE,
                ValuePredicate.Type.CONTAINS ->
                    if (value.isNumber()) {
                        "="
                    } else {
                        "LIKE"
                    }

                else -> return false
            }

            val attType = TASK_ATTS_TYPES[attribute] ?: AttributeType.TEXT

            if (attType.shouldUseLowerCase(type)) {
                value = DataValue.create("%${value.asText().lowercase()}%")
                condition.append(" ")
                    .append("LOWER(")
                    .append(field)
                    .append(") ")
                    .append(operator)
                    .append(" ")
            } else {
                condition.append(" ")
                    .append(field)
                    .append(" ")
                    .append(operator)
                    .append(" ")
            }

            val predicateValue = castSqlParamValueToListOf(value, attType)
            addSqlQueryParams(condition, predicateValue)
            return true
        }

        return false
    }

    private fun addEmptyVariableCondition(
        name: String?,
        isProcessVar: Boolean
    ): Boolean {
        if (name.isNullOrBlank()) {
            return false
        }

        val attType = procTaskAttsSyncService.getTaskAttTypeOrTextDefault(name)
        variableConditions.getOrPut(name) {
            VariableCondition(name, isProcessVar, attType, isEmptyCondition = true)
        }

        val alias = getVariableAlias(name)

        condition.append("$alias.id_ IS NULL")
        return true
    }

    private fun addVariableCondition(
        name: String?,
        value: DataValue,
        predicateType: ValuePredicate.Type,
        isProcessVar: Boolean,
        isRuVariable: Boolean = false
    ): Boolean {

        if (name.isNullOrBlank()) {
            return false
        }

        val attType = procTaskAttsSyncService.getTaskAttTypeOrTextDefault(name)
        val variableCondition = variableConditions.getOrPut(name) {
            VariableCondition(name, isProcessVar, attType)
        }

        val alias = getVariableAlias(name)
        val taskColumn = attType.getTaskAttColumn()

        val values = castSqlParamValueToListOf(value, attType, isRuVariable = isRuVariable)

        when (predicateType) {
            ValuePredicate.Type.EQ -> {
                // Collect EQ values to group them into IN later
                variableCondition.eqValues.addAll(values)
            }

            ValuePredicate.Type.GT -> {
                val paramName = "p${params.size}"
                params[paramName] = values[0]
                variableCondition.otherConditions.add("$alias.$taskColumn > #{$paramName}")
            }

            ValuePredicate.Type.LT -> {
                val paramName = "p${params.size}"
                params[paramName] = values[0]
                variableCondition.otherConditions.add("$alias.$taskColumn < #{$paramName}")
            }

            ValuePredicate.Type.GE -> {
                val paramName = "p${params.size}"
                params[paramName] = values[0]
                variableCondition.otherConditions.add("$alias.$taskColumn >= #{$paramName}")
            }

            ValuePredicate.Type.LE -> {
                val paramName = "p${params.size}"
                params[paramName] = values[0]
                variableCondition.otherConditions.add("$alias.$taskColumn <= #{$paramName}")
            }

            ValuePredicate.Type.IN -> {
                val paramNames = mutableListOf<String>()
                values.forEach {
                    val paramName = "p${params.size}"
                    params[paramName] = it
                    paramNames.add("#{$paramName}")
                }
                variableCondition.otherConditions.add("$alias.$taskColumn IN (${paramNames.joinToString(",")})")
            }

            ValuePredicate.Type.CONTAINS,
            ValuePredicate.Type.LIKE -> {
                if (attType == AttributeType.TEXT) {
                    val likeValues = values.map { "%$it%".lowercase() }
                    val paramNames = mutableListOf<String>()
                    likeValues.forEach {
                        val paramName = "p${params.size}"
                        params[paramName] = it
                        paramNames.add("#{$paramName}")
                    }
                    val columnExpression = "LOWER($alias.$taskColumn)"
                    if (paramNames.size == 1) {
                        variableCondition.otherConditions.add("$columnExpression LIKE ${paramNames[0]}")
                    } else {
                        variableCondition.otherConditions.add("(${paramNames.map { "$columnExpression LIKE $it" }.joinToString(" OR ")})")
                    }
                } else {
                    val likeValues = values.map { "%$it%" }
                    val paramNames = mutableListOf<String>()
                    likeValues.forEach {
                        val paramName = "p${params.size}"
                        params[paramName] = it
                        paramNames.add("#{$paramName}")
                    }
                    if (paramNames.size == 1) {
                        variableCondition.otherConditions.add("$alias.$taskColumn LIKE ${paramNames[0]}")
                    } else {
                        variableCondition.otherConditions.add("(${paramNames.map { "$alias.$taskColumn LIKE $it" }.joinToString(" OR ")})")
                    }
                }
            }

            else -> return false
        }
        condition.append("$alias.id_ IS NOT NULL")
        return true
    }

    private fun castSqlParamValueToListOf(
        value: DataValue,
        type: AttributeType,
        result: MutableList<Any?> = ArrayList(),
        isRuVariable: Boolean = false
    ): List<Any?> {
        if (value.isTextual() && value.asText() == ATT_CURRENT_USER_WITH_AUTH) {
            return AuthContext.getCurrentUserWithAuthorities()
        }

        if (value.isTextual() && value.asText() == ATT_CURRENT_USER) {
            return listOf(AuthContext.getCurrentUser())
        }

        if (value.isArray()) {
            value.forEach {
                castSqlParamValueToListOf(it, type, result)
            }
            return result
        }
        if (value.isNull()) {
            result.add(null)
        }
        val convertedValue: Any = when (type) {
            // act_ru_variable store boolean as long_, 0 or 1
            AttributeType.BOOLEAN -> {
                if (isRuVariable) {
                    if (value.asBoolean()) {
                        1
                    } else {
                        0
                    }
                } else {
                    value
                }
            }

            AttributeType.DATETIME,
            AttributeType.DATE -> {
                val dateTime = if (value.isTextual()) {
                    val txt = value.asText()
                    OffsetDateTime.parse(
                        if (!txt.contains('T')) {
                            "${txt}T00:00:00Z"
                        } else {
                            txt
                        }
                    )
                } else if (value.isNumber()) {
                    OffsetDateTime.ofInstant(Instant.ofEpochMilli(value.asLong()), ZoneOffset.UTC)
                } else {
                    error("Unknown date or datetime value: '$value'")
                }

                // act_ru_variable store date as long_, milliseconds
                if (isRuVariable) {
                    dateTime.toInstant().toEpochMilli()
                } else {
                    if (type == AttributeType.DATE) {
                        dateTime.toLocalDate()
                    } else {
                        dateTime
                    }
                }
            }

            AttributeType.PERSON,
            AttributeType.AUTHORITY_GROUP,
            AttributeType.AUTHORITY -> {
                if (value.isTextual()) {
                    authoritiesApi.getAuthorityName(value.asText())
                } else {
                    error("Invalid authority type: $value")
                }
            }

            AttributeType.NUMBER -> {
                when {
                    value.isIntegralNumber() -> value.asInt()
                    value.isLong() -> value.asLong()
                    value.isFloatingPointNumber() -> value.asDouble()
                    else -> error("Invalid number value: $value")
                }
            }

            else -> value.asText()
        }
        result.add(convertedValue)
        return result
    }

    private fun addSqlQueryParams(
        query: StringBuilder,
        values: Collection<Any?>
    ) {
        if (values.isEmpty()) {
            return
        }
        for (value in values) {
            val paramName = "p${params.size}"
            params[paramName] = value
            query.append("#{$paramName}").append(",")
        }
        query.setLength(query.length - 1)
    }

    private fun getVariableAlias(variableName: String): String {
        return "${VARIABLE_ALIAS_PREFIX}_${variableName.replace("[^a-zA-Z0-9_]".toRegex(), "_")}"
    }

    private fun createTaskQuery(
        selectFields: String,
        withLimitAndSort: Boolean
    ): NativeTaskQuery {
        val sqlSelectQuery = StringBuilder("SELECT $selectFields")
        if (withLimitAndSort) {
            for (sortBy in sorting) {
                sqlSelectQuery.append(",${sortBy.attribute}")
            }
        }
        sqlSelectQuery.append(" FROM act_ru_task $TASK_ALIAS ")

        // Add existing joins
        if (joins.isNotEmpty()) {
            sqlSelectQuery.append(joins.toString()).append(" ")
        }

        // Add optimized variable joins with conditions
        for ((variableName, variableCondition) in variableConditions) {
            val alias = getVariableAlias(variableName)
            val taskAttType = variableCondition.attType.getTaskAttType()

            sqlSelectQuery.append(" LEFT JOIN act_ru_variable $alias ON ")
            sqlSelectQuery.append("$alias.name_ = '$variableName' AND ")
            sqlSelectQuery.append("$alias.type_ = '$taskAttType' AND ")
            sqlSelectQuery.append("$alias.proc_inst_id_ = $TASK_ALIAS.proc_inst_id_ AND ")

            if (variableCondition.isProcessVar) {
                sqlSelectQuery.append("$alias.task_id_ IS NULL")
            } else {
                sqlSelectQuery.append("$alias.task_id_ = $TASK_ALIAS.id_")
            }

            // Add filter conditions directly to JOIN for better performance
            if (!variableCondition.isEmptyCondition) {
                // EQ values should be combined with OR, other conditions with AND
                val eqConditions = mutableListOf<String>()
                val otherConditions = mutableListOf<String>()

                // Add EQ values as IN condition
                if (variableCondition.eqValues.isNotEmpty()) {
                    val taskColumn = variableCondition.attType.getTaskAttColumn()
                    if (variableCondition.eqValues.size == 1) {
                        val paramName = "p${params.size}"
                        params[paramName] = variableCondition.eqValues[0]
                        eqConditions.add("$alias.$taskColumn = #{$paramName}")
                    } else {
                        val paramNames = mutableListOf<String>()
                        variableCondition.eqValues.forEach {
                            val paramName = "p${params.size}"
                            params[paramName] = it
                            paramNames.add("#{$paramName}")
                        }
                        eqConditions.add("$alias.$taskColumn IN (${paramNames.joinToString(",")})")
                    }
                }

                // Add other conditions (GT, LT, GE, LE, etc.)
                otherConditions.addAll(variableCondition.otherConditions)

                val finalConditions = mutableListOf<String>()
                if (eqConditions.isNotEmpty()) {
                    finalConditions.add(eqConditions.joinToString(" OR "))
                }
                if (otherConditions.isNotEmpty()) {
                    // Each condition in otherConditions is already complete and should be added as-is
                    // For range conditions (like GE and LT together), they should be combined with AND
                    // For CONTAINS with multiple values, the condition is already properly formed with OR inside
                    if (otherConditions.size == 1) {
                        finalConditions.add(otherConditions[0])
                    } else {
                        // Check if these are range conditions (GE/LE and LT/GT combinations)
                        val hasRangeConditions = otherConditions.any { condition ->
                            condition.contains(" >= ") || condition.contains(" <= ") ||
                                condition.contains(" > ") || condition.contains(" < ")
                        }
                        val hasLikeConditions = otherConditions.any { condition ->
                            condition.contains(" LIKE ")
                        }

                        if (hasRangeConditions && !hasLikeConditions) {
                            // Range conditions should be combined with AND
                            finalConditions.add(otherConditions.joinToString(" AND "))
                        } else {
                            // LIKE/CONTAINS conditions should be combined with OR
                            finalConditions.add(otherConditions.joinToString(" OR "))
                        }
                    }
                }

                if (finalConditions.isNotEmpty()) {
                    sqlSelectQuery.append(" AND (")
                    if (finalConditions.size == 1) {
                        sqlSelectQuery.append(finalConditions[0])
                    } else {
                        // If we have both EQ and other conditions, combine them with OR
                        sqlSelectQuery.append(finalConditions.joinToString(" OR "))
                    }
                    sqlSelectQuery.append(")")
                }
            }
        }

        if (condition.isNotEmpty()) {
            sqlSelectQuery.append(" WHERE ")
                .append(condition.toString())
        }

        if (withLimitAndSort && sorting.isNotEmpty()) {
            sqlSelectQuery.append(" ORDER BY ")
            sorting.forEach {
                sqlSelectQuery.append(it.attribute)
                sqlSelectQuery.append(
                    if (it.ascending) {
                        " ASC,"
                    } else {
                        " DESC,"
                    }
                )
            }
            sqlSelectQuery.setLength(sqlSelectQuery.length - 1)
        }

        if (withLimitAndSort) {
            val maxItems = if (this.maxItems < 0) {
                1000
            } else {
                this.maxItems
            }
            sqlSelectQuery.append(" LIMIT $maxItems")
        }
        if (withLimitAndSort && skipCount > 0) {
            sqlSelectQuery.append(" OFFSET $skipCount")
        }

        var nativeTaskQuery = taskService.createNativeTaskQuery().sql(sqlSelectQuery.toString())
        for ((key, value) in params) {
            nativeTaskQuery = nativeTaskQuery.parameter(key, value)
        }

        log.trace { "Build proc task query:\n $sqlSelectQuery \n with params: \n$params" }

        return nativeTaskQuery
    }

    fun selectTasks(): DbFindRes<String> {
        val tasks: List<String>
        val tasksFromCamundaTime = measureTimeMillis {
            tasks = createTaskQuery(
                "DISTINCT $TASK_ALIAS.${TaskQueryProperty.TASK_ID.name}",
                withLimitAndSort = true
            ).list().map { it.id }
        }

        val totalCount: Long
        val camundaCountTime = measureTimeMillis {
            totalCount = if (maxItems > tasks.size) {
                tasks.size.toLong()
            } else {
                createTaskQuery(
                    "COUNT(DISTINCT $TASK_ALIAS.${TaskQueryProperty.TASK_ID.name})",
                    withLimitAndSort = false
                ).count()
            }
        }

        log.debug { "Camunda task count: $camundaCountTime ms" }
        log.debug { "Camunda tasks: $tasksFromCamundaTime ms" }

        return DbFindRes(tasks, totalCount)
    }

    private fun AttributeType.getTaskAttType(): String {
        return when (this) {
            AttributeType.DATE,
            AttributeType.DATETIME -> "date"

            AttributeType.NUMBER -> "double"
            AttributeType.BOOLEAN -> "boolean"
            else -> "string"
        }
    }

    private fun AttributeType.getTaskAttColumn(predicateType: ValuePredicate.Type? = null): String {
        return when (this) {
            AttributeType.TEXT -> "text_"
            AttributeType.NUMBER -> "double_"
            AttributeType.DATE,
            AttributeType.DATETIME,
            AttributeType.BOOLEAN -> "long_"
            else -> "text_"
        }
    }

    private fun AttributeType.shouldUseLowerCase(predicateType: ValuePredicate.Type? = null): Boolean {
        return this == AttributeType.TEXT &&
            (predicateType == ValuePredicate.Type.CONTAINS || predicateType == ValuePredicate.Type.LIKE)
    }

    private fun String.isAttFromSync(): Boolean = this.startsWith(TASK_DOCUMENT_ATT_PREFIX) ||
        this.startsWith(TASK_DOCUMENT_TYPE_ATT_PREFIX)
}
