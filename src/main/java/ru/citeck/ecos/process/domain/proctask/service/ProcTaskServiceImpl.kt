package ru.citeck.ecos.process.domain.proctask.service

import mu.KotlinLogging
import org.camunda.bpm.engine.FormService
import org.camunda.bpm.engine.TaskService
import org.camunda.bpm.engine.impl.TaskQueryProperty
import org.camunda.bpm.engine.task.NativeTaskQuery
import org.springframework.stereotype.Service
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.context.lib.auth.AuthGroup
import ru.citeck.ecos.context.lib.auth.AuthRole
import ru.citeck.ecos.data.sql.repo.find.DbFindPage
import ru.citeck.ecos.data.sql.repo.find.DbFindRes
import ru.citeck.ecos.data.sql.repo.find.DbFindSort
import ru.citeck.ecos.model.lib.attributes.dto.AttributeType
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.*
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.Outcome
import ru.citeck.ecos.process.domain.proctask.converter.CacheableTaskConverter
import ru.citeck.ecos.process.domain.proctask.converter.splitToUserGroupCandidates
import ru.citeck.ecos.process.domain.proctask.converter.toProcTask
import ru.citeck.ecos.process.domain.proctask.dto.ProcTaskDto
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.ValuePredicate
import ru.citeck.ecos.records3.record.dao.query.dto.query.QueryPage
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.system.measureTimeMillis

