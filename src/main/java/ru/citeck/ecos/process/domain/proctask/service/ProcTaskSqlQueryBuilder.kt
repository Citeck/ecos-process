package ru.citeck.ecos.process.domain.proctask.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.camunda.bpm.engine.TaskService
import org.camunda.bpm.engine.impl.TaskQueryProperty
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl
import org.camunda.bpm.engine.task.NativeTaskQuery
import org.springframework.jdbc.datasource.DataSourceUtils
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
import ru.citeck.ecos.process.domain.proctask.attssync.TaskSyncAttribute
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
    private val procTaskAttsSyncService: ProcTaskAttsSyncService,
    private val processEngineConfiguration: ProcessEngineConfigurationImpl
) {

    companion object {

        private const val TASK_ALIAS = "task"
        private const val IDENTITY_LINK_ALIAS = "il"
        private const val VARIABLE_ALIAS_PREFIX = "var"
        private const val DEFAULT_MAX_ITEMS = 1000

        // SQL fragment that never matches — used to make a predicate yield no rows.
        private const val ALWAYS_FALSE = "1 = 0"

        const val ATT_ACTORS = "actors"
        const val ATT_ACTOR = "actor"
        const val ATT_ASSIGNEE = "assignee"
        const val ATT_NAME = "name"
        const val ATT_DUE_DATE = "dueDate"
        const val ATT_PRIORITY = "priority"
        const val ATT_DOCUMENT = "document"
        const val ATT_MAIN_DOCUMENT_REF = "mainDocumentRef"
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
            ATT_DOCUMENT to listOf(BPMN_DOCUMENT_REF),
            ATT_DOCUMENT_TYPE to listOf(BPMN_DOCUMENT_TYPE),
            ATT_DOCUMENT_TYPE_REF to listOf("documentTypeRef"),
            ATT_MAIN_DOCUMENT_REF to listOf(ATT_MAIN_DOCUMENT_REF)
        )

        /**
         * Textual "document" value is matched against both documentRef and mainDocumentRef
         * process variables. Both names are checked within a single condition, so that
         * PostgreSQL can turn it into a semi-join instead of "EXISTS(a) OR EXISTS(b)",
         * which would be evaluated per task row.
         */
        private val DOCUMENT_VARIABLE_NAMES = listOf(BPMN_DOCUMENT_REF, ATT_MAIN_DOCUMENT_REF)

        private const val VAR_COND_PLACEHOLDER_PREFIX = "@@VAR_COND_"
        private const val VAR_COND_PLACEHOLDER_SUFFIX = "@@"

        private val log = KotlinLogging.logger {}
    }

    private fun getEffectiveMaxItems(): Int = if (maxItems < 0) DEFAULT_MAX_ITEMS else maxItems

    private data class VariableConditionKey(
        val names: List<String>,
        val isEmptyCondition: Boolean
    )

    private data class VariableCondition(
        val index: Int,
        val names: List<String>,
        val isProcessVar: Boolean,
        val attType: AttributeType,
        val isEmptyCondition: Boolean = false,
        val eqValues: MutableList<Any?> = mutableListOf(),
        val otherConditions: MutableList<String> = mutableListOf(),
        val isList: Boolean = false
    )

    private val condition = StringBuilder()
    private val params = LinkedHashMap<String, Any?>()
    private val variableConditions = mutableMapOf<VariableConditionKey, VariableCondition>()

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
                    addEmptyVariableCondition(listOf(predicate.getAttribute()), false)
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

        val attribute = predicate.getAttribute()
        val type = predicate.getType()
        var value = predicate.getValue()

        if (attribute == ATT_ACTOR || attribute == ATT_ACTORS) {

            if (attribute == ATT_ACTOR) {
                if (value.isTextual() && value.asText() == "\$CURRENT") {
                    value = DataValue.create(AuthContext.getCurrentUserWithAuthorities())
                }
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
                        "NOT EXISTS (SELECT 1 FROM act_ru_identitylink $IDENTITY_LINK_ALIAS " +
                        "WHERE $IDENTITY_LINK_ALIAS.task_id_ = $TASK_ALIAS.id_ AND $IDENTITY_LINK_ALIAS.type_ = 'candidate')" +
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
            condition.append("($TASK_ALIAS.assignee_ IS NULL AND EXISTS (")
            condition.append("SELECT 1 FROM act_ru_identitylink $IDENTITY_LINK_ALIAS ")
            condition.append("WHERE $IDENTITY_LINK_ALIAS.task_id_ = $TASK_ALIAS.id_ AND $IDENTITY_LINK_ALIAS.type_ = 'candidate' AND (")
            if (users.isNotEmpty()) {
                condition.append("$IDENTITY_LINK_ALIAS.user_id_ IN (")
                addSqlQueryParams(condition, users)
                condition.append(")")
            }
            if (groups.isNotEmpty()) {
                if (users.isNotEmpty()) {
                    condition.append(" OR ")
                }
                condition.append("$IDENTITY_LINK_ALIAS.group_id_ IN (")
                addSqlQueryParams(condition, groups)
                condition.append(")")
            }
            condition.append("))))")
            return true
        } else if (PROC_VARIABLES_MAPPING.containsKey(attribute)) {

            // Textual document value should also match mainDocumentRef variable
            val variableNames = if (attribute == ATT_DOCUMENT && value.isTextual()) {
                DOCUMENT_VARIABLE_NAMES
            } else {
                PROC_VARIABLES_MAPPING[attribute]
            }

            return addVariableCondition(
                variableNames,
                value,
                type,
                isProcessVar = true,
                isRuVariable = true
            )
        } else if (attribute.isAttFromSync()) {

            return addVariableCondition(listOf(attribute), value, type, isProcessVar = false, isRuVariable = true)
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

    /**
     * All names of a single condition are matched by one sub query, so they must share the type
     * and the storage table. Names are resolved to the first defined sync attribute; a mismatch
     * would mean a sync attribute is named after a process variable and is reported to the log,
     * because the resulting condition would use one type for all names.
     */
    private fun resolveSyncAttribute(names: List<String>): TaskSyncAttribute? {
        val attDefs = names.mapNotNull { procTaskAttsSyncService.getTaskSyncAttribute(it) }
        val attDef = attDefs.firstOrNull() ?: return null
        if (attDefs.any { it.type != attDef.type || it.multiple != attDef.multiple }) {
            log.warn {
                "Variables $names of the same condition have different sync attribute definitions. " +
                    "Type '${attDef.type}' and multiple=${attDef.multiple} will be used for all of them"
            }
        }
        return attDef
    }

    private fun addEmptyVariableCondition(
        names: List<String>?,
        isProcessVar: Boolean
    ): Boolean {
        if (names.isNullOrEmpty() || names.any { it.isBlank() }) {
            return false
        }

        val key = VariableConditionKey(names, isEmptyCondition = true)
        val attDef = resolveSyncAttribute(names)
        val attType = attDef?.type ?: AttributeType.TEXT
        val isList = attDef?.multiple ?: false
        val variableCondition = variableConditions.getOrPut(key) {
            VariableCondition(
                variableConditions.size,
                names,
                isProcessVar,
                attType,
                isEmptyCondition = true,
                isList = isList
            )
        }

        condition.append(varCondPlaceholder(variableCondition))
        return true
    }

    private fun addVariableCondition(
        names: List<String>?,
        value: DataValue,
        predicateType: ValuePredicate.Type,
        isProcessVar: Boolean,
        isRuVariable: Boolean = false
    ): Boolean {

        if (names.isNullOrEmpty() || names.any { it.isBlank() }) {
            return false
        }

        val key = VariableConditionKey(names, isEmptyCondition = false)
        val attDef = resolveSyncAttribute(names)
        val attType = attDef?.type ?: AttributeType.TEXT
        val isList = attDef?.multiple ?: false
        val variableCondition = variableConditions.getOrPut(key) {
            VariableCondition(variableConditions.size, names, isProcessVar, attType, isList = isList)
        }

        val alias = getVariableAlias(variableCondition)
        val taskColumn = attType.getTaskAttColumn()

        val values = castSqlParamValueToListOf(value, attType, isRuVariable = isRuVariable)

        when (predicateType) {
            ValuePredicate.Type.EQ -> {
                if (isList) {
                    if (isStringLikeAttType(attType)) {
                        addLikeToOtherCondition(
                            alias,
                            taskColumn,
                            attType,
                            isList,
                            values,
                            variableCondition,
                            quotedListMatch = true
                        )
                    } else {
                        condition.append(ALWAYS_FALSE)
                        return true
                    }
                } else {
                    // Collect EQ values to group them into IN later - doesn't consider predicate level and leads to the selection error result
                    variableCondition.eqValues.addAll(values)
                }
            }

            ValuePredicate.Type.GT -> {
                if (isList) {
                    condition.append(ALWAYS_FALSE)
                    return true
                }
                val paramName = "p${params.size}"
                params[paramName] = values[0]
                variableCondition.otherConditions.add("$alias.$taskColumn > #{$paramName}")
            }

            ValuePredicate.Type.LT -> {
                if (isList) {
                    condition.append(ALWAYS_FALSE)
                    return true
                }
                val paramName = "p${params.size}"
                params[paramName] = values[0]
                variableCondition.otherConditions.add("$alias.$taskColumn < #{$paramName}")
            }

            ValuePredicate.Type.GE -> {
                if (isList) {
                    condition.append(ALWAYS_FALSE)
                    return true
                }
                val paramName = "p${params.size}"
                params[paramName] = values[0]
                variableCondition.otherConditions.add("$alias.$taskColumn >= #{$paramName}")
            }

            ValuePredicate.Type.LE -> {
                if (isList) {
                    condition.append(ALWAYS_FALSE)
                    return true
                }
                val paramName = "p${params.size}"
                params[paramName] = values[0]
                variableCondition.otherConditions.add("$alias.$taskColumn <= #{$paramName}")
            }

            ValuePredicate.Type.IN -> {
                if (isList) {
                    if (isStringLikeAttType(attType)) {
                        addLikeToOtherCondition(
                            alias,
                            taskColumn,
                            attType,
                            isList,
                            values,
                            variableCondition,
                            quotedListMatch = true
                        )
                    } else {
                        condition.append(ALWAYS_FALSE)
                        return true
                    }
                } else {
                    val paramNames = mutableListOf<String>()
                    values.forEach {
                        val paramName = "p${params.size}"
                        params[paramName] = it
                        paramNames.add("#{$paramName}")
                    }
                    variableCondition.otherConditions.add("$alias.$taskColumn IN (${paramNames.joinToString(",")})")
                }
            }

            ValuePredicate.Type.CONTAINS,
            ValuePredicate.Type.LIKE -> {
                addLikeToOtherCondition(alias, taskColumn, attType, isList, values, variableCondition)
            }

            else -> return false
        }
        condition.append(varCondPlaceholder(variableCondition))
        return true
    }

    private fun addLikeToOtherCondition(
        alias: String,
        taskColumn: String,
        attType: AttributeType,
        isList: Boolean,
        values: List<Any?>,
        variableCondition: VariableCondition,
        quotedListMatch: Boolean = false
    ) {
        val likeExpression = if (isList) "convert_from($alias.bytes_, 'UTF8')" else "$alias.$taskColumn"
        // For EQ/IN on list attributes the JSON-serialized list looks like ["foo","bar"];
        // wrap the value with quotes (%"foo"%) so 'foo' does not falsely match 'foobar'.
        val wrap: (Any?) -> String = if (isList && quotedListMatch) {
            { v -> "%\"$v\"%" }
        } else {
            { v -> "%$v%" }
        }
        if (attType == AttributeType.TEXT) {
            val likeValues = values.map { wrap(it).lowercase() }
            val paramNames = mutableListOf<String>()
            likeValues.forEach {
                val paramName = "p${params.size}"
                params[paramName] = it
                paramNames.add("#{$paramName}")
            }
            val columnExpression = "LOWER($likeExpression)"
            if (paramNames.size == 1) {
                variableCondition.otherConditions.add("$columnExpression LIKE ${paramNames[0]}")
            } else {
                variableCondition.otherConditions.add(
                    "(${paramNames.map { "$columnExpression LIKE $it" }.joinToString(" OR ")})"
                )
            }
        } else {
            val likeValues = values.map { wrap(it) }
            val paramNames = mutableListOf<String>()
            likeValues.forEach {
                val paramName = "p${params.size}"
                params[paramName] = it
                paramNames.add("#{$paramName}")
            }
            if (paramNames.size == 1) {
                variableCondition.otherConditions.add("$likeExpression LIKE ${paramNames[0]}")
            } else {
                variableCondition.otherConditions.add(
                    "(${
                        paramNames.map { "$likeExpression LIKE $it" }.joinToString(" OR ")
                    })"
                )
            }
        }
    }

    private fun isStringLikeAttType(attType: AttributeType): Boolean = when (attType) {
        AttributeType.TEXT,
        AttributeType.ASSOC,
        AttributeType.PERSON,
        AttributeType.AUTHORITY,
        AttributeType.AUTHORITY_GROUP -> true
        else -> false
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

    /**
     * Aliases of different conditions may coincide - each of them lives in its own EXISTS
     * sub query, so they never share a scope.
     */
    private fun getVariableAlias(variableCondition: VariableCondition): String {
        val name = variableCondition.names.joinToString("_") +
            if (variableCondition.isEmptyCondition) "_empty" else ""
        return "${VARIABLE_ALIAS_PREFIX}_${name.replace("[^a-zA-Z0-9_]".toRegex(), "_")}"
    }

    /**
     * Variable conditions are rendered as EXISTS subqueries, but the subquery text can only be
     * built after all predicates are visited (filters are collected into [VariableCondition]).
     * So a placeholder is put into [condition] while walking the predicate and is replaced by
     * the actual subquery in [buildTaskSql].
     */
    private fun varCondPlaceholder(variableCondition: VariableCondition): String {
        val polarity = if (variableCondition.isEmptyCondition) "NOT_EXISTS" else "EXISTS"
        // index is used instead of the key itself, because variable names come from the query
        // and may contain characters which would break the placeholder
        return "$VAR_COND_PLACEHOLDER_PREFIX${polarity}_${variableCondition.index}$VAR_COND_PLACEHOLDER_SUFFIX"
    }

    /**
     * Replace variable condition placeholders with EXISTS/NOT EXISTS sub queries.
     *
     * Variable conditions used to be rendered as LEFT JOIN plus "alias.id_ IS [NOT] NULL" check
     * in WHERE. When such checks end up under OR (e.g. document is matched against both
     * documentRef and mainDocumentRef), PostgreSQL is not able to reduce outer joins to inner
     * ones, so it has to read the whole act_ru_task and apply the selective filter last.
     * A semi-join lets the planner start from act_ru_variable instead.
     */
    private fun renderVariableConditions(conditionSql: String): String {
        var result = conditionSql
        for (variableCondition in variableConditions.values) {
            val placeholder = varCondPlaceholder(variableCondition)
            if (!result.contains(placeholder)) {
                continue
            }
            val subQuery = buildVariableSubQuery(variableCondition)
            val operator = if (variableCondition.isEmptyCondition) "NOT EXISTS" else "EXISTS"
            result = result.replace(placeholder, "$operator ($subQuery)")
        }
        return result
    }

    private fun buildVariableSubQuery(variableCondition: VariableCondition): String {

        val alias = getVariableAlias(variableCondition)

        // Variable name comes from the query predicate (only its prefix is validated),
        // so it must be bound as a parameter and never inlined into the SQL text.
        val nameParams = variableCondition.names.map {
            val paramName = "p${params.size}"
            params[paramName] = it
            "#{$paramName}"
        }
        val nameCondition = if (nameParams.size == 1) {
            "$alias.name_ = ${nameParams[0]}"
        } else {
            "$alias.name_ IN (${nameParams.joinToString(",")})"
        }

        val subQuery = StringBuilder("SELECT 1 FROM ")
        if (variableCondition.isList) {
            subQuery.append("act_ge_bytearray $alias WHERE ")
            subQuery.append("$nameCondition AND ")
            subQuery.append("$alias.root_proc_inst_id_ = $TASK_ALIAS.proc_inst_id_")
        } else {
            subQuery.append("act_ru_variable $alias WHERE ")
            subQuery.append("$nameCondition AND ")
            subQuery.append("$alias.type_ = '${variableCondition.attType.getTaskAttType()}' AND ")
            subQuery.append("$alias.proc_inst_id_ = $TASK_ALIAS.proc_inst_id_ AND ")

            if (variableCondition.isProcessVar) {
                subQuery.append("$alias.task_id_ IS NULL")
            } else {
                subQuery.append("$alias.task_id_ = $TASK_ALIAS.id_")
            }
        }

        if (!variableCondition.isEmptyCondition) {
            buildVariableFilterCondition(alias, variableCondition)?.let {
                subQuery.append(" AND (").append(it).append(")")
            }
        }

        return subQuery.toString()
    }

    private fun buildVariableFilterCondition(alias: String, variableCondition: VariableCondition): String? {
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
                val hasRangeConditions = otherConditions.any { cond ->
                    cond.contains(" >= ") || cond.contains(" <= ") ||
                        cond.contains(" > ") || cond.contains(" < ")
                }
                val hasLikeConditions = otherConditions.any { cond ->
                    cond.contains(" LIKE ")
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

        return when {
            finalConditions.isEmpty() -> null
            finalConditions.size == 1 -> finalConditions[0]
            // If we have both EQ and other conditions, combine them with OR
            else -> finalConditions.joinToString(" OR ")
        }
    }

    /**
     * Visible for testing: the shape of the generated SQL is the point of this builder,
     * so it is asserted directly (see ProcTaskSqlQueryShapeTest).
     */
    internal fun buildTaskSql(
        selectFields: String,
        withLimitAndSort: Boolean
    ): String {
        val sqlSelectQuery = StringBuilder("SELECT $selectFields")
        if (withLimitAndSort) {
            for (sortBy in sorting) {
                sqlSelectQuery.append(",${sortBy.attribute}")
            }
        }
        sqlSelectQuery.append(" FROM act_ru_task $TASK_ALIAS ")

        if (condition.isNotEmpty()) {
            sqlSelectQuery.append(" WHERE ")
                .append(renderVariableConditions(condition.toString()))
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
            sqlSelectQuery.append(" LIMIT ${getEffectiveMaxItems()}")
        }
        if (withLimitAndSort && skipCount > 0) {
            sqlSelectQuery.append(" OFFSET $skipCount")
        }

        return sqlSelectQuery.toString()
    }

    private fun createTaskQuery(
        selectFields: String,
        withLimitAndSort: Boolean
    ): NativeTaskQuery {
        val sql = buildTaskSql(selectFields, withLimitAndSort)

        var nativeTaskQuery = taskService.createNativeTaskQuery().sql(sql)
        for ((key, value) in params) {
            nativeTaskQuery = nativeTaskQuery.parameter(key, value)
        }

        log.trace { "Build proc task query:\n $sql \n with params: \n$params" }

        return nativeTaskQuery
    }

    /**
     * Execute SQL query and return list of task IDs directly via JDBC.
     * This avoids creating TaskEntity objects which would pollute DbEntityCache
     * with incomplete entities (revision=0, executionId=null).
     *
     * Uses DataSourceUtils to participate in the existing transaction context if one exists.
     *
     * Note: SQL injection is not a concern here because:
     * 1. SQL structure is built internally by buildTaskSql() using only internal column names
     * 2. Everything coming from the query - both values and variable names - goes through
     *    parameterized queries (? placeholders)
     */
    @Suppress("SqlSourceToSinkFlow")
    private fun executeTaskIdsQuery(sql: String): List<String> {
        // Convert MyBatis #{paramName} to JDBC ? placeholders
        val paramValues = mutableListOf<Any?>()
        val jdbcSql = sql.replace(Regex("#\\{(\\w+)}")) { match ->
            val paramName = match.groupValues[1]
            require(params.containsKey(paramName)) {
                "SQL parameter '$paramName' not found in params map"
            }
            paramValues.add(params[paramName])
            "?"
        }

        log.trace { "Execute task IDs query:\n $jdbcSql \n with params: \n$paramValues" }

        val dataSource = processEngineConfiguration.dataSource
        val connection = DataSourceUtils.getConnection(dataSource)
        try {
            connection.prepareStatement(jdbcSql).use { stmt ->
                paramValues.forEachIndexed { index, value ->
                    stmt.setObject(index + 1, value)
                }
                stmt.executeQuery().use { rs ->
                    val result = mutableListOf<String>()
                    while (rs.next()) {
                        result.add(rs.getString(1))
                    }
                    return result
                }
            }
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource)
        }
    }

    fun selectTasks(): DbFindRes<String> {
        val tasks: List<String>
        val tasksFromCamundaTime = measureTimeMillis {
            val sql = buildTaskSql(
                "DISTINCT $TASK_ALIAS.${TaskQueryProperty.TASK_ID.name}",
                withLimitAndSort = true
            )
            tasks = executeTaskIdsQuery(sql)
        }

        val totalCount: Long
        val camundaCountTime = measureTimeMillis {
            totalCount = if (getEffectiveMaxItems() > tasks.size) {
                skipCount + tasks.size.toLong()
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

    private fun AttributeType.getTaskAttColumn(): String {
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
