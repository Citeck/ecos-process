package ru.citeck.ecos.process.domain.proctask.service

import mu.KotlinLogging
import org.camunda.bpm.engine.FormService
import org.camunda.bpm.engine.TaskService
import org.springframework.stereotype.Service
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.data.sql.records.utils.DbDateUtils
import ru.citeck.ecos.data.sql.repo.find.DbFindRes
import ru.citeck.ecos.model.lib.comments.dto.CommentDto
import ru.citeck.ecos.model.lib.comments.dto.CommentTag
import ru.citeck.ecos.model.lib.comments.dto.CommentTagType
import ru.citeck.ecos.model.lib.comments.service.CommentsService
import ru.citeck.ecos.model.lib.delegation.dto.AuthDelegation
import ru.citeck.ecos.model.lib.delegation.service.DelegationService
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.*
import ru.citeck.ecos.process.domain.proctask.converter.CacheableTaskConverter
import ru.citeck.ecos.process.domain.proctask.converter.toProcTask
import ru.citeck.ecos.process.domain.proctask.dto.CompleteTaskData
import ru.citeck.ecos.process.domain.proctask.dto.ProcTaskDto
import ru.citeck.ecos.process.domain.proctask.dto.getComment
import ru.citeck.ecos.process.domain.proctask.service.ProcTaskSqlQueryBuilder.Companion.ATT_DOCUMENT_TYPE
import ru.citeck.ecos.process.domain.proctask.service.ProcTaskSqlQueryBuilder.Companion.ATT_DOCUMENT_TYPE_REF
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.PredicateUtils
import ru.citeck.ecos.records2.predicate.model.*
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.dao.query.dto.query.QueryPage
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import ru.citeck.ecos.webapp.api.authority.EcosAuthoritiesApi
import ru.citeck.ecos.webapp.api.entity.EntityRef
import kotlin.system.measureTimeMillis

private const val TASK_COMMENT_BROADCAST_ASPECT = "task-comments-broadcastable"

