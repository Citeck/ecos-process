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
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.predicate.model.*
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import ru.citeck.ecos.webapp.api.authority.EcosAuthoritiesApi
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.system.measureTimeMillis

class ProcTaskSqlQueryBuilder(
    private val authoritiesApi: EcosAuthoritiesApi,
    private val taskService: TaskService
) {

    companion object {

        private const val TASK_ALIAS = "task"
        private const val CANDIDATE_ALIAS = "candidate"

        private val TASK_ATTS_MAPPING = mapOf(
            RecordConstants.ATT_CREATED to "$TASK_ALIAS.${TaskQueryProperty.CREATE_TIME.name}"
        )

        private val TASK_ATTS_TYPES = mapOf(
            RecordConstants.ATT_CREATED to AttributeType.DATETIME
        )

        const val ATT_ACTORS = "actors"
        const val ATT_ACTOR = "actor"
        const val ATT_DOCUMENT = "document"
        const val ATT_DOCUMENT_TYPE = "documentType"

        private val PROC_VARIABLES_MAPPING = mapOf(
            ATT_DOCUMENT to BPMN_DOCUMENT_REF,
            ATT_DOCUMENT_TYPE to BPMN_DOCUMENT_TYPE
        )

        private val log = KotlinLogging.logger {}
    }

    private val joins = StringBuilder()
    private val condition = StringBuilder()
    private val params = LinkedHashMap<String, Any?>()

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
            val actors = if (value.isTextual() && value.asText() == "\$CURRENT") {
                AuthContext.getCurrentUserWithAuthorities()
            } else {
                castSqlParamValueToListOf(value, AttributeType.AUTHORITY)
            }
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

            return addVariableCondition(PROC_VARIABLES_MAPPING[attribute], value, type, true)
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
                else -> return false
            }

            val attType = TASK_ATTS_TYPES[attribute] ?: AttributeType.TEXT
            val predicateValue = castSqlParamValueToListOf(value, attType)

            condition.append(" ")
                .append(field)
                .append(" ")
                .append(operator)
                .append(" ")
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

        condition.append(" NOT EXISTS(SELECT id_ FROM act_ru_variable WHERE name_ = '$name' AND ")
        if (isProcessVar) {
            condition.append("task_id_ IS NULL")
        } else {
            condition.append("$TASK_ALIAS.id_ = task_id_")
        }
        condition.append(
            " AND $TASK_ALIAS.PROC_INST_ID_ = PROC_INST_ID_ AND type_='string' " +
                "AND text_ IS NOT NULL AND text_ != ''"
        )
        condition.append(")")
        return true
    }

    private fun addVariableCondition(
        name: String?,
        value: DataValue,
        type: ValuePredicate.Type,
        isProcessVar: Boolean
    ): Boolean {

        if (name.isNullOrBlank() || type != ValuePredicate.Type.EQ && type != ValuePredicate.Type.IN) {
            return false
        }
        val values = castSqlParamValueToListOf(value, AttributeType.TEXT)

        condition.append(" EXISTS(SELECT id_ FROM act_ru_variable WHERE name_ = '$name' AND ")
        if (isProcessVar) {
            condition.append("task_id_ IS NULL")
        } else {
            condition.append("$TASK_ALIAS.id_ = task_id_")
        }
        condition.append(" AND $TASK_ALIAS.PROC_INST_ID_ = PROC_INST_ID_ AND type_='string' AND text_")
        if (values.size == 1) {
            condition.append(" = ")
            addSqlQueryParams(condition, values)
        } else {
            condition.append(" IN (")
            addSqlQueryParams(condition, values)
            condition.append(")")
        }
        condition.append(")")

        return true
    }

    private fun castSqlParamValueToListOf(
        value: DataValue,
        type: AttributeType,
        result: MutableList<Any?> = ArrayList()
    ): List<Any?> {
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
                if (type == AttributeType.DATE) {
                    dateTime.toLocalDate()
                } else {
                    dateTime
                }
            }

            AttributeType.AUTHORITY -> {
                if (value.isTextual()) {
                    authoritiesApi.getAuthorityName(value.asText())
                } else {
                    error("Invalid authority type: $value")
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
        if (joins.isNotEmpty()) {
            sqlSelectQuery.append(joins.toString()).append(" ")
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
}