@Service
class ProcTaskServiceImpl(
    private val camundaTaskService: TaskService,
    private val camundaTaskFormService: FormService,
    private val cacheableTaskConverter: CacheableTaskConverter
) : ProcTaskService {

    companion object {
        private val log = KotlinLogging.logger {}
        private const val SQL_QUERY_FIELDS_PH = "__FIELDS__"

        private const val TASK_TABLE_SQL_ALIAS = "task"
        private val TASK_ATTS_SQL_MAPPING = mapOf(
            RecordConstants.ATT_CREATED to "$TASK_TABLE_SQL_ALIAS.${TaskQueryProperty.CREATE_TIME.name}"
        )
        private val TASK_ATTS_TYPES = mapOf(
            RecordConstants.ATT_CREATED to AttributeType.DATETIME
        )
    }

    override fun getCurrentUserTasksIds(
        predicate: Predicate,
        page: QueryPage,
        sortBy: List<SortBy>
    ): DbFindRes<String> {

        val fullAuth = AuthContext.getCurrentFullAuth()

        val actors = mutableSetOf(fullAuth.getUser())
        actors.addAll(fullAuth.getAuthorities())

        val users = actors.filter { !it.startsWith(AuthGroup.PREFIX) && !it.startsWith(AuthRole.PREFIX) }
        val groups = actors.filter { it.startsWith(AuthGroup.PREFIX) }

        val sorting = sortBy.mapNotNull {
            val field = TASK_ATTS_SQL_MAPPING[it.attribute]
            if (!field.isNullOrBlank()) {
                DbFindSort(field, it.ascending)
            } else {
                null
            }
        }

        val params = mutableMapOf<String, Any?>()

        var baseSqlSelectQuery = "SELECT $SQL_QUERY_FIELDS_PH from act_ru_task $TASK_TABLE_SQL_ALIAS " +
            "LEFT JOIN act_ru_identitylink ilink on ilink.task_id_ = $TASK_TABLE_SQL_ALIAS.id_ " +
            "WHERE ($TASK_TABLE_SQL_ALIAS.assignee_ IN (${addSqlQueryParams(params, users)})" +
            "OR ($TASK_TABLE_SQL_ALIAS.assignee_ IS NULL " +
            "AND ilink.type_ = 'candidate' " +
            "AND (" +
            "ilink.group_id_ IN (${addSqlQueryParams(params, groups)}) " +
            "OR ilink.user_id_ IN (${addSqlQueryParams(params, users)})" +
            ")" +
            "))"

        val queryBuilder = StringBuilder(baseSqlSelectQuery)
        addPredicateConditions(queryBuilder, predicate, params)
        baseSqlSelectQuery = queryBuilder.toString()

        fun createTasksQuery(
            selectFields: String,
            page: DbFindPage = DbFindPage.ALL,
            sorting: List<DbFindSort> = emptyList()
        ): NativeTaskQuery {

            val limit = page.maxItems
            val offset = page.skipCount

            var fullSelectFields = selectFields
            sorting.forEach {
                fullSelectFields += ",${it.column}"
            }

            val sqlSelectQuery = StringBuilder(baseSqlSelectQuery.replace(SQL_QUERY_FIELDS_PH, fullSelectFields))

            if (sorting.isNotEmpty()) {
                sqlSelectQuery.append(" ORDER BY ")
                sorting.forEach {
                    sqlSelectQuery.append(it.column)
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

            if (limit >= 0) {
                sqlSelectQuery.append(" LIMIT $limit")
            }
            if (offset > 0) {
                sqlSelectQuery.append(" OFFSET $offset")
            }
            var query = camundaTaskService.createNativeTaskQuery().sql(sqlSelectQuery.toString())
            for ((key, value) in params) {
                query = query.parameter(key, value)
            }
            return query
        }

        val tasksLimit = if (page.maxItems < 0) {
            1000
        } else {
            page.maxItems
        }

        val taskIds: List<String>
        val tasksFromCamundaTime = measureTimeMillis {
            taskIds = createTasksQuery(
                "DISTINCT $TASK_TABLE_SQL_ALIAS.${TaskQueryProperty.TASK_ID.name}",
                DbFindPage(page.skipCount, tasksLimit),
                sorting
            ).list().map {
                it.id
            }
        }
        val totalCount: Long
        val camundaCountTime = measureTimeMillis {
            totalCount = createTasksQuery(
                "COUNT(DISTINCT $TASK_TABLE_SQL_ALIAS.${TaskQueryProperty.TASK_ID.name})"
            ).count()
        }
        log.debug { "Camunda task count: $camundaCountTime ms" }
        log.debug { "Camunda tasks: $tasksFromCamundaTime ms" }

        return DbFindRes(taskIds, totalCount)
    }

    private fun addSqlQueryParams(params: MutableMap<String, Any?>, values: Collection<Any?>): String {
        val result = StringBuilder()
        for (value in values) {
            val paramName = "p${params.size}"
            params[paramName] = value
            if (result.isNotEmpty()) {
                result.append(",")
            }
            result.append("#{$paramName}")
        }
        return result.toString()
    }

    private fun addPredicateConditions(query: StringBuilder, predicate: Predicate, params: MutableMap<String, Any?>) {

        if (predicate is ValuePredicate) {
            val field = TASK_ATTS_SQL_MAPPING[predicate.getAttribute()]
            if (field.isNullOrBlank()) {
                return
            }
            val operator = when (predicate.getType()) {
                ValuePredicate.Type.GT -> ">"
                ValuePredicate.Type.LT -> "<"
                ValuePredicate.Type.GE -> ">="
                ValuePredicate.Type.LE -> "<="
                else -> null
            } ?: return

            val attType = TASK_ATTS_TYPES[predicate.getAttribute()] ?: AttributeType.TEXT
            val predicateValue = mutableListOf<Any?>()
            castSqlParamValueToListOf(predicate.getValue(), attType, predicateValue)

            query.append(" AND ")
                .append(field)
                .append(" ")
                .append(operator)
                .append(" ")
                .append(addSqlQueryParams(params, predicateValue))
        }
    }

    private fun castSqlParamValueToListOf(value: DataValue, type: AttributeType, result: MutableList<Any?>) {
        if (value.isArray()) {
            value.forEach {
                castSqlParamValueToListOf(it, type, result)
            }
            return
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
            else -> value.asText()
        }
        result.add(convertedValue)
    }

    override fun getTasksByProcess(processId: String): List<ProcTaskDto> {
        return camundaTaskService.createTaskQuery()
            .processInstanceId(processId)
            .initializeFormKeys()
            .list()
            .map { it.toProcTask() }
    }

    override fun getTasksByProcessForCurrentUser(processId: String): List<ProcTaskDto> {
        log.debug {
            "getTasksByProcessForCurrentUser: processId=$processId "
        }

        return getTasksByProcess(processId).filter {
            it.currentUserIsTaskActor()
        }
    }

    override fun getTasksByDocument(document: String): List<ProcTaskDto> {
        return camundaTaskService.createTaskQuery()
            .processVariableValueEquals(BPMN_DOCUMENT_REF, document)
            .initializeFormKeys()
            .list()
            .map { it.toProcTask() }
    }

    override fun getTasksByDocumentForCurrentUser(document: String): List<ProcTaskDto> {
        log.debug {
            "getTasksByDocumentForCurrentUser: document=$document"
        }

        return getTasksByDocument(document).filter {
            it.currentUserIsTaskActor()
        }
    }

    override fun getTaskById(taskId: String): ProcTaskDto? {
        val result: ProcTaskDto?
        val time = measureTimeMillis {
            result = camundaTaskService.createTaskQuery()
                .taskId(taskId)
                .initializeFormKeys()
                .singleResult()
                ?.toProcTask()
        }

        log.trace { "Get Camunda Task by id: time=$time ms" }

        return result
    }

    override fun getTasksByIds(taskIds: List<String>): List<ProcTaskDto?> {
        val result: List<ProcTaskDto?>
        val time = measureTimeMillis {
            result = camundaTaskService.createTaskQuery()
                .taskIdIn(*taskIds.toTypedArray())
                .initializeFormKeys()
                .list()
                .map {
                    val procTask: ProcTaskDto?
                    val time = measureTimeMillis {
                        procTask = it.toProcTask()
                    }

                    log.trace { "Task to procTask: $time ms" }

                    procTask
                }
        }

        log.debug { "Get Camunda Tasks by ids: $time ms" }

        return result
    }

    override fun completeTask(taskId: String, outcome: Outcome, variables: Map<String, Any?>) {
        val currentUser = AuthContext.getCurrentUser()

        camundaTaskService.setVariableLocal(taskId, BPMN_TASK_COMPLETED_BY, currentUser)

        val completionVariables = variables.toMutableMap()
        completionVariables[outcome.outcomeId()] = outcome.value
        completionVariables[outcome.nameId()] = outcome.name.toString()
        completionVariables[BPMN_LAST_TASK_COMPLETOR] = currentUser

        log.debug { "Complete task: taskId=$taskId, outcome=$outcome, variables=$completionVariables" }

        cacheableTaskConverter.removeFromActualTaskCache(taskId)
        camundaTaskFormService.submitTaskForm(taskId, completionVariables)
    }

    override fun getVariables(taskId: String): Map<String, Any?> {
        return camundaTaskService.getVariables(taskId)
    }

    override fun claimTask(taskId: String, userId: String) {
        camundaTaskService.claim(taskId, userId)
        moveCandidatesToOriginalAtts(taskId)

        cacheableTaskConverter.removeFromActualTaskCache(taskId)
    }

    override fun unclaimTask(taskId: String) {
        camundaTaskService.setAssignee(taskId, null)
        returnCandidatesFromOriginalAtts(taskId)

        cacheableTaskConverter.removeFromActualTaskCache(taskId)
    }

    override fun setAssignee(taskId: String, userId: String) {
        camundaTaskService.setAssignee(taskId, userId)
        moveCandidatesToOriginalAtts(taskId)

        cacheableTaskConverter.removeFromActualTaskCache(taskId)
    }

    private fun moveCandidatesToOriginalAtts(taskId: String) {
        camundaTaskService.getIdentityLinksForTask(taskId)?.run {
            val (candidateUsers, candidateGroups) = splitToUserGroupCandidates()

            if (candidateUsers.isNotEmpty()) {
                camundaTaskService.setVariableLocal(
                    taskId,
                    BPMN_TASK_CANDIDATES_USER_ORIGINAL,
                    candidateUsers.toList()
                )
            }

            if (candidateGroups.isNotEmpty()) {
                camundaTaskService.setVariableLocal(
                    taskId,
                    BPMN_TASK_CANDIDATES_GROUP_ORIGINAL,
                    candidateGroups.toList()
                )
            }

            log.debug {
                "Move candidates to original attributes: taskId=$taskId, " +
                    "users=$candidateUsers, groups=$candidateGroups"
            }

            candidateUsers.forEach { camundaTaskService.deleteCandidateUser(taskId, it) }
            candidateGroups.forEach { camundaTaskService.deleteCandidateGroup(taskId, it) }
        }
    }

    private fun returnCandidatesFromOriginalAtts(taskId: String) {
        @Suppress("UNCHECKED_CAST")
        val originalCandidateUsers = camundaTaskService.getVariableLocal(
            taskId,
            BPMN_TASK_CANDIDATES_USER_ORIGINAL
        ) as? List<String>? ?: emptyList()

        originalCandidateUsers.forEach {
            camundaTaskService.addCandidateUser(taskId, it)
        }

        @Suppress("UNCHECKED_CAST")
        val originalCandidateGroups = camundaTaskService.getVariableLocal(
            taskId,
            BPMN_TASK_CANDIDATES_GROUP_ORIGINAL
        ) as? List<String>? ?: emptyList()

        originalCandidateGroups.forEach {
            camundaTaskService.addCandidateGroup(taskId, it)
        }

        log.debug {
            "Return candidates from original attributes: taskId=$taskId, " +
                "users=$originalCandidateUsers, groups=$originalCandidateGroups"
        }

        camundaTaskService.removeVariableLocal(taskId, BPMN_TASK_CANDIDATES_USER_ORIGINAL)
        camundaTaskService.removeVariableLocal(taskId, BPMN_TASK_CANDIDATES_GROUP_ORIGINAL)
    }
}