@Service
class ProcTaskServiceImpl(
    private val authoritiesApi: EcosAuthoritiesApi,
    private val camundaTaskService: TaskService,
    private val camundaTaskFormService: FormService,
    private val cacheableTaskConverter: CacheableTaskConverter,
    private val delegationService: DelegationService,
    private val commentsService: CommentsService,
    private val recordsService: RecordsService
) : ProcTaskService {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun findTasks(predicate: Predicate): DbFindRes<String> {
        return findTasks(predicate, emptyList(), QueryPage.create {})
    }

    override fun findTasks(
        predicate: Predicate,
        sortBy: List<SortBy>,
        page: QueryPage
    ): DbFindRes<String> {
        var isManagerWithoutSubordinates = false
        val managerToActorsPredicate = PredicateUtils.mapValuePredicates(predicate) {
            if (it.getAttribute() == "actorManager") {
                val manager = it.getValue().asText()
                val subordinates = getAllSubordinates(manager)
                if (subordinates.isNotEmpty()) {
                    Predicates.inVals("actor", subordinates)
                } else {
                    isManagerWithoutSubordinates = true
                    VoidPredicate.INSTANCE
                }
            } else {
                it
            }
        } ?: VoidPredicate.INSTANCE

        if (isManagerWithoutSubordinates) {
            return DbFindRes.empty()
        }

        var preparedPredicate = managerToActorsPredicate.transformDocumentTypeRefToDocumentTypeAtt()
        preparedPredicate = PredicateUtils.mapValuePredicates(preparedPredicate) {

            if (it.getAttribute() == ProcTaskSqlQueryBuilder.ATT_PRIORITY && it.getValue().isTextual()) {
                it.setValue(it.getValue().asInt())
            }

            if (it.getAttribute() == ProcTaskSqlQueryBuilder.ATT_DUE_DATE && it.getValue().isTextual()) {
                val value = it.getValue()
                val textVal = DbDateUtils.normalizeDateTimePredicateValue(
                    value.asText(),
                    true
                )
                val rangeDelimIdx = textVal.indexOf('/')

                val newPred = if (rangeDelimIdx > 0 && textVal.length > rangeDelimIdx + 1) {

                    val rangeFrom = textVal.substring(0, rangeDelimIdx)
                    val rangeTo = textVal.substring(rangeDelimIdx + 1)

                    AndPredicate.of(
                        ValuePredicate.ge(it.getAttribute(), rangeFrom),
                        ValuePredicate.lt(it.getAttribute(), rangeTo)
                    )
                } else {
                    ValuePredicate(it.getAttribute(), it.getType(), textVal)
                }

                newPred
            } else if (it.getAttribute() == ProcTaskSqlQueryBuilder.ATT_ACTOR && it.getValue().isTextual()) {
                var actor = it.getValue().asText()
                if (actor == "\$CURRENT") {
                    actor = AuthContext.getCurrentUser()
                }

                val delegations: List<AuthDelegation>
                val delegationsGetTime = measureTimeMillis {
                    delegations = delegationService.getActiveAuthDelegations(actor, emptyList())
                }
                log.debug { "Get active auth delegations: $delegationsGetTime ms" }
                if (delegations.isNotEmpty()) {
                    val actorsVariants = mutableListOf<Predicate>()
                    actorsVariants.add(it)
                    delegations.forEach { delegation ->
                        val delegationConditions = mutableListOf<Predicate>()
                        delegationConditions.add(
                            Predicates.`in`(
                                ProcTaskSqlQueryBuilder.ATT_ACTORS,
                                delegation.delegatedAuthorities
                            )
                        )
                        if (delegation.delegatedTypes.isNotEmpty()) {
                            delegationConditions.add(
                                Predicates.inVals(ATT_DOCUMENT_TYPE, delegation.delegatedTypes)
                            )
                        } else {
                            delegationConditions.add(
                                Predicates.notEmpty(ATT_DOCUMENT_TYPE)
                            )
                        }
                        actorsVariants.add(AndPredicate.of(delegationConditions))
                    }
                    OrPredicate.of(actorsVariants)
                } else {
                    it
                }
            } else {
                it
            }
        } ?: VoidPredicate.INSTANCE

        if (predicate is VoidPredicate && !AuthContext.isRunAsSystemOrAdmin()) {
            return DbFindRes.empty()
        }

        return ProcTaskSqlQueryBuilder(authoritiesApi, camundaTaskService)
            .addConditions(preparedPredicate)
            .setPage(page.skipCount, page.maxItems)
            .setSorting(sortBy)
            .selectTasks()
    }

    override fun getTasksByProcess(processInstanceId: String): List<ProcTaskDto> {
        return camundaTaskService.createTaskQuery()
            .processInstanceId(processInstanceId)
            .initializeFormKeys()
            .list()
            .map { it.toProcTask() }
    }

    override fun getTasksByProcessForCurrentUser(processInstanceId: String): List<ProcTaskDto> {
        log.debug {
            "getTasksByProcessForCurrentUser: processInstanceId=$processInstanceId "
        }

        return getTasksByProcess(processInstanceId).filter {
            it.isCurrentUserTaskActorOrDelegate()
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
            it.isCurrentUserTaskActorOrDelegate()
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

    override fun completeTask(completeData: CompleteTaskData) {
        with(completeData) {
            val taskId = task.id
            val currentUser = AuthContext.getCurrentUser()
            val currentAuthorities = AuthContext.getCurrentAuthorities().toSet()

            val completedOnBehalfOf = getCompletedOnBehalfOfValue(completeData.task, currentUser, currentAuthorities)

            val taskLocalVariables = mutableMapOf<String, Any?>()
            taskLocalVariables[BPMN_TASK_COMPLETED_BY] = currentUser

            if (completedOnBehalfOf.isNotEmpty()) {
                taskLocalVariables[BPMN_TASK_COMPLETED_ON_BEHALF_OF] = completedOnBehalfOf
            }

            val completionVariables = variables.toMutableMap()
            completionVariables[outcome.outcomeId()] = outcome.value
            completionVariables[outcome.nameId()] = outcome.name.toString()
            completionVariables[BPMN_LAST_TASK_COMPLETOR] = currentUser

            val taskComment = getComment()
            completionVariables[BPMN_COMMENT] = taskComment
            taskLocalVariables[BPMN_TASK_COMMENT_LOCAL] = taskComment

            log.debug {
                "Complete task: taskId=$taskId, outcome=$outcome, variables=$completionVariables, " +
                    "taskLocalVariables=$taskLocalVariables"
            }

            cacheableTaskConverter.removeFromActualTaskCache(taskId)

            camundaTaskService.setVariablesLocal(taskId, taskLocalVariables)
            camundaTaskFormService.submitTaskForm(taskId, completionVariables)

            taskComment?.let {
                createTaskCommentIfRequired(it, task)
            }
        }
    }

    private fun createTaskCommentIfRequired(comment: String, task: ProcTaskDto) {
        if (comment.isBlank() || task.documentRef.isEmpty()) {
            return
        }

        val hasNoBroadcastCommentsAspect = recordsService.getAtt(
            task.documentRef,
            "_aspects._has.$TASK_COMMENT_BROADCAST_ASPECT?bool!"
        ).asBoolean().not()
        if (hasNoBroadcastCommentsAspect) {
            return
        }

        val broadcastCommentsDisabled = recordsService.getAtt(
            task.documentRef,
            "$TASK_COMMENT_BROADCAST_ASPECT:broadcastComments?bool"
        ).asBoolean(true).not()
        if (broadcastCommentsDisabled) {
            return
        }

        val commentDto = CommentDto(
            text = comment,
            record = task.documentRef,
            tags = listOf(
                CommentTag(
                    type = CommentTagType.TASK,
                    name = task.name
                )
            )
        )

        commentsService.createComment(commentDto)
    }

    private fun getCompletedOnBehalfOfValue(
        task: ProcTaskDto,
        currentUser: String,
        currentAuthorities: Set<String>
    ): String {

        if (task.assignee.getLocalId() == currentUser) {
            return ""
        }
        val candidatesRefs = HashSet(task.candidateUsers)
        candidatesRefs.addAll(task.candidateGroups)
        val candidates = authoritiesApi.getAuthorityNames(candidatesRefs.toList())

        if (task.assignee.isEmpty() && candidates.any { currentAuthorities.contains(it) }) {
            return ""
        }
        val documentType = task.documentType
        if (documentType.isNullOrBlank()) {
            return ""
        }
        val delegations = delegationService.getActiveAuthDelegations(
            currentUser,
            listOf(documentType)
        )
        val delegation = if (task.assignee.isNotEmpty()) {
            delegations.find { delegation ->
                delegation.delegatedAuthorities.contains(task.assignee.getLocalId())
            }
        } else {
            delegations.find { delegation ->
                candidates.any { delegation.delegatedAuthorities.contains(it) }
            }
        }
        if (delegation == null) {
            return ""
        }
        return delegation.delegator
    }

    override fun getVariables(taskId: String): Map<String, Any?> {
        return camundaTaskService.getVariables(taskId)
    }

    override fun getVariable(taskId: String, variableName: String): Any? {
        return camundaTaskService.getVariable(taskId, variableName)
    }

    override fun getVariablesLocal(taskId: String): Map<String, Any?> {
        return camundaTaskService.getVariablesLocal(taskId)
    }

    override fun getVariableLocal(taskId: String, variableName: String): Any? {
        return camundaTaskService.getVariableLocal(taskId, variableName)
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

    private fun getAllSubordinates(manager: String, results: MutableSet<String> = LinkedHashSet()): Set<String> {
        val subordinates = getSubordinatesList(manager)
        if (subordinates.isNotEmpty()) {
            results.addAll(subordinates)
            for (subordinate in subordinates) {
                getAllSubordinates("emodel/person@$subordinate", results)
            }
        }
        return results
    }

    private fun getSubordinatesList(manager: String): Collection<String> {
        val managerId = if (manager == "\$CURRENT") {
            authoritiesApi.getPersonRef(AuthContext.getCurrentUser())
        } else {
            EntityRef.valueOf(manager)
        }

        return recordsService.query(
            RecordsQuery.create {
                withSourceId("emodel/person")
                withLanguage(PredicateService.LANGUAGE_PREDICATE)
                withQuery(Predicates.eq("manager", managerId))
                withMaxItems(300)
            }
        ).getRecords().map { it.getLocalId() }
    }

    private fun Predicate.transformDocumentTypeRefToDocumentTypeAtt(): Predicate {
        var predicate = PredicateUtils.mapAttributePredicates(this) {
            if (it.getAttribute() == ATT_DOCUMENT_TYPE_REF) {
                it.setAttribute(ATT_DOCUMENT_TYPE)
            }
            it
        } ?: VoidPredicate.INSTANCE

        predicate = PredicateUtils.mapValuePredicates(predicate) {
            if (it.getAttribute() == ATT_DOCUMENT_TYPE) {
                it.setValue(EntityRef.valueOf(it.getValue().asText()).getLocalId())
                val type = when (it.getType()) {
                    ValuePredicate.Type.CONTAINS,
                    ValuePredicate.Type.LIKE -> ValuePredicate.Type.EQ

                    else -> it.getType()
                }
                it.setType(type)
            }
            it
        } ?: VoidPredicate.INSTANCE

        return predicate
    }
}
